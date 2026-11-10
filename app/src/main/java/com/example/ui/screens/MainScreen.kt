package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SpaceStatus
import com.example.data.model.UserRole
import com.example.ui.dialogs.IntimacyLogDialog
import com.example.ui.dialogs.PartnerObservationDialog
import com.example.ui.dialogs.PeriodLogDialog
import com.example.ui.dialogs.SendCheckInDialog
import com.example.ui.dialogs.SymptomLogDialog
import com.example.ui.theme.AppleHairlineBorder
import com.example.ui.theme.AppleHealthFertile
import com.example.ui.theme.AppleHealthFollicularSubtle
import com.example.ui.theme.AppleSystemBlue
import com.example.ui.theme.AppleSystemGray5
import com.example.ui.theme.AppleSystemIndigo
import com.example.ui.theme.BrandTerracottaRose
import com.example.ui.theme.BrandWarmRose
import com.example.ui.theme.StatusSuccess
import com.example.ui.viewmodel.MainViewModel
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
  viewModel: MainViewModel,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsState()
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  var currentNavTab by remember { mutableIntStateOf(0) }
  var showJoinCodeFormOnboarding by remember { mutableStateOf(false) }

  // Dialog target dates (capturan la fecha explícita de apertura y no mutan con selecciones externas)
  var periodDialogTargetDate by remember { mutableStateOf<LocalDate?>(null) }
  var symptomDialogTargetDate by remember { mutableStateOf<LocalDate?>(null) }
  var intimacyDialogTargetDate by remember { mutableStateOf<LocalDate?>(null) }
  var showObservationDialog by remember { mutableStateOf(false) }
  var showCheckInDialog by remember { mutableStateOf(false) }
  var showPrivacyDialog by remember { mutableStateOf(false) }

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

  Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      AppleTopBar(
        currentRole = uiState.currentRole,
        isPaired = uiState.space?.status == SpaceStatus.PAIRED.name,
        isGuestMode = uiState.isGuestMode,
        hasSpace = uiState.space != null,
        onSwitchRole = { newRole -> viewModel.switchRole(newRole) },
        onOpenPrivacy = { showPrivacyDialog = true },
        onSignOut = {
          if (uiState.isGuestMode) {
            viewModel.exitGuestMode()
          } else {
            viewModel.signOut(scope)
          }
        }
      )
    },
    bottomBar = {
      if (uiState.space != null || uiState.isGuestMode) {
        // iOS Style Tab Bar (only when in active space or demo)
        NavigationBar(
          containerColor = MaterialTheme.colorScheme.surface,
          tonalElevation = 0.dp,
          modifier = Modifier
            .navigationBarsPadding()
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
          NavigationBarItem(
            selected = currentNavTab == 0,
            onClick = { currentNavTab = 0 },
            icon = { Icon(Icons.Default.Spa, contentDescription = "Hoy", modifier = Modifier.size(22.dp)) },
            label = { Text("Hoy", fontWeight = if (currentNavTab == 0) FontWeight.SemiBold else FontWeight.Normal) },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = BrandTerracottaRose,
              selectedTextColor = BrandTerracottaRose,
              unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
              unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
              indicatorColor = Color.Transparent
            ),
            modifier = Modifier.testTag("nav_tab_dashboard")
          )
          NavigationBarItem(
            selected = currentNavTab == 1,
            onClick = { currentNavTab = 1 },
            icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Calendario", modifier = Modifier.size(22.dp)) },
            label = { Text("Calendario", fontWeight = if (currentNavTab == 1) FontWeight.SemiBold else FontWeight.Normal) },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = BrandTerracottaRose,
              selectedTextColor = BrandTerracottaRose,
              unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
              unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
              indicatorColor = Color.Transparent
            ),
            modifier = Modifier.testTag("nav_tab_calendar")
          )
          NavigationBarItem(
            selected = currentNavTab == 2,
            onClick = { currentNavTab = 2 },
            icon = { Icon(Icons.Default.Favorite, contentDescription = "Pareja", modifier = Modifier.size(22.dp)) },
            label = { Text("Pareja", fontWeight = if (currentNavTab == 2) FontWeight.SemiBold else FontWeight.Normal) },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = BrandTerracottaRose,
              selectedTextColor = BrandTerracottaRose,
              unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
              unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
              indicatorColor = Color.Transparent
            ),
            modifier = Modifier.testTag("nav_tab_pairing")
          )
        }
      }
    },
    modifier = modifier.fillMaxSize()
  ) { innerPadding ->
    Column(modifier = Modifier.padding(innerPadding)) {
      if (uiState.isGuestMode) {
        DemoModeBanner(onExitDemo = { viewModel.exitGuestMode() })
      }

      Box(modifier = Modifier.weight(1f)) {
        if (uiState.isResolvingSpace) {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              CircularProgressIndicator(color = AppleSystemBlue)
              Spacer(modifier = Modifier.height(14.dp))
              Text("Conectando con tu espacio...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        } else if (uiState.space == null && !uiState.isGuestMode) {
          // Unpaired authenticated account onboarding
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
                  onOpenPeriodDialog = { periodDialogTargetDate = viewModel.getToday() },
                  onOpenSymptomDialog = { symptomDialogTargetDate = viewModel.getToday() },
                  onOpenIntimacyDialog = { intimacyDialogTargetDate = viewModel.getToday() },
                  onNavigateToCalendar = { currentNavTab = 1 },
                  onQuickReplySelected = { reply -> viewModel.answerCheckIn(reply) },
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
                  onOpenCheckInDialog = { showCheckInDialog = true },
                  onOpenObservationDialog = { showObservationDialog = true }
                )
              }
            }
            1 -> {
              val lastPeriodStart = remember(uiState.pastCycles) {
                uiState.pastCycles.mapNotNull {
                  try { java.time.LocalDate.parse(it.startDate) } catch (e: Exception) { null }
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
                activeCode = uiState.activePairingCode,
                spaceStatus = uiState.space?.status,
                partnerId = uiState.space?.partnerId,
                isLoading = uiState.isLoading,
                onGenerateCode = { viewModel.generatePairingCode() },
                onJoinCode = { code -> viewModel.joinWithCode(code) },
                onRevokePairing = { viewModel.revokePairing() }
              )
            }
          }
        }
      }
    }
  }

  // Dialogs
  // Dialogs
  periodDialogTargetDate?.let { targetDate ->
    val targetDateStr = targetDate.toString()
    val existingCycleForDate = remember(targetDate, uiState.pastCycles) {
      uiState.pastCycles.firstOrNull { it.startDate == targetDateStr }
        ?: uiState.pastCycles.firstOrNull { cycle ->
          val start = try { java.time.LocalDate.parse(cycle.startDate) } catch (e: Exception) { null }
          val end = cycle.endDate?.let { try { java.time.LocalDate.parse(it) } catch (e: Exception) { null } }
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
      onSave = { flow, pain, mood, energy, cramps, headache, backPain, bloating, mucus, breasts, cravings, acne, pubNote, privNote, isPriv, onComplete ->
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
          onComplete = onComplete
        )
      }
    )
  }

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

  if (showObservationDialog) {
    PartnerObservationDialog(
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

  if (showCheckInDialog) {
    SendCheckInDialog(
      onDismiss = { showCheckInDialog = false },
      onSend = { prompt, note, onComplete ->
        viewModel.sendCheckIn(prompt, note, onComplete = onComplete)
      }
    )
  }

  if (showPrivacyDialog) {
    PrivacySettingsDialog(
      currentRole = uiState.currentRole,
      space = uiState.space,
      onDismiss = { showPrivacyDialog = false },
      onSavePermissions = { permissions, onComplete ->
        scope.launch {
          val res = viewModel.updateSharingPermissions(permissions)
          onComplete(res.isSuccess)
        }
      }
    )
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

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(24.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth()
    ) {
      IconButton(onClick = onBack, modifier = Modifier.testTag("onboarding_back_button")) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
      }
      Spacer(modifier = Modifier.width(8.dp))
      Text("Vincular con tu pareja", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }

    Spacer(modifier = Modifier.height(24.dp))

    Text(
      text = "Ingresa el código que te compartió tu novia (ej. LUNA-8X4K):",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
      value = inputCode,
      onValueChange = { inputCode = it.uppercase() },
      label = { Text("Código de invitación") },
      placeholder = { Text("LUNA-XXXX") },
      singleLine = true,
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier.fillMaxWidth().testTag("onboarding_code_input")
    )

    if (errorMessage != null) {
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = errorMessage,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall
      )
    }

    Spacer(modifier = Modifier.height(24.dp))

    Button(
      onClick = { onSubmitCode(inputCode) },
      enabled = inputCode.isNotBlank() && !isLoading,
      shape = RoundedCornerShape(14.dp),
      colors = ButtonDefaults.buttonColors(containerColor = AppleSystemBlue),
      modifier = Modifier.fillMaxWidth().height(52.dp).testTag("onboarding_submit_code_button")
    ) {
      if (isLoading) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
      } else {
        Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Conectarme al espacio", fontWeight = FontWeight.Bold)
      }
    }
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

    Button(
      onClick = onGoToPairing,
      shape = RoundedCornerShape(16.dp),
      colors = ButtonDefaults.buttonColors(containerColor = AppleSystemBlue),
      modifier = Modifier.fillMaxWidth().height(52.dp).testTag("onboarding_join_partner_button")
    ) {
      Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text("Tengo un código de mi pareja", fontWeight = FontWeight.Bold)
    }

    Spacer(modifier = Modifier.height(14.dp))

    OutlinedButton(
      onClick = onCreateSpace,
      enabled = !isLoading,
      shape = RoundedCornerShape(16.dp),
      border = androidx.compose.foundation.BorderStroke(1.dp, AppleHairlineBorder),
      modifier = Modifier.fillMaxWidth().height(52.dp).testTag("onboarding_create_space_button")
    ) {
      if (isLoading) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
      } else {
        Icon(Icons.Default.Spa, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Crear mi propio espacio de ciclo", fontWeight = FontWeight.SemiBold)
      }
    }
  }
}

