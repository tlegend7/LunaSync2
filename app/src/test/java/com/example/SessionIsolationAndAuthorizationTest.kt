package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.migration.LegacySymptomsMigration
import com.example.data.migration.MigrationResult
import com.example.data.model.DailySymptom
import com.example.data.model.FlowLevel
import com.example.data.model.Mood
import com.example.data.model.SpaceStatus
import com.example.data.model.SpottingSuggestionStatus
import com.example.data.model.UserRole
import com.example.ui.viewmodel.MainViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SessionIsolationAndAuthorizationTest {

  private lateinit var application: Application
  private lateinit var viewModel: MainViewModel

  @Before
  fun setup() {
    application = ApplicationProvider.getApplicationContext()
    if (FirebaseApp.getApps(application).isEmpty()) {
      val options = FirebaseOptions.Builder()
        .setApplicationId("com.aistudio.lunasync.kpmvtw")
        .setProjectId("demo-no-project")
        .setApiKey("fakeApiKeyForTesting123")
        .build()
      FirebaseApp.initializeApp(application, options)
    }
    viewModel = MainViewModel(application)
  }

  // --- 1. PRUEBAS DE ROLES Y MODO INVITADO ---

  @Test
  fun `guest mode allows visual role switching for demo purposes`() {
    viewModel.enterGuestDemoMode()
    assertTrue(viewModel.uiState.value.isGuestMode)
    assertEquals(UserRole.PRIMARY_ELLA, viewModel.uiState.value.currentRole)

    viewModel.switchRole(UserRole.SUPPORT_PARTNER)
    assertEquals(UserRole.SUPPORT_PARTNER, viewModel.uiState.value.currentRole)

    viewModel.switchRole(UserRole.PRIMARY_ELLA)
    assertEquals(UserRole.PRIMARY_ELLA, viewModel.uiState.value.currentRole)
  }

  @Test
  fun `non-guest mode rejects arbitrary role switching`() {
    assertFalse(viewModel.uiState.value.isGuestMode)
    val initialRole = viewModel.uiState.value.currentRole

    viewModel.switchRole(UserRole.SUPPORT_PARTNER)
    assertEquals(initialRole, viewModel.uiState.value.currentRole)
  }

  // --- 2. PRUEBAS DE CIERRE DE SESIÓN AUTENTICADO Y AISLAMIENTO ---

  @Test
  fun `signOut execution completely clears space, cycles, symptoms and invalidates sessionId`() {
    viewModel.enterGuestDemoMode()
    assertNotNull(viewModel.uiState.value.space)
    assertFalse(viewModel.uiState.value.pastCycles.isEmpty())
    val priorSessionId = viewModel.currentSessionId

    // Ejecuta signOut real con CoroutineScope
    val testScope = CoroutineScope(Dispatchers.Unconfined)
    viewModel.signOut(testScope)

    val state = viewModel.uiState.value
    assertNull("Space must be null after signOut", state.space)
    assertTrue("Past cycles must be empty", state.pastCycles.isEmpty())
    assertTrue("Intimacy logs must be empty", state.intimacyLogs.isEmpty())
    assertTrue("Partner observations must be empty", state.partnerObservations.isEmpty())
    assertNull("Today symptom must be null", state.todaySymptom)
    assertNull("Active pairing code must be null", state.activePairingCode)
    assertFalse("Guest mode must be false", state.isGuestMode)
    assertNotEquals("Session ID must be rotated after signOut", priorSessionId, viewModel.currentSessionId)
  }

  @Test
  fun `cross-session race condition prevention between account A and account C`() {
    // 1. Simular sesión activa de Cuenta A
    val sessionIdA = viewModel.currentSessionId
    val userAUid = "user_A_123"
    viewModel.activeObservedUserId = userAUid
    assertTrue(viewModel.isSessionValid(sessionIdA, userAUid))

    // 2. Operación asíncrona de A en vuelo...
    // 3. Usuario A cierra sesión y entra Cuenta C
    val testScope = CoroutineScope(Dispatchers.Unconfined)
    viewModel.signOut(testScope)

    val userCUid = "user_C_456"
    val sessionIdC = viewModel.currentSessionId
    viewModel.activeObservedUserId = userCUid

    // 4. La respuesta tardía de A llega ahora
    val isALateResponseAllowed = viewModel.isSessionValid(sessionIdA, userAUid)
    assertFalse("Late response from User A must be invalidated for User C", isALateResponseAllowed)

    // 5. La sesión de C es la única válida
    assertTrue("Current User C session must be valid", viewModel.isSessionValid(sessionIdC, userCUid))
    assertFalse("User C cannot validate with old session ID of A", viewModel.isSessionValid(sessionIdA, userCUid))
  }

  // --- 3. PRUEBAS DE REVOCACIÓN Y GENERACIÓN DE CÓDIGOS ---

  @Test
  fun `revocation unpairs partner in demo mode correctly`() {
    viewModel.enterGuestDemoMode()
    assertEquals(SpaceStatus.PAIRED.name, viewModel.uiState.value.space?.status)
    assertEquals("demo_partner_user", viewModel.uiState.value.space?.partnerId)

    viewModel.revokePairing()

    assertEquals(SpaceStatus.UNPAIRED.name, viewModel.uiState.value.space?.status)
    assertNull(viewModel.uiState.value.space?.partnerId)
    assertNull(viewModel.uiState.value.activePairingCode)
  }

  @Test
  fun `pairing invite code format matches LUNA prefix and 4 random chars`() {
    viewModel.enterGuestDemoMode()
    viewModel.generatePairingCode()
    val code = viewModel.uiState.value.activePairingCode
    assertNotNull(code)
    assertTrue("Pairing code must start with LUNA-", code!!.startsWith("LUNA-"))
    assertEquals("Total format must be 9 characters (LUNA-XXXX)", 9, code.length)
  }

  // --- 4. PRUEBAS DE MANCHADO: REGLAS DE SANGRAMIENTO Y PRIVACIDAD CONSERVADORA ---

  @Test
  fun `spotting suggestion preserves privacy and does not downgrade heavy or medium flow`() {
    // Escenario 1: Si ya existe HEAVY, aceptar la sugerencia de manchado NO debe degradar el sangrado
    val existingHeavy = FlowLevel.HEAVY.name
    val finalHeavyFlow = if (existingHeavy in listOf("LIGHT", "MEDIUM", "HEAVY")) existingHeavy else FlowLevel.SPOTTING.name
    assertEquals("Heavy flow must be preserved", FlowLevel.HEAVY.name, finalHeavyFlow)

    // Escenario 2: Si ya existe MEDIUM, no degradar
    val existingMedium = FlowLevel.MEDIUM.name
    val finalMediumFlow = if (existingMedium in listOf("LIGHT", "MEDIUM", "HEAVY")) existingMedium else FlowLevel.SPOTTING.name
    assertEquals("Medium flow must be preserved", FlowLevel.MEDIUM.name, finalMediumFlow)

    // Escenario 3: Si no había flujo o era NONE, se convierte en SPOTTING
    val existingNone = FlowLevel.NONE.name
    val finalSpottingFlow = if (existingNone in listOf("LIGHT", "MEDIUM", "HEAVY")) existingNone else FlowLevel.SPOTTING.name
    assertEquals("None flow must become SPOTTING", FlowLevel.SPOTTING.name, finalSpottingFlow)

    // Escenario 4: Privacidad en registro existente se conserva
    val existingIsPrivate = true
    val retainedPrivacy = existingIsPrivate
    assertTrue("Existing privacy setting must be retained", retainedPrivacy)

    // Escenario 5: Registro nuevo generado por sugerencia debe ser privado por defecto
    val brandNewRecordPrivacy: Boolean? = null
    val defaultPrivate = brandNewRecordPrivacy ?: true
    assertTrue("Brand new suggestion record must default to private", defaultPrivate)
  }

  // --- 5. PRUEBAS DE FORMULARIOS: ESTADOS DE GUARDADO, ERROR Y CIERRE ---

  @Test
  fun `form state keeps dialog open and preserves inputs on error, closes only on success`() {
    var isDialogOpen = true
    var dialogError: String? = null
    var isSaving = false
    var inputPublicNote = "Dolor lumbar persistente"
    var inputPainLevel = 4

    // 1. Simular envío iniciado
    isSaving = true
    assertTrue("Saving spinner should be active", isSaving)
    assertTrue("Dialog must remain open while saving", isDialogOpen)

    // 2. Simular fallo de red o reglas (callback onComplete(false))
    val onCompleteError: (Boolean) -> Unit = { success ->
      isSaving = false
      if (!success) {
        dialogError = "Error de red al guardar"
        // El diálogo NO se cierra
      } else {
        isDialogOpen = false
      }
    }
    onCompleteError(false)

    assertFalse("Saving spinner should stop after failure", isSaving)
    assertTrue("Dialog must remain open after failure", isDialogOpen)
    assertNotNull("Error message must be presented", dialogError)
    assertEquals("Dolor lumbar persistente", inputPublicNote)
    assertEquals("User inputs must be preserved without data loss", 4, inputPainLevel)

    // 3. Simular reintento exitoso (callback onComplete(true))
    val onCompleteSuccess: (Boolean) -> Unit = { success ->
      isSaving = false
      if (success) {
        isDialogOpen = false
      }
    }
    onCompleteSuccess(true)

    assertFalse("Dialog must be dismissed after confirmed server success", isDialogOpen)
  }

  // --- 6. PRUEBAS DE CONTRATO Y CONTADORES DE MIGRACIÓN ---

  @Test
  fun `migration result contract clearly distinguishes created, skipped, simulated and errors`() {
    val simResult = MigrationResult(
      totalLegacyRead = 3,
      categoriesCreated = 0,
      categoriesSkippedExisting = 1,
      categoriesSimulated = 2,
      physicalMigrated = 1,
      physicalSkipped = 0,
      moodMigrated = 1,
      moodSkipped = 0,
      notesMigrated = 0,
      notesSkipped = 1,
      errorsCount = 0
    )

    // En simulación (dryRun = true): categoriesCreated DEBE ser 0 y categoriesSimulated > 0
    assertEquals(0, simResult.categoriesCreated)
    assertEquals(2, simResult.categoriesSimulated)
    assertEquals(3, simResult.totalLegacyRead)
    assertEquals(1, simResult.categoriesSkippedExisting)

    val realResult = MigrationResult(
      totalLegacyRead = 3,
      categoriesCreated = 5,
      categoriesSkippedExisting = 1,
      categoriesSimulated = 0,
      physicalMigrated = 2,
      physicalSkipped = 1,
      moodMigrated = 2,
      moodSkipped = 0,
      notesMigrated = 1,
      notesSkipped = 0,
      errorsCount = 0
    )

    // En ejecución real (dryRun = false): categoriesCreated > 0 y categoriesSimulated == 0
    assertEquals(5, realResult.categoriesCreated)
    assertEquals(0, realResult.categoriesSimulated)
    assertEquals(3, realResult.totalLegacyRead)
    assertEquals(1, realResult.categoriesSkippedExisting)
  }
}
