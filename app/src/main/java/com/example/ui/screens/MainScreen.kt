package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CheckIn
import com.example.data.model.SpaceStatus
import com.example.data.model.UserRole
import com.example.ui.components.LunaBottomNavPill
import com.example.ui.components.LunaButton
import com.example.ui.components.LunaButtonType
import com.example.ui.components.LunaContextTopBar
import com.example.data.preferences.AppAppearanceMode
import com.example.ui.dialogs.CheckInResponseDialog
import com.example.ui.dialogs.IntimacyLogDialog
import com.example.ui.dialogs.PartnerObservationDialog
import com.example.ui.dialogs.PeriodLogDialog
import com.example.ui.dialogs.PrivacySettingsDialog
import com.example.ui.dialogs.SendCheckInDialog
import com.example.ui.dialogs.SettingsDialog
import com.example.ui.dialogs.SymptomLogDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import java.time.LocalDate
import kotlinx.coroutines.launch

/**
 * Pantalla principal anfitriona de LunaSync (orquesta navegación Figma: Hoy, Calendario, Nosotros)
 */
@Composable
fun MainScreen(
  viewModel: MainViewModel,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsState()
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  // 0: Hoy, 1: Calendario, 2: Nosotros
  var currentNavTab by remember { mutableIntStateOf(0) }
  var showJoinCodeFormOnboarding by remember { mutableStateOf(false) }

  // Control de tema (Sistema / Claro / Oscuro)
  val appearanceMode by viewModel.appearanceMode.collectAsState()
  val systemDark = isSystemInDarkTheme()
  val effectiveDark = appearanceMode.isDark(systemDark)

  // Dialog target dates (inmutables durante el ciclo de vida del diálogo)
  var periodDialogTargetDate by remember { mutableStateOf<LocalDate?>(null) }
  var symptomDialogTargetDate by remember { mutableStateOf<LocalDate?>(null) }
  var intimacyDialogTargetDate by remember { mutableStateOf<LocalDate?>(null) }
  var showObservationDialog by remember { mutableStateOf(false) }
  var showCheckInDialog by remember { mutableStateOf(false) }
  var showPrivacyDialog by remember { mutableStateOf(false) }
  var showSettingsDialog by remember { mutableStateOf(false) }
  var checkInToAnswer by remember { mutableStateOf<CheckIn?>(null) }

  // Descartar diálogos ante cambios o cierre de sesión
  LaunchedEffect(uiState.currentUser?.uid, uiState.isGuestMode, uiState.space?.id) {
    periodDialogTargetDate = null
    symptomDialogTargetDate = null
    intimacyDialogTargetDate = null
    showObservationDialog = false
    showCheckInDialog = false
    showPrivacyDialog = false
    showSettingsDialog = false
    checkInToAnswer = null
  }

  LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
    uiState.errorMessage?.let {
      snackbarHostState.showSnackbar(it)
      viewModel.clearMessages()
    }
    uiState.successMessage?.let {
      snackbarHostState.showSnackbar(it)
      viewModel.clearMessages()
    }
  }

  LunaSyncTheme(darkTheme = effectiveDark) {
    Scaffold(
      snackbarHost = { SnackbarHost(snackbarHostState) },
      topBar = {
        Surface(
          color = MaterialTheme.colorScheme.background,
          modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 24.dp, vertical = 4.dp)
          ) {
            val contextLabel = if (uiState.currentRole == UserRole.PRIMARY_ELLA) {
              "LunaSync / Tu espacio"
            } else {
              "LunaSync / En pareja"
            }
            val initials = if (uiState.isGuestMode) {
              if (uiState.currentRole == UserRole.PRIMARY_ELLA) "AS" else "DM"
            } else uiState.currentUser?.displayName?.trim()?.takeIf { it.isNotEmpty() }
              ?.split(Regex("\\s+"))?.take(2)?.joinToString("") { it.take(1) }?.uppercase()
              ?: uiState.currentUser?.email?.take(1)?.uppercase() ?: "LS"

            LunaContextTopBar(
              contextText = contextLabel,
              avatarInitials = initials,
              onAvatarClick = { showSettingsDialog = true }
            )

            // Selector rápido para modo demo
            if (uiState.isGuestMode) {
              DemoModeBanner(
                currentRole = uiState.currentRole,
                onSwitchRole = { role -> snackbarHostState.currentSnackbarData?.dismiss(); viewModel.switchRole(role) },
                onExitDemo = { viewModel.exitGuestMode() }
              )
            }
          }
        }
      },
      bottomBar = {
        if (uiState.space != null || uiState.isGuestMode) {
          Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier
              .fillMaxWidth()
              .navigationBarsPadding()
              .padding(horizontal = 24.dp, vertical = 12.dp)
          ) {
            LunaBottomNavPill(
              selectedTab = currentNavTab,
              onTabSelected = { tab ->
                if (tab != currentNavTab) snackbarHostState.currentSnackbarData?.dismiss()
                currentNavTab = tab
              }
            )
          }
        }
      },
      modifier = modifier.fillMaxSize()
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
      ) {
        if (uiState.isResolvingSpace) {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
              Spacer(modifier = Modifier.height(14.dp))
              Text(
                "Conectando con tu espacio...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        } else if (uiState.space == null && !uiState.isGuestMode) {
          // Onboarding para cuenta autenticada sin espacio aún
          if (showJoinCodeFormOnboarding) {
            OnboardingJoinCodeScreen(
              isLoading = uiState.isLoading,
              errorMessage = uiState.errorMessage,
              onBack = { showJoinCodeFormOnboarding = false },
              onSubmitCode = { code -> viewModel.joinWithCode(code) }
            )
          } else {
            UnpairedAccountOnboardingView(
              isLoading = uiState.isLoading,
              onCreateSpace = { viewModel.createOwnerSpace() },
              onGoToPairing = { showJoinCodeFormOnboarding = true }
            )
          }
        } else {
          when (currentNavTab) {
            0 -> {
              if (uiState.currentRole == UserRole.PRIMARY_ELLA) {
                EllaDashboardView(
                  cycleStatus = uiState.cycleStatus,
                  todaySymptom = uiState.todaySymptom,
                  intimacyLogs = uiState.intimacyLogs,
                  partnerObservations = uiState.partnerObservations,
                  latestCheckIn = uiState.latestCheckIn,
                  activeSpottingSuggestion = uiState.activeSpottingSuggestion,
                  selectedDate = viewModel.getToday(),
                  onSelectDate = { date -> viewModel.selectDate(date); currentNavTab = 1 },
                  onOpenPeriodDialog = { periodDialogTargetDate = viewModel.getToday() },
                  onOpenSymptomDialog = { symptomDialogTargetDate = viewModel.getToday() },
                  onOpenIntimacyDialog = { intimacyDialogTargetDate = viewModel.getToday() },
                  onNavigateToCalendar = { currentNavTab = 1 },
                  onQuickReplySelected = { reply -> viewModel.answerCheckIn(reply) },
                  onOpenReply = { checkIn -> checkInToAnswer = checkIn },
                  onOpenPrivacy = { showPrivacyDialog = true },
                  onAcceptSpotting = { viewModel.acceptSpottingSuggestion() },
                  onDismissSpotting = { viewModel.dismissSpottingSuggestion() }
                )
              } else {
                PartnerDashboardView(
                  space = uiState.space,
                  cycleStatus = uiState.cycleStatus,
                  todaySymptom = uiState.todaySymptom,
                  intimacyLogs = uiState.intimacyLogs,
                  partnerCareTips = uiState.partnerCareTips,
                  latestCheckIn = uiState.latestCheckIn,
                  partnerObservations = uiState.partnerObservations,
                  isLoading = uiState.isLoading,
                  errorMessage = uiState.errorMessage,
                  selectedDate = viewModel.getToday(),
                  onOpenCheckInDialog = { showCheckInDialog = true },
                  onOpenObservationDialog = { showObservationDialog = true }
                )
              }
            }
            1 -> {
              val lastPeriodStart = remember(uiState.pastCycles) {
                uiState.pastCycles.mapNotNull {
                  try { LocalDate.parse(it.startDate) } catch (e: Exception) { null }
                }.maxOrNull()
              }
              val symptomsList = remember(uiState.allSymptoms, uiState.todaySymptom) {
                if (uiState.allSymptoms.isNotEmpty()) uiState.allSymptoms else listOfNotNull(uiState.todaySymptom)
              }
              CycleCalendarView(
                lastPeriodStart = lastPeriodStart,
                averagePeriodLength = uiState.space?.averagePeriodLength ?: 5,
                averageCycleLength = uiState.space?.averageCycleLength ?: 28,
                pastCycles = uiState.pastCycles,
                symptoms = symptomsList,
                intimacyLogs = uiState.intimacyLogs,
                predictedCycles = uiState.predictedCycles,
                selectedDate = uiState.selectedDate,
                today = viewModel.getToday(),
                canLogSymptoms = uiState.currentRole == UserRole.PRIMARY_ELLA,
                canReadSymptoms = uiState.currentRole == UserRole.PRIMARY_ELLA || uiState.space?.sharingPermissions?.shareSymptoms == true,
                canReadMood = uiState.currentRole == UserRole.PRIMARY_ELLA || uiState.space?.sharingPermissions?.shareMood == true,
                canReadIntimacy = uiState.currentRole == UserRole.PRIMARY_ELLA || uiState.space?.sharingPermissions?.shareIntimacy == true,
                onDateSelected = { date -> viewModel.selectDate(date) },
                onOpenSymptomDialogForDate = { date ->
                  if (uiState.currentRole == UserRole.PRIMARY_ELLA) {
                    symptomDialogTargetDate = date
                  }
                },
                onOpenPeriodDialogForDate = { date ->
                  if (uiState.currentRole == UserRole.PRIMARY_ELLA) {
                    periodDialogTargetDate = date
                  }
                }
              )
            }
            2 -> {
              PairingScreen(
                currentRole = uiState.currentRole,
                activeInvite = uiState.activePairingInvite,
                activeInviteVersion = uiState.space?.activeInviteVersion ?: 1L,
                inviteError = uiState.pairingCodeError,
                isGuestMode = uiState.isGuestMode,
                clock = viewModel.clock,
                sharingPermissions = uiState.space?.sharingPermissions ?: com.example.data.model.SharingPermissions(shareSymptoms = false, shareMood = false),
                onOpenSharedRecords = { currentNavTab = 1 },
                spaceStatus = uiState.space?.status,
                partnerId = uiState.space?.partnerId,
                isLoading = uiState.isLoading,
                onGenerateCode = { viewModel.generatePairingCode() },
                onJoinCode = { code -> viewModel.joinWithCode(code) },
                onRevokePairing = { onComplete -> viewModel.revokePairing(onComplete) },
                onOpenPrivacySettings = { showPrivacyDialog = true }
              )
            }
          }
        }
      }
    }

    // ---------------- DIÁLOGOS DE FLUJO ----------------

    // 1. Registro de Periodo / Sangrado (period · Tu menstruación)
    periodDialogTargetDate?.let { targetDate ->
      val targetDateStr = targetDate.toString()
      val existingCycleForDate = remember(targetDate, uiState.pastCycles) {
        uiState.pastCycles.firstOrNull { it.startDate == targetDateStr }
          ?: uiState.pastCycles.firstOrNull { cycle ->
            val start = try { LocalDate.parse(cycle.startDate) } catch (e: Exception) { null }
            val end = cycle.endDate?.let { try { LocalDate.parse(it) } catch (e: Exception) { null } }
            if (start != null && end != null) {
              !targetDate.isBefore(start) && !targetDate.isAfter(end)
            } else false
          }
      }

      PeriodLogDialog(
        initialDate = targetDate,
        existingCycle = existingCycleForDate,
        today = viewModel.getToday(),
        onDismiss = { periodDialogTargetDate = null },
        onSave = { startDate, endDate, onComplete ->
          if (existingCycleForDate != null) {
            viewModel.updateCycleDates(existingCycleForDate.id, startDate, endDate, onComplete)
          } else {
            viewModel.recordPeriodStart(startDate, endDate, onComplete = onComplete)
          }
        },
        onDelete = if (existingCycleForDate != null) {
          { onComplete ->
            viewModel.deleteCycle(existingCycleForDate.id, onComplete)
          }
        } else null
      )
    }

    // 2. Registro de Síntomas y Ánimo (symptoms · ¿Cómo te sientes?)
    symptomDialogTargetDate?.let { targetDate ->
      val symptomForTargetDate = remember(targetDate, uiState.allSymptoms, uiState.todaySymptom) {
        val dateStr = targetDate.toString()
        uiState.allSymptoms.firstOrNull { it.date == dateStr }
          ?: if (dateStr == viewModel.getToday().toString()) uiState.todaySymptom else null
      }

      SymptomLogDialog(
        initialSymptom = symptomForTargetDate,
        date = targetDate,
        onDismiss = { symptomDialogTargetDate = null },
        onSave = { flow, pain, mood, energy, cramps, headache, backPain, bloating, mucus, breasts, cravings, acne, pubNote, privNote, isPriv, isMoodPriv, onComplete ->
          viewModel.saveSymptom(
            date = targetDate,
            flowLevel = flow,
            painLevel = pain,
            mood = mood,
            energyLevel = energy,
            cramps = cramps,
            headache = headache,
            backPain = backPain,
            bloating = bloating,
            cervicalMucus = mucus,
            tenderBreasts = breasts,
            cravings = cravings,
            acne = acne,
            publicNote = pubNote,
            privateNote = privNote,
            isPrivate = isPriv,
            isMoodPrivate = isMoodPriv,
            onComplete = onComplete
          )
        }
      )
    }

    // 3. Registro de Intimidad (intimacy · Un registro íntimo)
    intimacyDialogTargetDate?.let { targetDate ->
      IntimacyLogDialog(
        date = targetDate,
        onDismiss = { intimacyDialogTargetDate = null },
        onSave = { type, lube, orgasm, effect, notes, isPriv, onComplete ->
          viewModel.logIntimacy(
            date = targetDate,
            encounterType = type,
            usedLubricant = lube,
            hadOrgasm = orgasm,
            postContactEffect = effect,
            notes = notes,
            isPrivate = isPriv,
            onComplete = onComplete
          )
        }
      )
    }

    // 4. Observación de Pareja (observation · Un gesto de cuidado)
    if (showObservationDialog) {
      PartnerObservationDialog(
        date = viewModel.getToday(),
        onDismiss = { showObservationDialog = false },
        onSave = { perceivedMood, pain, spotting, action, msg, onComplete ->
          viewModel.logPartnerObservation(
            perceivedMood = perceivedMood,
            apparentPainLevel = pain,
            unloggedSpottingReported = spotting,
            careActionTaken = action,
            sweetMessage = msg,
            onComplete = onComplete
          )
        }
      )
    }

    // 5. Enviar Check-In (checkin · Una pregunta, sin presión)
    if (showCheckInDialog) {
      SendCheckInDialog(
        onDismiss = { showCheckInDialog = false },
        onSend = { prompt, note, onComplete ->
          viewModel.sendCheckIn(prompt, note, onComplete = onComplete)
        }
      )
    }

    // 6. Privacidad y Permisos (privacy · Tú eliges)
    if (showPrivacyDialog) {
      val permissions = uiState.space?.sharingPermissions ?: com.example.data.model.SharingPermissions()
      PrivacySettingsDialog(
        currentPermissions = permissions,
        canEdit = uiState.currentRole == UserRole.PRIMARY_ELLA && uiState.space != null &&
          (uiState.isGuestMode || uiState.currentUser?.uid == uiState.space?.ownerId),
        onSavePermissions = { updatedPermissions, onComplete ->
          scope.launch {
            viewModel.updateSharingPermissions(updatedPermissions).fold(
              onSuccess = { onComplete(true, null) },
              onFailure = { error -> onComplete(false, viewModel.sanitizeUserErrorMessage(error,
                "No se pudieron guardar los permisos. Intenta nuevamente.")) }
            )
          }
        },
        onDismiss = { showPrivacyDialog = false }
      )
    }

    // 7. Ajustes y Apariencia (settings · A tu gusto)
    if (showSettingsDialog) {
      SettingsDialog(
        currentAppearance = appearanceMode,
        isGuestMode = uiState.isGuestMode,
        onAppearanceSelected = viewModel::selectAppearance,
        currentRole = uiState.currentRole,
        onSwitchRole = {
          val newRole = if (uiState.currentRole == UserRole.PRIMARY_ELLA) {
            UserRole.SUPPORT_PARTNER
          } else {
            UserRole.PRIMARY_ELLA
          }
          viewModel.switchRole(newRole)
        },
        onOpenPrivacy = { showPrivacyDialog = true },
        onSignOut = {
          if (uiState.isGuestMode) {
            viewModel.exitGuestMode()
          } else {
            viewModel.signOut(scope)
          }
        },
        userEmail = uiState.currentUser?.email,
        onDismiss = { showSettingsDialog = false }
      )
    }

    // 8. Responder Check-In (reply · Un mensaje de Dani)
    checkInToAnswer?.let { checkIn ->
      CheckInResponseDialog(
        checkIn = checkIn,
        onSendResponse = { option, note, onComplete ->
          viewModel.answerCheckIn(option, note, checkInId = checkIn.id, onComplete = onComplete)
        },
        onDismiss = { checkInToAnswer = null }
      )
    }
  }
}

