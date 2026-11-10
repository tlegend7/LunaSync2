package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CoupleSpace
import com.example.data.model.CycleEntry
import com.example.data.model.FlowLevel
import com.example.data.model.Mood
import com.example.data.model.SharingPermissions
import com.example.data.model.SpaceStatus
import com.example.data.model.UserRole
import com.example.data.repository.AuthRepository
import com.example.data.repository.CoupleSpaceRepository
import com.example.data.repository.CycleRepository
import com.example.data.repository.IntimacyRepository
import com.example.data.repository.PartnerSupportRepository
import com.example.ui.viewmodel.MainViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class TestControlledAuthRepository(context: Context) : AuthRepository(context) {
  private val _testUserFlow = MutableStateFlow<FirebaseUser?>(null)
  override val currentUser: StateFlow<FirebaseUser?> = _testUserFlow.asStateFlow()

  fun emitUser(user: FirebaseUser?) {
    _testUserFlow.value = user
  }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SessionAndCalendarIntegrationTest {

  private lateinit var application: Application
  private lateinit var db: FirebaseFirestore
  private lateinit var auth: FirebaseAuth
  private lateinit var cycleRepo: CycleRepository
  private lateinit var coupleRepo: CoupleSpaceRepository
  private lateinit var partnerRepo: PartnerSupportRepository
  private lateinit var intimacyRepo: IntimacyRepository
  private lateinit var testAuthRepo: TestControlledAuthRepository

  // Fixed clock for deterministic integration tests: 2026-06-15 UTC
  private val fixedDate = LocalDate.of(2026, 6, 15)
  private val fixedClock = Clock.fixed(fixedDate.atStartOfDay(ZoneId.of("UTC")).toInstant(), ZoneId.of("UTC"))

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
    db = FirebaseFirestore.getInstance()
    try {
      db.useEmulator("127.0.0.1", 8085)
    } catch (_: Exception) {}

    auth = FirebaseAuth.getInstance()
    try {
      auth.useEmulator("127.0.0.1", 9099)
    } catch (_: Exception) {}

    cycleRepo = CycleRepository(application, db)
    coupleRepo = CoupleSpaceRepository(application, db)
    partnerRepo = PartnerSupportRepository(application, db)
    intimacyRepo = IntimacyRepository(application, db)
    testAuthRepo = TestControlledAuthRepository(application)
  }

  private fun createVm(): MainViewModel {
    return MainViewModel(
      application = application,
      authRepo = testAuthRepo,
      spaceRepo = coupleRepo,
      cycleRepo = cycleRepo,
      intimacyRepo = intimacyRepo,
      partnerRepo = partnerRepo
    ).apply {
      this.clock = fixedClock
    }
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

  private suspend fun setupCoherentSession(vm: MainViewModel, user: FirebaseUser, spaceId: String): CoupleSpace {
    user.email?.let {
      try { auth.signInWithEmailAndPassword(it, "password123").await() } catch (_: Exception) {}
    }
    val space = CoupleSpace(
      id = spaceId,
      ownerId = user.uid,
      partnerId = null,
      status = SpaceStatus.UNPAIRED.name,
      averageCycleLength = 28,
      averagePeriodLength = 5,
      sharingPermissions = SharingPermissions()
    )
    testAuthRepo.emitUser(user)

    val sessionId = UUID.randomUUID().toString()
    vm.currentSessionId = sessionId
    vm.activeObservedUserId = user.uid

    vm.setUiStateForTesting { state ->
      state.copy(
        currentUser = user,
        currentRole = UserRole.PRIMARY_ELLA,
        space = space,
        isGuestMode = false,
        todaySymptom = null,
        activePairingCode = null,
        errorMessage = null,
        successMessage = null
      )
    }
    return space
  }

  @Test
  fun testNewSpaceHasNoFictitiousMenstrualSeed() = runWithLooper {
    val email = "ella_new_${UUID.randomUUID()}@test.com"
    val authResult = auth.createUserWithEmailAndPassword(email, "password123").await()
    val user = authResult.user!!

    val vm = createVm()
    testAuthRepo.emitUser(user)
    val sessionId = UUID.randomUUID().toString()
    vm.currentSessionId = sessionId
    vm.activeObservedUserId = user.uid

    vm.setUiStateForTesting { state ->
      state.copy(
        currentUser = user,
        currentRole = UserRole.PRIMARY_ELLA,
        space = null,
        isGuestMode = false,
        pastCycles = emptyList(),
        cycleStatus = null
      )
    }

    // Execute space creation through the real ViewModel function
    vm.createOwnerSpace().join()

    val createdSpace = vm.uiState.value.space
    assertNotNull("Space must be created and set in ViewModel state", createdSpace)
    val spaceId = createdSpace!!.id

    // Check cycles subcollection in Firestore is completely empty
    val cyclesSnap = db.collection("couple_spaces").document(spaceId).collection("cycles").get().await()
    assertTrue("Newly created space must not have any cycles seeded in Firestore", cyclesSnap.isEmpty)

    // Check ViewModel state
    vm.refreshCycleCalculations()
    val state = vm.uiState.value
    assertTrue("A freshly created space must have an empty pastCycles list", state.pastCycles.isEmpty())
    assertNull("A freshly created space must have null cycleStatus", state.cycleStatus)
  }

  @Test
  fun testCalendarSelectionDoesNotAlterTodayCycleStatusOrCareTips() = runWithLooper {
    val email = "ella_cal_${UUID.randomUUID()}@test.com"
    val authResult = auth.createUserWithEmailAndPassword(email, "password123").await()
    val user = authResult.user!!

    val vm = createVm()
    val spaceId = coupleRepo.createSpace(user.uid, 28, 5)
    setupCoherentSession(vm, user, spaceId)

    // Record a genuine period starting 10 days before today (June 5, 2026)
    val periodStart = fixedDate.minusDays(10) // 2026-06-05
    cycleRepo.recordPeriodStart(spaceId, periodStart.toString(), periodStart.plusDays(4).toString())

    val cyclesList = listOf(
      CycleEntry(
        id = "c1",
        spaceId = spaceId,
        startDate = periodStart.toString(),
        endDate = periodStart.plusDays(4).toString(),
        periodDuration = 5,
        cycleLength = null,
        isCalculated = false
      )
    )
    vm.setUiStateForTesting { it.copy(pastCycles = cyclesList) }
    vm.refreshCycleCalculations()

    val todayStatus = vm.uiState.value.cycleStatus
    assertNotNull(todayStatus)
    assertEquals(11, todayStatus!!.currentDay) // June 5 to June 15 = 10 days passed -> Day 11

    // Navigate to a date in the past (e.g. May 1)
    val pastDate = LocalDate.of(2026, 5, 1)
    vm.selectDate(pastDate)

    // State selectedDate is updated
    assertEquals(pastDate, vm.uiState.value.selectedDate)
    assertNotNull(vm.uiState.value.selectedDayDetail)

    // CRITICAL: Today's cycleStatus must remain unchanged at Day 11!
    assertEquals(11, vm.uiState.value.cycleStatus!!.currentDay)
    assertEquals(todayStatus.daysUntilNextPeriod, vm.uiState.value.cycleStatus!!.daysUntilNextPeriod)
  }

  @Test
  fun testSavingSymptomForSelectedDateSavesToExactDate() = runWithLooper {
    val email = "ella_symp_${UUID.randomUUID()}@test.com"
    val authResult = auth.createUserWithEmailAndPassword(email, "password123").await()
    val user = authResult.user!!
    auth.signInWithEmailAndPassword(email, "password123").await()

    val vm = createVm()
    val spaceId = coupleRepo.createSpace(user.uid, 28, 5)
    setupCoherentSession(vm, user, spaceId)

    val pastDate = fixedDate.minusDays(3) // 2026-06-12
    vm.selectDate(pastDate)

    var saveCallbackSuccess = false
    val saveJob = vm.saveSymptom(
      date = pastDate,
      flowLevel = FlowLevel.LIGHT,
      painLevel = 2,
      mood = Mood.CALM,
      energyLevel = 4,
      cramps = true,
      headache = false,
      backPain = false,
      bloating = false,
      cervicalMucus = com.example.data.model.CervicalMucus.NONE,
      tenderBreasts = false,
      cravings = false,
      acne = false,
      publicNote = "Nota histórica",
      privateNote = null,
      isPrivate = false,
      onComplete = { success, err ->
        saveCallbackSuccess = success
        if (!success) {
          throw AssertionError("Save symptom failed with error: $err")
        }
      }
    )
    saveJob.join()

    assertTrue("Save symptom callback succeeded", saveCallbackSuccess)

    // Check allSymptoms in ViewModel has entry for pastDate
    val symptom = vm.uiState.value.allSymptoms.firstOrNull { it.date == pastDate.toString() }
    assertNotNull("Symptom must exist for past date", symptom)
    assertEquals("LIGHT", symptom!!.flowLevel)
    assertEquals(true, symptom.cramps)

    // Today's symptom must remain null if none was logged for today
    assertNull("Today's symptom should still be null", vm.uiState.value.todaySymptom)
  }

  @Test
  fun testRejectFutureEvents() = runWithLooper {
    val email = "ella_future_${UUID.randomUUID()}@test.com"
    val authResult = auth.createUserWithEmailAndPassword(email, "password123").await()
    val user = authResult.user!!

    val vm = createVm()
    val spaceId = coupleRepo.createSpace(user.uid, 28, 5)
    setupCoherentSession(vm, user, spaceId)

    val futureDate = fixedDate.plusDays(2) // 2026-06-17

    var symptomRejected = false
    var symptomErrMsg: String? = null
    vm.saveSymptom(
      date = futureDate,
      flowLevel = FlowLevel.MEDIUM,
      painLevel = 1,
      mood = Mood.HAPPY,
      energyLevel = 3,
      cramps = false,
      headache = false,
      backPain = false,
      bloating = false,
      cervicalMucus = com.example.data.model.CervicalMucus.NONE,
      tenderBreasts = false,
      cravings = false,
      acne = false,
      publicNote = null,
      privateNote = null,
      isPrivate = false,
      onComplete = { success, err ->
        symptomRejected = !success
        symptomErrMsg = err
      }
    ).join()

    assertTrue("Future symptom must be rejected", symptomRejected)
    assertTrue(symptomErrMsg?.contains("futura") == true)

    var intimacyRejected = false
    var intimacyErrMsg: String? = null
    vm.logIntimacy(
      date = futureDate,
      encounterType = com.example.data.model.EncounterType.PROTECTED,
      usedLubricant = false,
      hadOrgasm = true,
      postContactEffect = com.example.data.model.PostContactEffect.NONE,
      notes = null,
      isPrivate = false,
      onComplete = { success, err ->
        intimacyRejected = !success
        intimacyErrMsg = err
      }
    ).join()

    assertTrue("Future intimacy must be rejected", intimacyRejected)
    assertTrue(intimacyErrMsg?.contains("futura") == true)
  }

  @Test
  fun testFormRetainsValuesOnError() = runWithLooper {
    val email = "ella_error_${UUID.randomUUID()}@test.com"
    val authResult = auth.createUserWithEmailAndPassword(email, "password123").await()
    val user = authResult.user!!

    val vm = createVm()
    val spaceId = coupleRepo.createSpace(user.uid, 28, 5)
    setupCoherentSession(vm, user, spaceId)

    // Attempting to record period with endDate before startDate
    var errorReceived: String? = null
    val invalidStart = fixedDate.minusDays(5)
    val invalidEnd = fixedDate.minusDays(8) // End is before start!

    val job = vm.recordPeriodStart(
      startDate = invalidStart,
      endDate = invalidEnd,
      onComplete = { success, err ->
        if (!success) errorReceived = err
      }
    )
    job.join()

    assertNotNull("Error message must be set", errorReceived)
    assertTrue(errorReceived?.contains("anterior") == true)

    // ViewModel error message state is preserved and input dates remain available for correction
    assertEquals("La fecha de fin no puede ser anterior a la de inicio", vm.uiState.value.errorMessage)
  }

  @Test
  fun testViewModelSaveSymptomWithExplicitDateSavesToThatDate() = runWithLooper {
    val email = "ella_hoy_${UUID.randomUUID()}@test.com"
    val authResult = auth.createUserWithEmailAndPassword(email, "password123").await()
    val user = authResult.user!!

    val vm = createVm()
    val spaceId = coupleRepo.createSpace(user.uid, 28, 5)
    setupCoherentSession(vm, user, spaceId)

    // 1. User navigates in Calendar to 5 days ago
    val pastDate = fixedDate.minusDays(5)
    vm.selectDate(pastDate)
    assertEquals(pastDate, vm.uiState.value.selectedDate)

    // 2. User goes back to 'Hoy' tab and clicks to log symptoms.
    // In MainScreen, clicking from Hoy captures viewModel.getToday().
    val capturedDateFromHoy = vm.getToday()
    assertEquals(fixedDate, capturedDateFromHoy)

    var saveSuccess = false
    vm.saveSymptom(
      date = capturedDateFromHoy,
      flowLevel = FlowLevel.MEDIUM,
      painLevel = 1,
      mood = Mood.HAPPY,
      energyLevel = 3,
      cramps = false,
      headache = false,
      backPain = false,
      bloating = false,
      cervicalMucus = com.example.data.model.CervicalMucus.NONE,
      tenderBreasts = false,
      cravings = false,
      acne = false,
      publicNote = "Registro desde Hoy",
      privateNote = null,
      isPrivate = false,
      onComplete = { success, _ -> saveSuccess = success }
    ).join()

    assertTrue("Save must succeed", saveSuccess)
    // Symptom was saved for Today (fixedDate), NOT for pastDate
    assertNotNull("Today symptom must be populated in state", vm.uiState.value.todaySymptom)
    assertEquals(fixedDate.toString(), vm.uiState.value.todaySymptom!!.date)
    assertNull("No symptom was erroneously created for the past date", vm.uiState.value.allSymptoms.firstOrNull { it.date == pastDate.toString() })
  }

  @Test
  fun testViewModelLogIntimacyWithExplicitDateDecoupledFromStateSelectedDate() = runWithLooper {
    val email = "ella_decouple_${UUID.randomUUID()}@test.com"
    val authResult = auth.createUserWithEmailAndPassword(email, "password123").await()
    val user = authResult.user!!

    val vm = createVm()
    val spaceId = coupleRepo.createSpace(user.uid, 28, 5)
    setupCoherentSession(vm, user, spaceId)

    // 1. Calendar is on pastDate (e.g. 4 days ago)
    val chosenDate = fixedDate.minusDays(4)
    vm.selectDate(chosenDate)

    // 2. Form opens and captures targetDate immutable snapshot
    val capturedDialogTargetDate = chosenDate

    // 3. User or async state mutates vm.uiState.selectedDate to another date (e.g. 10 days ago)
    val subsequentDate = fixedDate.minusDays(10)
    vm.selectDate(subsequentDate)
    assertEquals(subsequentDate, vm.uiState.value.selectedDate)

    // 4. Form submits using its capturedDialogTargetDate
    var saveSuccess = false
    var saveErrMsg: String? = null
    vm.logIntimacy(
      date = capturedDialogTargetDate,
      encounterType = com.example.data.model.EncounterType.PROTECTED,
      usedLubricant = true,
      hadOrgasm = true,
      postContactEffect = com.example.data.model.PostContactEffect.NONE,
      notes = "Encuentro protegido",
      isPrivate = false,
      onComplete = { success, err ->
        saveSuccess = success
        saveErrMsg = err
      }
    ).join()

    assertTrue("Intimacy save must succeed: $saveErrMsg", saveSuccess)
    val savedLog = vm.uiState.value.intimacyLogs.firstOrNull { it.date == capturedDialogTargetDate.toString() }
    assertNotNull("Intimacy log must be saved for the captured date", savedLog)
    assertNull("No log should exist for the subsequently mutated date", vm.uiState.value.intimacyLogs.firstOrNull { it.date == subsequentDate.toString() })
  }
}
