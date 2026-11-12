package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CheckInPromptType
import com.example.data.model.CoupleSpace
import com.example.data.model.DailySymptom
import com.example.data.model.FlowLevel
import com.example.data.model.Mood
import com.example.data.model.PartnerObservation
import com.example.data.model.PerceivedMood
import com.example.data.model.SharingPermissions
import com.example.data.model.SpaceStatus
import com.example.data.model.SpottingSuggestionStatus
import com.example.data.model.UserRole
import com.example.data.repository.AuthRepository
import com.example.data.repository.CoupleSpaceRepository
import com.example.data.repository.CycleRepository
import com.example.data.repository.PartnerSupportRepository
import com.example.data.repository.SpaceObservationResult
import com.example.ui.viewmodel.MainViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
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
import org.robolectric.shadows.ShadowLooper

/**
 * Repositorio de autenticación controlable para pruebas deterministas de sesión.
 * Permite emitir identidades de usuario reales o nulas sin depender de estado residual.
 */
class ControllableAuthRepository(context: Context) : AuthRepository(context) {
  private val _testUserFlow = MutableStateFlow<FirebaseUser?>(null)
  override val currentUser: StateFlow<FirebaseUser?> = _testUserFlow.asStateFlow()

  fun emitUser(user: FirebaseUser?) {
    _testUserFlow.value = user
  }

  override fun signOut(scope: CoroutineScope, onComplete: () -> Unit) {
    _testUserFlow.value = null
    onComplete()
  }
}

/**
 * Doble de prueba controlable para CycleRepository con registro estricto de llamadas,
 * argumentos y señales deterministas de inicio y finalización.
 */
class ControllableCycleRepository(app: Application) : CycleRepository(app) {
  // Señales explícitas de inicio de llamada
  var saveSymptomCalled = CompletableDeferred<DailySymptom>()
  var acceptSpottingCalled = CompletableDeferred<Triple<String, String, String>>() // spaceId, obsId, userId

  // Control de resolución
  var saveSymptomDeferred = CompletableDeferred<Unit>()
  var acceptSpottingDeferred = CompletableDeferred<Result<Unit>>()

  override suspend fun saveDailySymptom(symptom: DailySymptom) {
    saveSymptomCalled.complete(symptom)
    saveSymptomDeferred.await()
  }

  override suspend fun acceptSpottingSuggestion(
    spaceId: String,
    observationId: String,
    date: String,
    userId: String
  ): Result<Unit> {
    acceptSpottingCalled.complete(Triple(spaceId, observationId, userId))
    return acceptSpottingDeferred.await()
  }
}

/**
 * Doble de prueba controlable para CoupleSpaceRepository con registro de llamadas y argumentos.
 */
class ControllableCoupleSpaceRepository(app: Application) : CoupleSpaceRepository(app) {
  var generateCodeCalled = CompletableDeferred<Pair<String, String>>() // spaceId, createdBy
  var joinCodeCalled = CompletableDeferred<Pair<String, String>>() // code, partnerId

  var generateCodeDeferred = CompletableDeferred<String>()
  var joinCodeDeferred = CompletableDeferred<Result<String>>()

  val spaceMap = mutableMapOf<String, CoupleSpace>()

  override fun observeUserSpaceDetailed(userId: String): Flow<SpaceObservationResult> = flow {
    val space = spaceMap[userId]
    if (space != null) {
      emit(SpaceObservationResult.Success(space))
    } else {
      emit(SpaceObservationResult.NoSpaceFound)
    }
  }

  override suspend fun generateInviteCode(spaceId: String, createdBy: String): String {
    generateCodeCalled.complete(Pair(spaceId, createdBy))
    return generateCodeDeferred.await()
  }

  override suspend fun joinWithCode(code: String, partnerId: String): Result<String> {
    joinCodeCalled.complete(Pair(code, partnerId))
    return joinCodeDeferred.await()
  }
}

/**
 * Doble de prueba controlable para PartnerSupportRepository con registro determinista de llamadas.
 */
class ControllablePartnerSupportRepository(context: Context) : PartnerSupportRepository(context) {
  var logObservationCalled = CompletableDeferred<PartnerObservation>()
  var sendCheckInCalled = CompletableDeferred<Pair<CheckInPromptType, String?>>()

  var logObservationDeferred = CompletableDeferred<Unit>()
  var sendCheckInDeferred = CompletableDeferred<Unit>()