@Composable
private fun AppleTopBar(
  currentRole: UserRole,
  isPaired: Boolean,
  isGuestMode: Boolean,
  hasSpace: Boolean,
  onSwitchRole: (UserRole) -> Unit,
  onOpenPrivacy: () -> Unit,
  onSignOut: () -> Unit
) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    modifier = Modifier
      .fillMaxWidth()
      .statusBarsPadding()
      .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(32.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(BrandTerracottaRose.copy(alpha = 0.12f))
      ) {
        Icon(
          imageVector = Icons.Default.NightsStay,
          contentDescription = null,
          tint = BrandTerracottaRose,
          modifier = Modifier.size(18.dp)
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      Text(
        text = "LunaSync",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )

      Spacer(modifier = Modifier.weight(1f))

      if (isGuestMode) {
        AppleSegmentedControl(
          currentRole = currentRole,
          onRoleSelected = onSwitchRole
        )
      } else if (hasSpace) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = AppleSystemGray5.copy(alpha = 0.65f),
          modifier = Modifier.padding(vertical = 2.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Text(
              text = if (currentRole == UserRole.PRIMARY_ELLA) "Ella 🌙" else "Novio 🤝",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(6.dp))

      if (hasSpace || isGuestMode) {
        IconButton(
          onClick = onOpenPrivacy,
          modifier = Modifier.size(36.dp).testTag("privacy_settings_button")
        ) {
          Icon(
            Icons.Default.Security,
            contentDescription = "Privacidad",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
          )
        }
      }

      IconButton(
        onClick = onSignOut,
        modifier = Modifier.size(36.dp).testTag("sign_out_button")
      ) {
        Icon(
          Icons.AutoMirrored.Filled.Logout,
          contentDescription = "Salir",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(18.dp)
        )
      }
    }
  }
}

