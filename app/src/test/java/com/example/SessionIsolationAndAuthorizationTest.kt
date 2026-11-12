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
    viewModel.revokePairing()
    viewModel.generatePairingCode()
    val code = viewModel.uiState.value.activePairingCode
    assertNotNull(code)
    assertTrue("Pairing code must start with LUNA-", code!!.startsWith("LUNA-"))
    assertEquals("Total format must be 9 characters (LUNA-XXXX)", 9, code.length)
  }
}