  override suspend fun logObservation(
    spaceId: String,
    date: String,
    perceivedMood: PerceivedMood?,
    apparentPainLevel: Int?,
    unloggedSpottingReported: Boolean,
    careActionTaken: String?,
    sweetMessage: String?,
    userId: String
  ) {
    val obs = PartnerObservation(
      spaceId = spaceId,
      date = date,
      perceivedMood = perceivedMood?.name,
      apparentPainLevel = apparentPainLevel,
      unloggedSpottingReported = unloggedSpottingReported,
      careActionTaken = careActionTaken,
      sweetMessage = sweetMessage,
      loggedBy = userId
    )
    logObservationCalled.complete(obs)
    logObservationDeferred.await()
  }

  override suspend fun sendCheckIn(
    spaceId: String,
    senderUid: String,
    promptType: CheckInPromptType,
    customNote: String?
  ) {
    sendCheckInCalled.complete(Pair(promptType, customNote))
    sendCheckInDeferred.await()
  }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SessionPendingOperationsTest {

  private lateinit var application: Application
  private lateinit var auth: FirebaseAuth
  private lateinit var controllableAuthRepo: ControllableAuthRepository
  private lateinit var controllableCycleRepo: ControllableCycleRepository
  private lateinit var controllableSpaceRepo: ControllableCoupleSpaceRepository
  private lateinit var controllablePartnerRepo: ControllablePartnerSupportRepository
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

    auth = FirebaseAuth.getInstance()
    try {
      auth.useEmulator("127.0.0.1", 9099)
    } catch (_: Exception) {}

    controllableAuthRepo = ControllableAuthRepository(application)
    controllableCycleRepo = ControllableCycleRepository(application)
    controllableSpaceRepo = ControllableCoupleSpaceRepository(application)
    controllablePartnerRepo = ControllablePartnerSupportRepository(application)

    viewModel = MainViewModel(
      application = application,
      authRepo = controllableAuthRepo,
      cycleRepo = controllableCycleRepo,
      spaceRepo = controllableSpaceRepo,
      partnerRepo = controllablePartnerRepo
    )
  }

  private suspend fun createRealTestUser(prefix: String): FirebaseUser {
    val email = "${prefix}_${UUID.randomUUID().toString().take(8)}@test.com"
    val result = auth.createUserWithEmailAndPassword(email, "password123").await()
    return result.user!!
  }

  private fun setupCoherentSession(user: FirebaseUser, spaceId: String): String {
    val space = CoupleSpace(
      id = spaceId,
      ownerId = user.uid,
      partnerId = null,
      status = SpaceStatus.UNPAIRED.name,
      averageCycleLength = 28,
      averagePeriodLength = 5,
      sharingPermissions = SharingPermissions()
    )
    controllableSpaceRepo.spaceMap[user.uid] = space
    controllableAuthRepo.emitUser(user)

    val sessionId = UUID.randomUUID().toString()
    viewModel.currentSessionId = sessionId
    viewModel.activeObservedUserId = user.uid

    viewModel.setUiStateForTesting { state ->
      state.copy(
        currentUser = user,
        currentRole = UserRole.PRIMARY_ELLA,
        space = space,
        isGuestMode = false,
        todaySymptom = null,
        activePairingInvite = null,
        errorMessage = null,
        successMessage = null
      )
    }
    return sessionId
  }

  private fun <T> runWithLooper(timeoutMs: Long = 25000, block: suspend () -> T): T {
    val deferred = CoroutineScope(Dispatchers.IO).async { block() }
    val start = System.currentTimeMillis()
    while (!deferred.isCompleted && (System.currentTimeMillis() - start < timeoutMs)) {
      ShadowLooper.idleMainLooper()
      Thread.sleep(40)
    }
    if (!deferred.isCompleted) {
      throw java.util.concurrent.TimeoutException("runWithLooper timed out after ${timeoutMs}ms")
    }
    return runBlocking { deferred.await() }
  }

