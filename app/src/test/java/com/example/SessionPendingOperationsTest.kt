package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CoupleSpace
import com.example.data.model.DailySymptom
import com.example.data.model.FlowLevel
import com.example.data.model.Mood
import com.example.data.model.PartnerObservation
import com.example.data.model.SharingPermissions
import com.example.data.model.SpaceStatus
import com.example.data.model.SpottingSuggestionStatus
import com.example.data.repository.CoupleSpaceRepository
import com.example.data.repository.CycleRepository
import com.example.ui.viewmodel.MainViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
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

/**
 * Doble de prueba controlable para CycleRepository.
 * Permite retener en vuelo cualquier operacion asincrona para simular
 * respuestas tardias de red que compiten con el cierre o cambio de sesion.
 */
class ControllableCycleRepository(app: Application) : CycleRepository(app) {
  var saveSymptomCompletable = CompletableDeferred<Unit>()
  var acceptSpottingCompletable = CompletableDeferred<Result<Unit>>()
  var lastSavedSymptom: DailySymptom? = null

  override suspend fun saveDailySymptom(symptom: DailySymptom) {
    lastSavedSymptom = symptom
    saveSymptomCompletable.await()
  }

  override suspend fun acceptSpottingSuggestion(
    spaceId: String,
    observationId: String,
    date: String,
    userId: String
  ): Result<Unit> {
    return acceptSpottingCompletable.await()
  }
}

/**
 * Doble de prueba controlable para CoupleSpaceRepository.
 */
class ControllableCoupleSpaceRepository(app: Application) : CoupleSpaceRepository(app) {
  var generateCodeCompletable = CompletableDeferred<String>()
  var joinCodeCompletable = CompletableDeferred<Result<String>>()
  var revokeCompletable = CompletableDeferred<Unit>()

  override suspend fun generateInviteCode(spaceId: String, ownerId: String): String {
    return generateCodeCompletable.await()
  }

  override suspend fun joinWithCode(inviteCode: String, partnerId: String): Result<String> {
    return joinCodeCompletable.await()
  }

  override suspend fun revokePairing(spaceId: String) {
    revokeCompletable.await()
  }
}