@Composable
private fun AppleSegmentedControl(
  currentRole: UserRole,
  onRoleSelected: (UserRole) -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = AppleSystemGray5.copy(alpha = 0.65f),
    modifier = modifier.padding(vertical = 2.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(2.dp)
    ) {
      val isElla = currentRole == UserRole.PRIMARY_ELLA

      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .background(if (isElla) MaterialTheme.colorScheme.surface else Color.Transparent)
          .then(
            if (isElla) Modifier.shadow(1.dp, RoundedCornerShape(8.dp), clip = false) else Modifier
          )
          .clickable { onRoleSelected(UserRole.PRIMARY_ELLA) }
          .padding(horizontal = 10.dp, vertical = 6.dp)
          .testTag("switch_to_ella")
      ) {
        Text(
          text = "Ella 🌙",
          fontSize = 12.sp,
          fontWeight = if (isElla) FontWeight.Bold else FontWeight.Medium,
          color = if (isElla) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .background(if (!isElla) MaterialTheme.colorScheme.surface else Color.Transparent)
          .then(
            if (!isElla) Modifier.shadow(1.dp, RoundedCornerShape(8.dp), clip = false) else Modifier
          )
          .clickable { onRoleSelected(UserRole.SUPPORT_PARTNER) }
          .padding(horizontal = 10.dp, vertical = 6.dp)
          .testTag("switch_to_partner")
      ) {
        Text(
          text = "Novio 🤝",
          fontSize = 12.sp,
          fontWeight = if (!isElla) FontWeight.Bold else FontWeight.Medium,
          color = if (!isElla) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@Composable
fun PrivacySettingsDialog(
  currentRole: UserRole,
  space: com.example.data.model.CoupleSpace?,
  onDismiss: () -> Unit,
  onSavePermissions: (com.example.data.model.SharingPermissions, (Boolean) -> Unit) -> Unit
) {
  val isElla = currentRole == UserRole.PRIMARY_ELLA
  var shareSymptoms by remember { mutableStateOf(space?.sharingPermissions?.shareSymptoms ?: true) }
  var shareMood by remember { mutableStateOf(space?.sharingPermissions?.shareMood ?: true) }
  var shareIntimacy by remember { mutableStateOf(space?.sharingPermissions?.shareIntimacy ?: false) }
  var sharePartnerNotes by remember { mutableStateOf(space?.sharingPermissions?.sharePartnerNotes ?: true) }
  var isSaving by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = { if (!isSaving) onDismiss() },
    shape = RoundedCornerShape(24.dp),
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Security, contentDescription = null, tint = BrandTerracottaRose)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = if (isElla) "Permisos de Privacidad" else "Privacidad de la Pareja",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold
        )
      }
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
          text = if (isElla) {
            "Tú decides exactamente qué información ve tu novio en su dashboard:"
          } else {
            "Permisos configurados por tu pareja para este espacio compartido:"
          },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Compartir Síntomas Físicos", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            Text("Flujo, dolor, cólicos", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          if (isElla) {
            Switch(
              checked = shareSymptoms,
              onCheckedChange = { shareSymptoms = it },
              enabled = !isSaving,
              colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = StatusSuccess)
            )
          } else {
            Text(
              text = if (shareSymptoms) "Activo" else "Desactivado",
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              color = if (shareSymptoms) StatusSuccess else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Compartir Estado de Ánimo", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            Text("Humor y niveles de energía", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          if (isElla) {
            Switch(
              checked = shareMood,
              onCheckedChange = { shareMood = it },
              enabled = !isSaving,
              colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = StatusSuccess)
            )
          } else {
            Text(
              text = if (shareMood) "Activo" else "Desactivado",
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              color = if (shareMood) StatusSuccess else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Compartir Encuentros Íntimos", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            Text("Solo los no marcados como privados", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          if (isElla) {
            Switch(
              checked = shareIntimacy,
              onCheckedChange = { shareIntimacy = it },
              enabled = !isSaving,
              colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = StatusSuccess)
            )
          } else {
            Text(
              text = if (shareIntimacy) "Activo" else "Desactivado",
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              color = if (shareIntimacy) StatusSuccess else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = "Nota: La revocación se aplica inmediatamente en el servidor. Los dispositivos sin conexión actualizarán el estado tan pronto recuperen conectividad.",
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          lineHeight = 15.sp
        )
      }
    },
    confirmButton = {
      if (isElla) {
        Button(
          onClick = {
            isSaving = true
            onSavePermissions(
              com.example.data.model.SharingPermissions(
                shareSymptoms = shareSymptoms,
                shareMood = shareMood,
                shareIntimacy = shareIntimacy,
                sharePartnerNotes = sharePartnerNotes
              )
            ) { success ->
              isSaving = false
              if (success) {
                onDismiss()
              }
            }
          },
          enabled = !isSaving,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = BrandTerracottaRose)
        ) {
          if (isSaving) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
          } else {
            Text("Guardar Permisos", fontWeight = FontWeight.SemiBold)
          }
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, enabled = !isSaving) {
        Text("Cerrar")
      }
    }
  )
}

@Composable
private fun DemoModeBanner(
  onExitDemo: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    color = BrandTerracottaRose.copy(alpha = 0.08f),
    modifier = modifier
      .fillMaxWidth()
      .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
    ) {
      Icon(
        imageVector = Icons.Default.PlayArrow,
        contentDescription = null,
        tint = BrandTerracottaRose,
        modifier = Modifier.size(16.dp)
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = "Modo Demo Activo — Alterna arriba entre Ella 🌙 y Novio 🤝 para probar ambos roles",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Medium,
        color = BrandTerracottaRose,
        modifier = Modifier.weight(1f)
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = "Salir",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = AppleSystemBlue,
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .clickable { onExitDemo() }
          .padding(horizontal = 6.dp, vertical = 2.dp)
          .testTag("exit_demo_button")
      )
    }
  }
}