  // --- PRUEBA 1: saveSymptom éxito tardío de A NO fuga a Cuenta C ---
  @Test
  fun `saveSymptom late success from user A does not leak into user C session`() {
    runWithLooper {
      val userA = createRealTestUser("userA")
      val spaceA = "space_A"
      val sessionA = setupCoherentSession(userA, spaceA)

      val callbackDeferred = CompletableDeferred<Pair<Boolean, String?>>()

      // 1. Invocar guardado real en ViewModel
      val operationJob = viewModel.saveSymptom(
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
        callbackDeferred.complete(Pair(success, err))
      }

      // 2. Señal 1: Esperar de forma determinista a que el repositorio confirme recepción
      val receivedSymptom = withTimeout(5000) { controllableCycleRepo.saveSymptomCalled.await() }
      assertNotNull("Operation MUST reach repository or test fails", receivedSymptom)
      assertEquals(spaceA, receivedSymptom.spaceId)
      assertEquals(userA.uid, receivedSymptom.loggedBy)
      assertEquals("Nota confidencial de A", receivedSymptom.publicNote)
      assertFalse("Operation must still be pending", controllableCycleRepo.saveSymptomDeferred.isCompleted)
      assertFalse("Callback must not be invoked yet", callbackDeferred.isCompleted)
      assertFalse("Job must not be completed yet", operationJob.isCompleted)

      // 3. Cerrar sesión de A mediante ruta real
      val testScope = CoroutineScope(Dispatchers.Unconfined)
      viewModel.signOut(testScope)

      // 4. Activar sesión C
      val userC = createRealTestUser("userC")
      val spaceC = "space_C"
      val sessionC = setupCoherentSession(userC, spaceC)
      assertNotEquals("Session ID must be rotated", sessionA, sessionC)

      // 5. Completar la operación de A
      controllableCycleRepo.saveSymptomDeferred.complete(Unit)

      // Sincronización determinista sin Thread.sleep
      withTimeout(5000) { operationJob.join() }
      assertTrue("ViewModel job must be completed", operationJob.isCompleted)

      // 6. Verificación de blindaje: callback NO debe ser entregado para evitar contaminación de formulario en sesión invalidada
      assertFalse("Late response must not deliver callback to invalidated session", callbackDeferred.isCompleted)

      val stateC = viewModel.uiState.value
      assertNull("User C must NOT receive User A symptom", stateC.todaySymptom)
      assertEquals("User C space must remain space_C", spaceC, stateC.space?.id)
      assertEquals(userC.uid, stateC.currentUser?.uid)
    }
  }

  // --- PRUEBA 2: saveSymptom error tardío de A NO fuga a Cuenta C ---
  @Test
  fun `saveSymptom late error from user A does not leak error message into user C session`() {
    runWithLooper {
      val userA = createRealTestUser("userA")
      val spaceA = "space_A"
      setupCoherentSession(userA, spaceA)

      val callbackDeferred = CompletableDeferred<Pair<Boolean, String?>>()

      val operationJob = viewModel.saveSymptom(
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
        callbackDeferred.complete(Pair(success, err))
      }

      val receivedSymptom = withTimeout(5000) { controllableCycleRepo.saveSymptomCalled.await() }
      assertNotNull(receivedSymptom)
      assertFalse("Operation must still be pending", controllableCycleRepo.saveSymptomDeferred.isCompleted)
      assertFalse("Job must not be completed yet", operationJob.isCompleted)
      assertFalse("Callback must not be invoked yet", callbackDeferred.isCompleted)

      val testScope = CoroutineScope(Dispatchers.Unconfined)
      viewModel.signOut(testScope)

      val userC = createRealTestUser("userC")
      val spaceC = "space_C"
      setupCoherentSession(userC, spaceC)

      // Falla la operación de A
      val networkEx = RuntimeException("Error fatal en la red de A")
      controllableCycleRepo.saveSymptomDeferred.completeExceptionally(networkEx)

      // Sincronización determinista sin Thread.sleep
      withTimeout(5000) { operationJob.join() }
      assertTrue("ViewModel job must be completed", operationJob.isCompleted)

      assertFalse("Failure callback must not be delivered to invalidated session", callbackDeferred.isCompleted)
      assertNull("User C errorMessage must NOT be contaminated with User A error", viewModel.uiState.value.errorMessage)
    }
  }