/**
 * Pruebas de EJECUCIÓN REAL de aislamiento de sesión y operaciones pendientes en MainViewModel.
 * Verifica que ante carreras asíncronas entre la Cuenta A (que cierra sesión) y la Cuenta C (que inicia sesión),
 * ninguna respuesta tardía (exitosa o fallida) de A altere el estado, espacio o datos de C.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SessionPendingOperationsTest {

  private lateinit var application: Application
  private lateinit var controllableCycleRepo: ControllableCycleRepository
  private lateinit var controllableSpaceRepo: ControllableCoupleSpaceRepository
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

    controllableCycleRepo = ControllableCycleRepository(application)
    controllableSpaceRepo = ControllableCoupleSpaceRepository(application)

    viewModel = MainViewModel(
      application = application,
      cycleRepo = controllableCycleRepo,
      spaceRepo = controllableSpaceRepo
    )
  }

  private fun setupSessionForUser(userId: String, spaceId: String): String {
    val sessionId = UUID.randomUUID().toString()
    viewModel.currentSessionId = sessionId
    viewModel.activeObservedUserId = userId

    viewModel.setUiStateForTesting { state ->
      state.copy(
        isGuestMode = false,
        space = CoupleSpace(
          id = spaceId,
          ownerId = userId,
          partnerId = null,
          status = SpaceStatus.UNPAIRED.name,
          averageCycleLength = 28,
          averagePeriodLength = 5,
          sharingPermissions = SharingPermissions()
        ),
        todaySymptom = null,
        errorMessage = null,
        successMessage = null
      )
    }
    return sessionId
  }

  // --- PRUEBA 1: saveSymptom éxito tardío de A NO fuga a Cuenta C ---
  @Test
  fun `saveSymptom late success from user A does not leak into user C session`() = runBlocking {
    val userAUid = "user_A_111"
    val spaceA = "space_A"
    val sessionA = setupSessionForUser(userAUid, spaceA)

    var callbackInvoked = false
    var callbackSuccess = false
    var callbackError: String? = null

    // 1. Usuario A inicia guardado de síntoma (operación queda retenida en el repo)
    viewModel.saveSymptom(
      flowLevel = FlowLevel.LIGHT,
      painLevel = 2,
      mood = Mood.CALM,
      energyLevel = 3,
      cramps = false,
      headache = false,
      backPain = false,
      bloating = false,
      publicNote = "Nota confidencial de A",
      privateNote = null,
      isPrivate = false
    ) { success, err ->
      callbackInvoked = true
      callbackSuccess = success
      callbackError = err
    }

    assertNotNull("Operation must be pending in repository", controllableCycleRepo.lastSavedSymptom)
    assertFalse("Callback must not be invoked yet", callbackInvoked)

    // 2. Usuario A cierra sesión y entra Cuenta C
    val testScope = CoroutineScope(Dispatchers.Unconfined)
    viewModel.signOut(testScope)

    val userCUid = "user_C_222"
    val spaceC = "space_C"
    val sessionC = setupSessionForUser(userCUid, spaceC)

    assertNotEquals("Session ID must be different", sessionA, sessionC)
    assertEquals(userCUid, viewModel.activeObservedUserId)

    // 3. La operación tardía de A finalmente se completa en el backend
    controllableCycleRepo.saveSymptomCompletable.complete(Unit)

    // 4. Verificación de blindaje:
    // El callback recibe fallo por sesión invalidada y NO hay fuga al estado de C
    assertTrue("Callback must be invoked after completion", callbackInvoked)
    assertFalse("Late response must be marked as invalid session", callbackSuccess)
    assertEquals("Sesión no válida", callbackError)

    val stateC = viewModel.uiState.value
    assertNull("User C must NOT receive User A symptom", stateC.todaySymptom)
    assertNotEquals("User C space must remain untouched", spaceA, stateC.space?.id)
    assertEquals(spaceC, stateC.space?.id)
  }

  // --- PRUEBA 2: saveSymptom error tardío de A NO fuga a Cuenta C ---
  @Test
  fun `saveSymptom late error from user A does not leak error message into user C session`() = runBlocking {
    val userAUid = "user_A_333"
    val spaceA = "space_A"
    setupSessionForUser(userAUid, spaceA)

    var callbackInvoked = false
    var callbackError: String? = null

    viewModel.saveSymptom(
      flowLevel = FlowLevel.NONE,
      painLevel = 0,
      mood = Mood.CALM,
      energyLevel = 3,
      cramps = false,
      headache = false,
      backPain = false,
      bloating = false,
      publicNote = null,
      privateNote = null,
      isPrivate = false
    ) { success, err ->
      callbackInvoked = true
      callbackError = err
    }

    // Usuario A cierra sesión y entra C
    val testScope = CoroutineScope(Dispatchers.Unconfined)
    viewModel.signOut(testScope)

    val userCUid = "user_C_444"
    val spaceC = "space_C"
    setupSessionForUser(userCUid, spaceC)

    // Falla la operación tardía de A
    controllableCycleRepo.saveSymptomCompletable.completeExceptionally(RuntimeException("Error en la red de A"))

    // Verificación: el mensaje de error de A NO se asigna al UI state de C
    assertTrue(callbackInvoked)
    assertNull("User C errorMessage must NOT be populated with User A error", viewModel.uiState.value.errorMessage)
  }

  // --- PRUEBA 3: generatePairingCode tardío de A NO fuga a Cuenta C ---
  @Test
  fun `generatePairingCode late completion from user A does not update user C activePairingCode`() = runBlocking {
    val userAUid = "user_A_555"
    val spaceA = "space_A"
    setupSessionForUser(userAUid, spaceA)

    viewModel.generatePairingCode()

    // A cierra sesión y entra C
    val testScope = CoroutineScope(Dispatchers.Unconfined)
    viewModel.signOut(testScope)

    val userCUid = "user_C_666"
    val spaceC = "space_C"
    setupSessionForUser(userCUid, spaceC)

    // Se resuelve el código de A
    controllableSpaceRepo.generateCodeCompletable.complete("LUNA-AAAA")

    // Verificación: C no tiene el código de vinculación de A
    assertNull("User C must NOT receive User A pairing code", viewModel.uiState.value.activePairingCode)
  }

  // --- PRUEBA 4: joinWithCode tardío de A NO vincula a Cuenta C ---
  @Test
  fun `joinWithCode late completion from user A does not affect user C space`() = runBlocking {
    val userAUid = "user_A_777"
    val spaceA = "space_A"
    setupSessionForUser(userAUid, spaceA)

    viewModel.joinWithCode("LUNA-JOIN")

    // A cierra sesión y entra C
    val testScope = CoroutineScope(Dispatchers.Unconfined)
    viewModel.signOut(testScope)

    val userCUid = "user_C_888"
    val spaceC = "space_C"
    setupSessionForUser(userCUid, spaceC)

    // Se resuelve la vinculación de A
    controllableSpaceRepo.joinCodeCompletable.complete(Result.success("space_joined_by_A"))

    // Verificación: El espacio de C sigue siendo space_C y no el de A
    assertEquals("User C space must remain space_C", spaceC, viewModel.uiState.value.space?.id)
  }

  // --- PRUEBA 5: acceptSpottingSuggestion tardío de A NO fuga a Cuenta C ---
  @Test
  fun `acceptSpottingSuggestion late completion from user A does not alter user C state`() = runBlocking {
    val userAUid = "user_A_999"
    val spaceA = "space_A"
    setupSessionForUser(userAUid, spaceA)

    val suggestion = PartnerObservation(
      id = "obs_A",
      spaceId = spaceA,
      date = "2026-06-15",
      unloggedSpottingReported = true,
      spottingSuggestionStatus = SpottingSuggestionStatus.PENDING.name
    )
    viewModel.setUiStateForTesting { it.copy(activeSpottingSuggestion = suggestion) }

    viewModel.acceptSpottingSuggestion()

    // A cierra sesión y entra C
    val testScope = CoroutineScope(Dispatchers.Unconfined)
    viewModel.signOut(testScope)

    val userCUid = "user_C_000"
    val spaceC = "space_C"
    setupSessionForUser(userCUid, spaceC)

    // Se resuelve la aceptación de A
    controllableCycleRepo.acceptSpottingCompletable.complete(Result.success(Unit))

    // Verificación: El estado de éxito de A no se inyecta en C
    assertNull(viewModel.uiState.value.successMessage)
  }
}
