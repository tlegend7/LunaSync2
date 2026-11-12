package com.example

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.engine.CycleCalculationEngine
import com.example.data.model.*
import com.example.data.preferences.AppAppearanceMode
import com.example.data.repository.AuthRepository
import com.example.data.repository.FirestoreProvider
import com.example.ui.components.*
import com.example.ui.dialogs.*
import com.example.ui.screens.*
import com.example.ui.theme.LunaSyncTheme
import com.google.firebase.Timestamp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.*
import java.util.Date

/** Deterministic visual fixtures only. Repository/emulator tests verify real writes separately. */
class DesignStateCaptureTest {
  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
  private val date = LocalDate.of(2026, 6, 18)
  private val clock = Clock.fixed(date.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)
  private val history = listOf(
    CycleEntry(id = "current", startDate = "2026-06-01", endDate = "2026-06-05"),
    CycleEntry(id = "previous", startDate = "2026-05-04", endDate = "2026-05-08", cycleLength = 28))
  private val symptom = DailySymptom(date = date.toString(), flowLevel = "SPOTTING", mood = "CALM", painLevel = 1,
    privateNote = "Hoy prefiero ir con calma.")
  private val checkIn = CheckIn(id = "visual-message", promptType = CheckInPromptType.NEED_SPACE_OR_CUDDLES.name,
    customNote = "Aquí estoy para ti.")

  @Test fun captureTwentyStatesInBothAppearances() {
    val cases = listOf("welcome", "woman", "calendar", "symptoms", "period", "intimacy", "space", "privacy",
      "partner", "partnerCalendar", "checkin", "observation", "settings", "empty", "error", "join",
      "partnerSpace", "reply", "invite", "confirm")
    var screen by mutableStateOf("welcome")
    var dark by mutableStateOf(false)
    compose.activity.runOnUiThread { compose.activity.enableEdgeToEdge() }
    FirestoreProvider.ensureInitialized(compose.activity)
    compose.setContent {
      SideEffect {
        androidx.core.view.WindowCompat.getInsetsController(compose.activity.window, compose.activity.window.decorView).apply {
          isAppearanceLightStatusBars = !dark
          isAppearanceLightNavigationBars = !dark
        }
      }
      LunaSyncTheme(darkTheme = dark) {
        key(screen, dark) { Fixture(screen, dark) }
      }
    }
    for (night in listOf(false, true)) for (name in cases) {
      compose.runOnIdle { dark = night; screen = name }
      compose.waitForIdle()
      if (name == "error") {
        compose.onNodeWithTag("save_symptom_button").performClick()
        compose.onNodeWithTag("symptom_error").assertIsDisplayed()
      }
      compose.waitForIdle()
      val heading = when (name) {
        "welcome" -> "Tu ritmo.\nSu compañía."
        "woman" -> "Hoy, a tu ritmo."
        "calendar" -> "Tu calendario"
        "symptoms" -> "¿Cómo te sientes?"
        "period" -> "Tu menstruación"
        "intimacy" -> "Un registro íntimo"
        "space" -> "Nuestro espacio"
        "privacy" -> "Tú eliges"
        "partner" -> "Estar, sin adivinar."
        "partnerCalendar" -> "Calendario compartido"
        "checkin" -> "Una pregunta,\nsin presión."
        "observation" -> "Un gesto de cuidado"
        "settings" -> "A tu gusto"
        "empty" -> "Tu historia\nempieza contigo."
        "error" -> "Tu registro sigue\naquí."
        "join" -> "Una invitación\na acompañar."
        "partnerSpace" -> "Un vínculo con espacio"
        "reply" -> "Un mensaje de tu pareja"
        "invite" -> "Invitar, sin compartir\nde más."
        else -> "¿Desvincular\na tu pareja?"
      }
      compose.onNodeWithText(heading).assertIsDisplayed()
      // Wait for WindowManager's presented frame, not only Compose's layout tree.
      InstrumentationRegistry.getInstrumentation().waitForIdleSync()
      InstrumentationRegistry.getInstrumentation().uiAutomation.waitForIdle(150, 5000)
      android.os.SystemClock.sleep(1000)
      val output = File(compose.activity.getExternalFilesDir(null), if (compose.activity.resources.configuration.fontScale > 1.1f) "design-captures-large" else "design-captures").apply { mkdirs() }
      // Capture the compositor: PixelCopy of the Activity omits covering Dialog windows.
      val screenshot = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
      assertTrue("A composed screenshot is required for $name", screenshot != null)
      File(output, "${if (night) "dark" else "light"}-$name.png").outputStream().use {
        assertTrue(screenshot.compress(Bitmap.CompressFormat.PNG, 100, it))
      }
      screenshot.recycle()
    }
  }