  // --- PRUEBA 3: generatePairingCode tardío de A NO fuga a Cuenta C ---
  @Test
  fun `generatePairingCode late completion from user A does not update user C activePairingCode`() {
    runWithLooper {
      val userA = createRealTestUser("userA")
      val spaceA = "space_A"
      setupCoherentSession(userA, spaceA)

      val operationJob = viewModel.generatePairingCode()

      // Señal 1: Confirmar que entró al repositorio con los argumentos de A
      val (calledSpace, calledUser) = withTimeout(5000) { controllableSpaceRepo.generateCodeCalled.await() }
      assertEquals(spaceA, calledSpace)
      assertEquals(userA.uid, calledUser)
      assertFalse("Operation must still be pending", controllableSpaceRepo.generateCodeDeferred.isCompleted)
      assertFalse("Job must not be completed yet", operationJob.isCompleted)

      val testScope = CoroutineScope(Dispatchers.Unconfined)
      viewModel.signOut(testScope)

      val userC = createRealTestUser("userC")
      val spaceC = "space_C"
      setupCoherentSession(userC, spaceC)

      // Se resuelve el código de A
      controllableSpaceRepo.generateCodeDeferred.complete("LUNA-AAAA")

      // Señal 2: Sincronización determinista mediante Job.join() sin Thread.sleep
      withTimeout(5000) { operationJob.join() }
      assertTrue("Operation job must be completed", operationJob.isCompleted)

      assertNull("User C must NOT receive User A pairing code", viewModel.uiState.value.activePairingCode)
      assertEquals(spaceC, viewModel.uiState.value.space?.id)
    }
  }

  // --- PRUEBA 4: joinWithCode tardío de A NO altera el espacio de Cuenta C ---
  @Test
  fun `joinWithCode late completion from user A does not affect user C space`() {
    runWithLooper {
      val userA = createRealTestUser("userA")
      val spaceA = "space_A"
      setupCoherentSession(userA, spaceA)

      val operationJob = viewModel.joinWithCode("LUNA-JOIN")

      // Señal 1: Confirmar que entró al repositorio
      val (calledCode, calledPartner) = withTimeout(5000) { controllableSpaceRepo.joinCodeCalled.await() }
      assertEquals("LUNA-JOIN", calledCode)
      assertEquals(userA.uid, calledPartner)
      assertFalse("Operation must still be pending", controllableSpaceRepo.joinCodeDeferred.isCompleted)
      assertFalse("Job must not be completed yet", operationJob.isCompleted)

      val testScope = CoroutineScope(Dispatchers.Unconfined)
      viewModel.signOut(testScope)

      val userC = createRealTestUser("userC")
      val spaceC = "space_C"
      setupCoherentSession(userC, spaceC)

      controllableSpaceRepo.joinCodeDeferred.complete(Result.success("space_from_code"))

      // Señal 2: Sincronización determinista mediante Job.join() sin Thread.sleep
      withTimeout(5000) { operationJob.join() }
      assertTrue("Operation job must be completed", operationJob.isCompleted)

      assertEquals("User C space must remain space_C", spaceC, viewModel.uiState.value.space?.id)
    }
  }

  // --- PRUEBA 5: acceptSpottingSuggestion tardío de A NO altera Cuenta C ---
  @Test
  fun `acceptSpottingSuggestion late completion from user A does not alter user C state`() {
    runWithLooper {
      val userA = createRealTestUser("userA")
      val spaceA = "space_A"
      setupCoherentSession(userA, spaceA)

      val suggestion = PartnerObservation(
        id = "obs_A",
        spaceId = spaceA,
        date = "2026-06-15",
        unloggedSpottingReported = true,
        spottingSuggestionStatus = SpottingSuggestionStatus.PENDING.name
      )
      viewModel.setUiStateForTesting { it.copy(activeSpottingSuggestion = suggestion) }

      val operationJob = viewModel.acceptSpottingSuggestion()

      // Señal 1: Confirmar que entró al repositorio
      val (calledSpace, calledObs, calledUser) = withTimeout(5000) { controllableCycleRepo.acceptSpottingCalled.await() }
      assertEquals(spaceA, calledSpace)
      assertEquals("obs_A", calledObs)
      assertEquals(userA.uid, calledUser)
      assertFalse("Operation must still be pending", controllableCycleRepo.acceptSpottingDeferred.isCompleted)
      assertFalse("Job must not be completed yet", operationJob.isCompleted)

      val testScope = CoroutineScope(Dispatchers.Unconfined)
      viewModel.signOut(testScope)

      val userC = createRealTestUser("userC")
      val spaceC = "space_C"
      setupCoherentSession(userC, spaceC)

      controllableCycleRepo.acceptSpottingDeferred.complete(Result.success(Unit))

      // Señal 2: Sincronización determinista mediante Job.join() sin Thread.sleep
      withTimeout(5000) { operationJob.join() }
      assertTrue("Operation job must be completed", operationJob.isCompleted)

      assertNull("User C must NOT receive User A successMessage", viewModel.uiState.value.successMessage)
      assertNull(viewModel.uiState.value.activeSpottingSuggestion)
    }
  }