@Composable
fun OnboardingJoinCodeScreen(
  isLoading: Boolean,
  errorMessage: String?,
  onBack: () -> Unit,
  onSubmitCode: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var inputCode by remember { mutableStateOf("") }
  com.example.ui.components.LunaFlowDialog("Una invitación\na acompañar.",
    "Vincularse no significa compartir todo.", onBack, "onboarding_join_code_screen", busy = isLoading,
    actions = {
      errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("onboarding_join_error")) }
      LunaButton("Vincular mi cuenta", { onSubmitCode(inputCode.trim()) },
        modifier = Modifier.testTag("onboarding_submit_code_button"), enabled = inputCode.isNotBlank(),
        loading = isLoading, loadingText = "Vinculando…")
    }) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer) {
      Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(com.example.ui.components.LunaIcons.Link, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
        Text("Pídele su código a tu pareja", style = MaterialTheme.typography.titleMedium)
        Text("Ella genera la invitación y elige qué compartir. Los permisos se pueden cambiar después.",
          style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
    OutlinedTextField(inputCode, { inputCode = it.uppercase(java.util.Locale.ROOT) },
      enabled = !isLoading, label = { Text("Código de invitación") }, singleLine = true,
      shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().testTag("onboarding_code_input"))
    Text("Una vez vinculados", style = MaterialTheme.typography.titleLarge)
    Text("Podrás consultar solo lo que tu pareja autorice. Sus notas privadas nunca se comparten.",
      style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    LunaButton("Prefiero empezar sin vincular", onBack, modifier = Modifier.testTag("onboarding_back_button"),
      type = LunaButtonType.SECONDARY, enabled = !isLoading)
  }
}

@Composable
private fun UnpairedAccountOnboardingView(
  isLoading: Boolean,
  onCreateSpace: () -> Unit,
  onGoToPairing: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = modifier.fillMaxSize().padding(28.dp)
  ) {
    Text(
      text = "¡Te damos la bienvenida a LunaSync!",
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(10.dp))
    Text(
      text = "¿Cómo deseas comenzar?",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(30.dp))

    LunaButton(
      text = "Tengo un código de mi pareja",
      onClick = onGoToPairing,
      type = LunaButtonType.PRIMARY,
      modifier = Modifier.testTag("onboarding_join_partner_button")
    )

    Spacer(modifier = Modifier.height(14.dp))

    LunaButton(
      text = "Crear mi propio espacio de ciclo",
      onClick = onCreateSpace,
      enabled = !isLoading,
      loading = isLoading,
      type = LunaButtonType.SECONDARY,
      modifier = Modifier.testTag("onboarding_create_space_button")
    )
  }
}

@Composable
private fun DemoModeBanner(
  currentRole: UserRole,
  onSwitchRole: (UserRole) -> Unit,
  onExitDemo: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer,
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)) {
    Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
      Text(if (currentRole == UserRole.PRIMARY_ELLA) "Demo · Ella" else "Demo · Pareja",
        style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
      androidx.compose.material3.TextButton({
        onSwitchRole(if (currentRole == UserRole.PRIMARY_ELLA) UserRole.SUPPORT_PARTNER else UserRole.PRIMARY_ELLA)
      }, modifier = Modifier.testTag(if (currentRole == UserRole.PRIMARY_ELLA) "switch_to_partner" else "switch_to_ella")) {
        Text(if (currentRole == UserRole.PRIMARY_ELLA) "Ver Pareja" else "Ver Ella", style = MaterialTheme.typography.labelSmall)
      }
      androidx.compose.material3.TextButton(onExitDemo, modifier = Modifier.testTag("exit_demo_button")) {
        Text("Salir", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
      }
    }
  }
}
