package com.example

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CoupleSpace
import com.example.data.model.SharingPermissions
import com.example.data.model.SpaceStatus
import com.example.data.model.UserRole
import com.example.data.repository.CoupleSpaceRepository
import com.example.data.repository.CycleRepository
import com.example.data.repository.IntimacyRepository
import com.example.data.repository.PartnerSupportRepository
import com.example.ui.screens.MainScreen
import com.example.ui.viewmodel.MainViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainScreenConnectedFormComposeTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  private lateinit var application: Application
  private lateinit var db: FirebaseFirestore
  private lateinit var auth: FirebaseAuth
  private lateinit var cycleRepo: CycleRepository
  private lateinit var coupleRepo: CoupleSpaceRepository
  private lateinit var partnerRepo: PartnerSupportRepository
  private lateinit var intimacyRepo: IntimacyRepository
  private lateinit var testAuthRepo: TestControlledAuthRepository

  // Deterministic reference date: Monday, June 15, 2026 UTC
  private val fixedDate = LocalDate.of(2026, 6, 15)
  private val fixedClock = Clock.fixed(fixedDate.atStartOfDay(ZoneId.of("UTC")).toInstant(), ZoneId.of("UTC"))

  private val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("es", "ES"))

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

  private fun createVm(referenceDate: LocalDate = fixedDate): MainViewModel {
    val controlledClock = Clock.fixed(referenceDate.atStartOfDay(ZoneId.of("UTC")).toInstant(), ZoneId.of("UTC"))
    return MainViewModel(
      application = application,
      authRepo = testAuthRepo,
      spaceRepo = coupleRepo,
      cycleRepo = cycleRepo,
      intimacyRepo = intimacyRepo,
      partnerRepo = partnerRepo
    ).apply {
      this.clock = controlledClock
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

  private fun waitForCondition(timeoutMs: Long = 10000, condition: () -> Boolean) {
    val start = System.currentTimeMillis()
    while (!condition() && (System.currentTimeMillis() - start < timeoutMs)) {
      ShadowLooper.idleMainLooper()
      composeTestRule.waitForIdle()
      Thread.sleep(50)
    }
    if (!condition()) {
      throw AssertionError("Condition timed out after ${timeoutMs}ms")
    }
  }

  private suspend fun setupCoherentSession(
    vm: MainViewModel,
    user: FirebaseUser,
    spaceId: String,
    referenceDate: LocalDate = vm.getToday()
  ): CoupleSpace {
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
        selectedDate = referenceDate,
        todaySymptom = null,
        activePairingCode = null,
        errorMessage = null,
        successMessage = null
      )
    }
    vm.updateSelectedDayDetail()
    return space
  }

  /**
   * Recorrido A:
   * Navegar a fecha histórica en calendario -> Volver a pestaña Hoy -> Clic en Registrar Síntomas ->
   * Comprobar que el diálogo muestra la fecha de hoy -> Guardar ->
   * Comprobar que el síntoma se persiste con la fecha de hoy y no con la histórica.
   */
  @Test
  fun testFlowA_OpeningSymptomFromHoyTargetsTodayEvenIfCalendarNavigated() {
    val email = "ella_flow_a_${UUID.randomUUID()}@test.com"
    val (user, spaceId) = runWithLooper {
      val authResult = auth.createUserWithEmailAndPassword(email, "password123").await()
      val u = authResult.user!!
      val sId = coupleRepo.createSpace(u.uid, 28, 5)
      // Pre-populate an active cycle so cycleStatus is not null and CTA is "Registrar Síntomas"
      cycleRepo.recordPeriodStart(sId, fixedDate.minusDays(10), null)
      Pair(u, sId)
    }

    val vm = createVm()
    runWithLooper {
      setupCoherentSession(vm, user, spaceId)
    }

    composeTestRule.setContent {
      MainScreen(viewModel = vm)
    }

    // 1. Navigate to Calendar Tab
    composeTestRule.onNodeWithTag("nav_tab_calendar").performClick()

    // 2. Select a past day (e.g. Day 5 of June 2026)
    composeTestRule.onNodeWithTag("calendar_day_5").performClick()
    val historicalDate = LocalDate.of(2026, 6, 5)
    assertEquals(historicalDate, vm.uiState.value.selectedDate)

    // 3. Return to 'Hoy' Tab
    composeTestRule.onNodeWithTag("nav_tab_dashboard").performClick()

    // 4. Click 'Registrar' (log_symptoms_cta)
    composeTestRule.onNodeWithTag("log_symptoms_cta").performClick()

    // 5. Verify the dialog displays Today's date (June 15, 2026)
    val todayFormatted = fixedDate.format(dateFormatter)
    composeTestRule.onNodeWithTag("symptom_dialog_date_text").assertIsDisplayed()
    composeTestRule.onNodeWithTag("symptom_dialog_date_text").assertTextContains(todayFormatted, substring = true, ignoreCase = true)

    // 6. Select a flow chip and Save
    composeTestRule.onNodeWithTag("flow_chip_LIGHT").performClick()
    composeTestRule.onNodeWithTag("save_symptom_button").performClick()

    // Wait until symptom is persisted
    waitForCondition {
      vm.uiState.value.todaySymptom != null
    }

    // 7. Verify persisted symptom is for Today (2026-06-15) and NOT historicalDate (2026-06-05)
    val todaySymptom = vm.uiState.value.todaySymptom
    assertNotNull("Today's symptom must be persisted", todaySymptom)
    assertEquals(fixedDate.toString(), todaySymptom!!.date)
    assertEquals("LIGHT", todaySymptom.flowLevel)

    val historicalSymptom = vm.uiState.value.allSymptoms.firstOrNull { it.date == historicalDate.toString() }
    assertNull("No symptom must be persisted for the historical date", historicalSymptom)
  }

  /**
   * Recorrido B:
   * Navegar a fecha histórica en calendario -> Volver a pestaña Hoy -> Clic en Registrar Intimidad ->
   * Comprobar que el diálogo muestra la fecha de hoy -> Guardar ->
   * Comprobar que la intimidad se persiste con la fecha de hoy.
   */
  @Test
  fun testFlowB_OpeningIntimacyFromHoyTargetsTodayEvenIfCalendarNavigated() {
    val email = "ella_flow_b_${UUID.randomUUID()}@test.com"
    val (user, spaceId) = runWithLooper {
      val authResult = auth.createUserWithEmailAndPassword(email, "password123").await()
      val u = authResult.user!!
      val sId = coupleRepo.createSpace(u.uid, 28, 5)
      Pair(u, sId)
    }

    val vm = createVm()
    runWithLooper {
      setupCoherentSession(vm, user, spaceId)
    }

    composeTestRule.setContent {
      MainScreen(viewModel = vm)
    }

    // 1. Navigate to Calendar Tab
    composeTestRule.onNodeWithTag("nav_tab_calendar").performClick()

    // 2. Select Day 7 of June 2026
    composeTestRule.onNodeWithTag("calendar_day_7").performClick()
    val pastDate = LocalDate.of(2026, 6, 7)
    assertEquals(pastDate, vm.uiState.value.selectedDate)

    // 3. Return to 'Hoy' Tab
    composeTestRule.onNodeWithTag("nav_tab_dashboard").performClick()

    // 4. Click 'Intimidad' CTA
    composeTestRule.onNodeWithTag("log_intimacy_cta").performClick()

    // 5. Verify the dialog displays Today's date
    val todayFormatted = fixedDate.format(dateFormatter)
    composeTestRule.onNodeWithTag("intimacy_dialog_date_text").assertIsDisplayed()
    composeTestRule.onNodeWithTag("intimacy_dialog_date_text").assertTextContains(todayFormatted, substring = true, ignoreCase = true)

    // 6. Click Save
    composeTestRule.onNodeWithTag("save_intimacy_button").performClick()

    // Wait until intimacy is persisted
    waitForCondition {
      vm.uiState.value.intimacyLogs.any { it.date == fixedDate.toString() }
    }

    // 7. Verify intimacy log is saved for Today (2026-06-15) and NOT pastDate (2026-06-07)
    val todayIntimacy = vm.uiState.value.intimacyLogs.firstOrNull { it.date == fixedDate.toString() }
    assertNotNull("Intimacy log must exist for today", todayIntimacy)

    val pastIntimacy = vm.uiState.value.intimacyLogs.firstOrNull { it.date == pastDate.toString() }
    assertNull("No intimacy log must exist for past date", pastIntimacy)
  }

  /**
   * Recorrido C:
   * Pestaña Calendario -> Seleccionar fecha pasada -> Clic en Registrar Síntomas del día seleccionado ->
   * Guardar -> Comprobar que se persiste con la fecha seleccionada.
   */
  @Test
  fun testFlowC_OpeningSymptomFromCalendarTargetsSelectedDate() {
    val email = "ella_flow_c_${UUID.randomUUID()}@test.com"
    val (user, spaceId) = runWithLooper {
      val authResult = auth.createUserWithEmailAndPassword(email, "password123").await()
      val u = authResult.user!!
      val sId = coupleRepo.createSpace(u.uid, 28, 5)
      Pair(u, sId)
    }

    val vm = createVm()
    runWithLooper {
      setupCoherentSession(vm, user, spaceId)
    }

    composeTestRule.setContent {
      MainScreen(viewModel = vm)
    }

    // 1. Go to Calendar
    composeTestRule.onNodeWithTag("nav_tab_calendar").performClick()

    // 2. Select historical day (e.g. Day 4 of June 2026)
    composeTestRule.onNodeWithTag("calendar_day_4").performClick()
    val targetSelectedDate = LocalDate.of(2026, 6, 4)
    assertEquals(targetSelectedDate, vm.uiState.value.selectedDate)

    // 3. Click 'Registrar síntomas de este día' in the day detail card
    composeTestRule.onNodeWithTag("log_symptoms_for_selected_date_button").performClick()

    // 4. Verify dialog displays the selected date (June 4, 2026)
    val formattedSelectedDate = targetSelectedDate.format(dateFormatter)
    composeTestRule.onNodeWithTag("symptom_dialog_date_text").assertIsDisplayed()
    composeTestRule.onNodeWithTag("symptom_dialog_date_text").assertTextContains(formattedSelectedDate, substring = true, ignoreCase = true)

    // 5. Select flow MEDIUM and Save
    composeTestRule.onNodeWithTag("flow_chip_MEDIUM").performClick()
    composeTestRule.onNodeWithTag("save_symptom_button").performClick()

    // Wait until symptom is saved for June 4
    waitForCondition {
      vm.uiState.value.allSymptoms.any { it.date == targetSelectedDate.toString() }
    }

    // 6. Verify symptom is persisted for June 4, 2026
    val saved = vm.uiState.value.allSymptoms.firstOrNull { it.date == targetSelectedDate.toString() }
    assertNotNull("Symptom must be saved for selected date June 4", saved)
    assertEquals("MEDIUM", saved!!.flowLevel)
  }

  /**
   * Recorrido D:
   * Abrir formulario capturando fecha -> Modificar la fecha seleccionada en segundo plano/estado ->
   * Guardar -> Comprobar que se persiste la fecha que capturó el diálogo al abrirse.
   */
  @Test
  fun testFlowD_CapturedDateDecoupledFromSubsequentStateMutation() {
    val email = "ella_flow_d_${UUID.randomUUID()}@test.com"
    val (user, spaceId) = runWithLooper {
      val authResult = auth.createUserWithEmailAndPassword(email, "password123").await()
      val u = authResult.user!!
      val sId = coupleRepo.createSpace(u.uid, 28, 5)
      Pair(u, sId)
    }

    val vm = createVm()
    runWithLooper {
      setupCoherentSession(vm, user, spaceId)
    }

    composeTestRule.setContent {
      MainScreen(viewModel = vm)
    }

    // 1. Go to Calendar and select Day 3
    composeTestRule.onNodeWithTag("nav_tab_calendar").performClick()
    composeTestRule.onNodeWithTag("calendar_day_3").performClick()
    val capturedDate = LocalDate.of(2026, 6, 3)

    // 2. Open Symptom Dialog for Day 3
    composeTestRule.onNodeWithTag("log_symptoms_for_selected_date_button").performClick()

    // 3. In the background / ViewModel, externally mutate selectedDate to Day 10 on UI thread
    val backgroundMutatedDate = LocalDate.of(2026, 6, 10)
    composeTestRule.runOnUiThread {
      vm.selectDate(backgroundMutatedDate)
    }
    assertEquals(backgroundMutatedDate, vm.uiState.value.selectedDate)

    // 4. Save the open dialog
    composeTestRule.onNodeWithTag("flow_chip_SPOTTING").performClick()
    composeTestRule.onNodeWithTag("save_symptom_button").performClick()

    // Wait until symptom is saved for capturedDate June 3
    waitForCondition {
      vm.uiState.value.allSymptoms.any { it.date == capturedDate.toString() }
    }

    // 5. Verify it was saved for the captured date (June 3), NOT mutated date (June 10)
    val savedOriginal = vm.uiState.value.allSymptoms.firstOrNull { it.date == capturedDate.toString() }
    assertNotNull("Symptom must be saved for captured date June 3", savedOriginal)
    assertEquals("SPOTTING", savedOriginal!!.flowLevel)

    val savedMutated = vm.uiState.value.allSymptoms.firstOrNull { it.date == backgroundMutatedDate.toString() }
    assertNull("No symptom should exist for background mutated date June 10", savedMutated)
  }

  /**
   * Recorrido E:
   * Flujo de período: abrir desde Hoy abre con fecha de hoy; abrir desde Calendario abre con fecha seleccionada;
   * permitir cambiar fechas en el diálogo.
   */
  @Test
  fun testFlowE_PeriodDialogOpeningDatesAndDateChange() {
    val email = "ella_flow_e_${UUID.randomUUID()}@test.com"
    val (user, spaceId) = runWithLooper {
      val authResult = auth.createUserWithEmailAndPassword(email, "password123").await()
      val u = authResult.user!!
      val sId = coupleRepo.createSpace(u.uid, 28, 5)
      Pair(u, sId)
    }

    val vm = createVm()
    runWithLooper {
      setupCoherentSession(vm, user, spaceId)
    }

    composeTestRule.setContent {
      MainScreen(viewModel = vm)
    }

    // Part 1: Open Period Dialog from Hoy (onboarding CTA when no cycles exist)
    composeTestRule.onNodeWithTag("register_period_start_cta").performClick()

    // Verify it opened with Today's date (June 15, 2026)
    val periodDateFormatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM yyyy", Locale("es", "ES"))
    val todayFormatted = fixedDate.format(periodDateFormatter)
    composeTestRule.onNodeWithTag("period_dialog_start_date_text").assertIsDisplayed()
    composeTestRule.onNodeWithTag("period_dialog_start_date_text").assertTextContains(todayFormatted, substring = true, ignoreCase = true)

    // Save period for today
    composeTestRule.onNodeWithTag("save_period_button").performClick()

    waitForCondition {
      vm.uiState.value.pastCycles.any { it.startDate == fixedDate.toString() }
    }

    // Verify Cycle was saved with startDate = 2026-06-15
    val cyclesAfterHoy = vm.uiState.value.pastCycles
    assertTrue("Cycle must be saved for today", cyclesAfterHoy.any { it.startDate == fixedDate.toString() })

    // Part 2: Open Period Dialog from Calendar on a selected past date
    composeTestRule.onNodeWithTag("nav_tab_calendar").performClick()

    // Select Day 2 in current calendar month
    composeTestRule.onNodeWithTag("calendar_day_2").performClick()
    val pastSelectedDate = LocalDate.of(2026, 6, 2)

    composeTestRule.onNodeWithTag("manage_period_from_calendar_button").performClick()

    // Verify Period Dialog opens with the selected date (June 2, 2026)
    val pastFormatted = pastSelectedDate.format(periodDateFormatter)
    composeTestRule.onNodeWithTag("period_dialog_start_date_text").assertIsDisplayed()
    composeTestRule.onNodeWithTag("period_dialog_start_date_text").assertTextContains(pastFormatted, substring = true, ignoreCase = true)

    // Save period for June 2
    composeTestRule.onNodeWithTag("save_period_button").performClick()

    waitForCondition {
      vm.uiState.value.pastCycles.any { it.startDate == pastSelectedDate.toString() }
    }

    val cyclesAfterCalendar = vm.uiState.value.pastCycles
    assertTrue("Cycle must be saved for June 2", cyclesAfterCalendar.any { it.startDate == pastSelectedDate.toString() })
  }

  /**
   * Verificación de reproducibilidad con fecha controlada de otro mes y otro año:
   * Por ejemplo: Miércoles 20 de Octubre de 2027 (2027-10-20).
   * Demuestra que el calendario, el encabezado, el diálogo y el destino de guardado
   * se rigen íntegramente por la fecha controlada y son completamente independientes del reloj del host.
   */
  @Test
  fun testScenarioReproducibilityWithDifferentMonthAndYear_October2027() {
    val controlledDate2027 = LocalDate.of(2027, 10, 20)
    val email = "ella_oct2027_${UUID.randomUUID()}@test.com"
    val (user, spaceId) = runWithLooper {
      val authResult = auth.createUserWithEmailAndPassword(email, "password123").await()
      val u = authResult.user!!
      val sId = coupleRepo.createSpace(u.uid, 28, 5)
      cycleRepo.recordPeriodStart(sId, controlledDate2027.minusDays(10), null)
      Pair(u, sId)
    }

    val vm = createVm(referenceDate = controlledDate2027)
    runWithLooper {
      setupCoherentSession(vm, user, spaceId, referenceDate = controlledDate2027)
    }

    composeTestRule.setContent {
      MainScreen(viewModel = vm)
    }

    // 1. Navegar al calendario
    composeTestRule.onNodeWithTag("nav_tab_calendar").performClick()

    // 2. El mes mostrado inicialmente debe ser Octubre de 2027.
    // Seleccionar día 5 de octubre de 2027 mediante calendar_date_2027-10-05 (o calendar_day_5)
    val octDateFormatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("es", "ES"))
    val pastDayOct5 = LocalDate.of(2027, 10, 5)
    composeTestRule.onNodeWithTag("calendar_day_5").performClick()
    assertEquals(pastDayOct5, vm.uiState.value.selectedDate)

    // 3. Volver a pestaña Hoy
    composeTestRule.onNodeWithTag("nav_tab_dashboard").performClick()

    // 4. Abrir diálogo de síntomas desde Hoy
    composeTestRule.onNodeWithTag("log_symptoms_cta").performClick()

    // El diálogo debe mostrar la fecha controlada: 20 de octubre de 2027
    val expectedFormatted = controlledDate2027.format(octDateFormatter)
    composeTestRule.onNodeWithTag("symptom_dialog_date_text").assertIsDisplayed()
    composeTestRule.onNodeWithTag("symptom_dialog_date_text").assertTextContains(expectedFormatted, substring = true, ignoreCase = true)

    // 5. Guardar síntoma
    composeTestRule.onNodeWithTag("flow_chip_MEDIUM").performClick()
    composeTestRule.onNodeWithTag("save_symptom_button").performClick()

    waitForCondition {
      vm.uiState.value.todaySymptom != null
    }

    // Comprobar persistencia en ViewModel
    val savedToday = vm.uiState.value.todaySymptom
    assertNotNull(savedToday)
    assertEquals(controlledDate2027.toString(), savedToday!!.date)
    assertEquals("MEDIUM", savedToday.flowLevel)

    // Comprobar lectura real persistida en Firestore
    val docInDb = runWithLooper {
      db.collection("couple_spaces").document(spaceId)
        .collection("physical_symptoms").document(controlledDate2027.toString())
        .get().await()
    }
    assertTrue("Document must exist in Firestore under 2027-10-20", docInDb.exists())
    assertEquals("MEDIUM", docInDb.getString("flowLevel"))
  }
}