  // --- PRUEBA 6 (Requisito I): Cierre y reapertura de la MISMA cuenta invalida la sesión anterior ---
  @Test
  fun `same account sign-out and re-entry invalidates operations from prior session`() {
    runWithLooper {
      val userA = createRealTestUser("userA_same")
      val spaceA = "space_A"
      val session1 = setupCoherentSession(userA, spaceA)

      val callbackDeferred = CompletableDeferred<Pair<Boolean, String?>>()

      // 1. Inicia guardado bajo la sesión 1
      val operationJob = viewModel.saveSymptom(
        flowLevel = FlowLevel.MEDIUM,
        painLevel = 3,
        mood = Mood.ANXIOUS,
        energyLevel = 2,
        cramps = true,
        headache = false,
        backPain = false,
        bloating = false,
        publicNote = "Nota sesión 1",
        privateNote = null,
        isPrivate = false
      ) { success, err ->
        callbackDeferred.complete(Pair(success, err))
      }

      // Señal 1: Recepción en repositorio
      val received = withTimeout(5000) { controllableCycleRepo.saveSymptomCalled.await() }
      assertNotNull(received)
      assertFalse("Operation must still be pending", controllableCycleRepo.saveSymptomDeferred.isCompleted)
      assertFalse("Job must not be completed yet", operationJob.isCompleted)
      assertFalse("Callback must not be invoked yet", callbackDeferred.isCompleted)

      // 2. Usuario A cierra sesión
      val testScope = CoroutineScope(Dispatchers.Unconfined)
      viewModel.signOut(testScope)

      // 3. El MISMO usuario A vuelve a iniciar sesión (mismo UID, nueva identidad de sesión)
      val session2 = setupCoherentSession(userA, spaceA)
      assertNotEquals("Session ID must be rotated upon re-entry", session1, session2)
      assertEquals(userA.uid, viewModel.activeObservedUserId)

      // 4. Se completa la operación pendiente de la sesión 1
      controllableCycleRepo.saveSymptomDeferred.complete(Unit)

      withTimeout(5000) { operationJob.join() }
      assertTrue("Operation job must be completed", operationJob.isCompleted)

      // 5. La continuación debe descartarse por sessionId obsoleto: no entregar callback a nueva sesión
      assertFalse("Stale operation from prior session must NOT deliver callback even for same user", callbackDeferred.isCompleted)
      assertNull("Session 2 todaySymptom must remain null without stale overwrite", viewModel.uiState.value.todaySymptom)
    }
  }

  // --- PRUEBA 7: logPartnerObservation éxito en sesión original ---
  @Test
  fun `logPartnerObservation original session success delivers callback and updates state`() {
    runWithLooper {
      val userPartner = createRealTestUser("partner_ok")
      val space = "space_partner_1"
      setupCoherentSession(userPartner, space)

      val callbackDeferred = CompletableDeferred<Pair<Boolean, String?>>()

      val job = viewModel.logPartnerObservation(
        perceivedMood = PerceivedMood.CALM,
        apparentPainLevel = 2,
        unloggedSpottingReported = true,
        careActionTaken = "Té de manzanilla",
        sweetMessage = "Te amo",
        onComplete = { success, err ->
          callbackDeferred.complete(Pair(success, err))
        }
      )

      val obsCalled = withTimeout(5000) { controllablePartnerRepo.logObservationCalled.await() }
      assertNotNull(obsCalled)
      assertEquals("CALM", obsCalled.perceivedMood)

      controllablePartnerRepo.logObservationDeferred.complete(Unit)
      val (success, err) = withTimeout(5000) { callbackDeferred.await() }
      job.join()

      assertTrue("Must report success in valid session", success)
      assertNull("No error on success", err)
      assertEquals("¡Observación y cuidado registrados!", viewModel.uiState.value.successMessage)
    }
  }