  @Composable private fun Fixture(screen: String, dark: Boolean) {
    val context = LocalContext.current
    val auth = remember { AuthRepository(context) }
    val status = remember { CycleCalculationEngine.calculateCycleStatus(LocalDate.of(2026, 6, 1), pastCycles = history, today = date) }
    val predictions = remember { CycleCalculationEngine.predictFutureCycles(LocalDate.of(2026, 6, 1), pastCycles = history) }
    val permissions = SharingPermissions(shareSymptoms = true, shareMood = false, shareIntimacy = false)
    val space = CoupleSpace(id = "visual", ownerId = "owner", partnerId = "partner", status = "PAIRED", sharingPermissions = permissions)
    val role = if (screen in listOf("partner", "partnerCalendar", "partnerSpace")) UserRole.SUPPORT_PARTNER else UserRole.PRIMARY_ELLA
    val tab = if (screen.contains("Calendar", true)) 1 else if (screen.contains("space", true)) 2 else 0
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
      if (screen == "welcome") {
        AuthScreen(auth, UserRole.PRIMARY_ELLA, {}, {}, {})
      } else if (screen in listOf("woman", "calendar", "space", "partner", "partnerCalendar", "partnerSpace", "empty")) {
        Scaffold(contentWindowInsets = WindowInsets.safeDrawing, topBar = {
          Column(Modifier.statusBarsPadding().padding(horizontal = 24.dp)) {
            LunaContextTopBar("LunaSync / Tu espacio", if (role == UserRole.PRIMARY_ELLA) "AS" else "DM", {})
          }
        }, bottomBar = {
          Box(Modifier.navigationBarsPadding().padding(horizontal = 24.dp, vertical = 12.dp)) { LunaBottomNavPill(tab, {}) }
        }) { padding ->
          Box(Modifier.fillMaxSize().padding(padding)) {
            when (screen) {
              "woman", "empty" -> EllaDashboardView(if (screen == "empty") null else status, symptom, emptyList(), emptyList(),
                checkIn, null, {}, {}, {}, {}, {}, {}, {}, selectedDate = date)
              "partner" -> PartnerDashboardView(space, status, symptom.copy(privateNote = null), emptyList(), emptyList(),
                checkIn, emptyList(), onOpenCheckInDialog = {}, onOpenObservationDialog = {}, selectedDate = date)
              "calendar", "partnerCalendar" -> CycleCalendarView(LocalDate.of(2026, 6, 1), 5, 28, history,
                listOf(symptom), emptyList(), predictions, date, date, role == UserRole.PRIMARY_ELLA, {}, {},
                canReadMood = role == UserRole.PRIMARY_ELLA)
              else -> PairingScreen(role, null, "PAIRED", "partner", false, {}, {}, sharingPermissions = permissions)
            }
          }
        }
      } else when (screen) {
        "symptoms", "error" -> SymptomLogDialog(symptom, date, {},
          { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, done -> done(false, "No se pudo conectar. Revisa tu conexión e intenta nuevamente.") })
        "period" -> PeriodLogDialog(date, today = date, onDismiss = {}, onSave = { _, _, _ -> })
        "intimacy" -> IntimacyLogDialog(date, {}, { _, _, _, _, _, _, _ -> })
        "privacy" -> PrivacySettingsDialog(permissions, { _, _ -> }, {})
        "checkin" -> SendCheckInDialog({}, { _, _, _ -> })
        "observation" -> PartnerObservationDialog({}, { _, _, _, _, _, _ -> }, date)
        "settings" -> SettingsDialog(if (dark) AppAppearanceMode.DARK else AppAppearanceMode.LIGHT, {}, role, {}, {}, {}, "ana@example.test", {})
        "join" -> OnboardingJoinCodeScreen(false, null, {}, {})
        "reply" -> CheckInResponseDialog(checkIn, { _, _, _ -> }, {})
        "invite" -> InviteDetailsDialog(PairingInvite(inviteCode = "LUNA-8X4K", status = "ACTIVE", version = 1,
          expiresAt = Timestamp(Date.from(clock.instant().plusSeconds(172800)))), 1, false, false, null, false, clock, {}, {}, {})
        "confirm" -> UnlinkPairingDialog(UserRole.PRIMARY_ELLA, {}, {})
      }
    }
  }
}