  // --- PRUEBA 8: logPartnerObservation error sanitizado en sesión original ---
  @Test
  fun `logPartnerObservation original session error delivers friendly sanitized error`() {
    runWithLooper {
      val userPartner = createRealTestUser("partner_err")
      val space = "space_partner_2"
      setupCoherentSession(userPartner, space)

      val callbackDeferred = CompletableDeferred<Pair<Boolean, String?>>()

      val job = viewModel.logPartnerObservation(
        perceivedMood = PerceivedMood.TIRED,
        apparentPainLevel = 3,
        unloggedSpottingReported = false,
        careActionTaken = null,
        sweetMessage = null,
        onComplete = { success, err ->
          callbackDeferred.complete(Pair(success, err))
        }
      )

      withTimeout(5000) { controllablePartnerRepo.logObservationCalled.await() }

      val rawFirebaseEx = RuntimeException("PERMISSION_DENIED: Missing rules permission on Firestore collection")
      controllablePartnerRepo.logObservationDeferred.completeExceptionally(rawFirebaseEx)

      val (success, err) = withTimeout(5000) { callbackDeferred.await() }
      job.join()

      assertFalse("Must report failure", success)
      assertNotNull(err)
      assertFalse("Must NOT leak raw Firebase exception to user", err!!.contains("Firestore"))
      assertEquals("No tienes permisos suficientes para realizar esta acción.", err)
      assertEquals(err, viewModel.uiState.value.errorMessage)
    }
  }

  // --- PRUEBA 9: logPartnerObservation cambio de sesión antes de error NO entrega callback ---
  @Test
  fun `logPartnerObservation session switch before error does NOT deliver callback to new session`() {
    runWithLooper {
      val userA = createRealTestUser("partner_A")
      val spaceA = "space_A"
      setupCoherentSession(userA, spaceA)

      val callbackDeferred = CompletableDeferred<Pair<Boolean, String?>>()

      val job = viewModel.logPartnerObservation(
        perceivedMood = PerceivedMood.SAD,
        apparentPainLevel = 1,
        unloggedSpottingReported = false,
        careActionTaken = null,
        sweetMessage = null,
        onComplete = { success, err ->
          callbackDeferred.complete(Pair(success, err))
        }
      )

      withTimeout(5000) { controllablePartnerRepo.logObservationCalled.await() }

      // Usuario B inicia sesión antes de que A termine
      val testScope = CoroutineScope(Dispatchers.Unconfined)
      viewModel.signOut(testScope)

      val userB = createRealTestUser("partner_B")
      setupCoherentSession(userB, "space_B")

      // Falla la operación de A
      controllablePartnerRepo.logObservationDeferred.completeExceptionally(RuntimeException("UNAVAILABLE: network offline"))

      job.join()
      assertTrue(job.isCompleted)

      assertFalse("Callback from invalidated session must NOT be delivered to new session", callbackDeferred.isCompleted)
      assertNull("User B errorMessage must not be contaminated", viewModel.uiState.value.errorMessage)
    }
  }

  // --- PRUEBA 10: sendCheckIn cambio de sesión antes de éxito NO entrega callback ---
  @Test
  fun `sendCheckIn session switch before success does NOT deliver callback to new session`() {
    runWithLooper {
      val userA = createRealTestUser("checkin_A")
      setupCoherentSession(userA, "space_checkin_A")

      val callbackDeferred = CompletableDeferred<Pair<Boolean, String?>>()

      val job = viewModel.sendCheckIn(
        promptType = CheckInPromptType.HOW_ARE_YOU_FEELING,
        customNote = "Nota A",
        onComplete = { success, err ->
          callbackDeferred.complete(Pair(success, err))
        }
      )

      withTimeout(5000) { controllablePartnerRepo.sendCheckInCalled.await() }

      val testScope = CoroutineScope(Dispatchers.Unconfined)
      viewModel.signOut(testScope)

      val userB = createRealTestUser("checkin_B")
      setupCoherentSession(userB, "space_checkin_B")

      controllablePartnerRepo.sendCheckInDeferred.complete(Unit)

      job.join()
      assertTrue(job.isCompleted)

      assertFalse("Callback from stale session must NOT be delivered to new session", callbackDeferred.isCompleted)
      assertNull("User B must NOT receive User A successMessage", viewModel.uiState.value.successMessage)
    }
  }
}
