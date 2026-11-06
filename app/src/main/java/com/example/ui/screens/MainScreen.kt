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
import com.example.ui.dialogs.SendCheckInDialog
import com.example.ui.dialogs.SymptomLogDialog
import com.example.ui.theme.AppleHairlineBorder
import com.example.ui.theme.AppleHealthFertile
import com.example.ui.theme.AppleHealthFollicularSubtle
import com.example.ui.theme.AppleSystemBlue
import com.example.ui.theme.AppleSystemGray5
import com.example.ui.theme.AppleSystemIndigo
import com.example.ui.viewmodel.MainViewModel
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

  // Dialog triggers
  var showSymptomDialog by remember { mutableStateOf(false) }
  var showIntimacyDialog by remember { mutableStateOf(false) }
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
      // iOS Style Tab Bar
      NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        modifier = Modifier
          .navigationBarsPadding()
          .border(0.5.dp, AppleHairlineBorder)
      ) {
        NavigationBarItem(
          selected = currentNavTab == 0,
          onClick = { currentNavTab = 0 },
          icon = { Icon(Icons.Default.Spa, contentDescription = "Hoy", modifier = Modifier.size(22.dp)) },
          label = { Text("Hoy", fontWeight = if (currentNavTab == 0) FontWeight.SemiBold else FontWeight.Normal) },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = AppleSystemBlue,
            selectedTextColor = AppleSystemBlue,
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
            selectedIconColor = AppleSystemBlue,
            selectedTextColor = AppleSystemBlue,
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
            selectedIconColor = AppleSystemBlue,
            selectedTextColor = AppleSystemBlue,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            indicatorColor = Color.Transparent
          ),
          modifier = Modifier.testTag("nav_tab_pairing")
        )
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
          // Loading space state
          Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              CircularProgressIndicator(color = AppleSystemBlue)
              Spacer(modifier = Modifier.height(14.dp))
              Text("Conectando con tu espacio...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        } else if (uiState.space == null && !uiState.isGuestMode) {
          // Authenticated account with NO space yet
          UnpairedAccountOnboardingView(
            isLoading = uiState.isLoading,
            onCreateSpace = { viewModel.createOwnerSpace() },
            onGoToPairing = { currentNavTab = 2 }
          )
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
                  onOpenSymptomDialog = { showSymptomDialog = true },
                  onOpenIntimacyDialog = { showIntimacyDialog = true },
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
                val last = uiState.pastCycles.firstOrNull()
                if (last != null) {
                  try { java.time.LocalDate.parse(last.startDate) } catch (e: Exception) { java.time.LocalDate.now().minusDays(14) }
                } else {
                  java.time.LocalDate.now().minusDays(14)
                }
              }
              val symptomsList = remember(uiState.todaySymptom) {
                listOfNotNull(uiState.todaySymptom)
              }
              CycleCalendarView(
                lastPeriodStart = lastPeriodStart,
                averagePeriodLength = uiState.space?.averagePeriodLength ?: 5,
                averageCycleLength = uiState.space?.averageCycleLength ?: 28,
                pastCycles = uiState.pastCycles,
                symptoms = symptomsList,
                intimacyLogs = uiState.intimacyLogs,
                predictedCycles = uiState.predictedCycles,
                canLogSymptoms = uiState.currentRole == UserRole.PRIMARY_ELLA,
                onDateSelected = { date -> viewModel.selectDate(date) },
                onOpenSymptomDialogForDate = { date ->
                  if (uiState.currentRole == UserRole.PRIMARY_ELLA) {
                    viewModel.selectDate(date)
                    showSymptomDialog = true
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
  if (showSymptomDialog) {
    SymptomLogDialog(
      initialSymptom = uiState.todaySymptom,
      onDismiss = { showSymptomDialog = false },
      onSave = { flow, pain, mood, energy, cramps, headache, backPain, bloating, mucus, breasts, cravings, acne, pubNote, privNote, isPriv ->
        viewModel.saveSymptom(
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
          isPrivate = isPriv
        )
      }
    )
  }

  if (showIntimacyDialog) {
    IntimacyLogDialog(
      onDismiss = { showIntimacyDialog = false },
      onSave = { type, lube, orgasm, effect, notes, isPriv ->
        viewModel.logIntimacy(type, lube, orgasm, effect, notes, isPriv)
      }
    )
  }

  if (showObservationDialog) {
    PartnerObservationDialog(
      onDismiss = { showObservationDialog = false },
      onSave = { perceivedMood, pain, spotting, action, msg ->
        viewModel.logPartnerObservation(
          perceivedMood = perceivedMood,
          apparentPainLevel = pain,
          unloggedSpottingReported = spotting,
          careActionTaken = action,
          sweetMessage = msg
        )
      }
    )
  }

  if (showCheckInDialog) {
    SendCheckInDialog(
      onDismiss = { showCheckInDialog = false },
      onSend = { prompt, note ->
        viewModel.sendCheckIn(prompt, note)
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
      .border(0.5.dp, AppleHairlineBorder)
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
          .background(AppleHealthFollicularSubtle)
      ) {
        Icon(
          imageVector = Icons.Default.NightsStay,
          contentDescription = null,
          tint = AppleSystemBlue,
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
        Icon(Icons.Default.Security, contentDescription = null, tint = AppleSystemBlue)
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
              colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AppleHealthFertile)
            )
          } else {
            Text(
              text = if (shareSymptoms) "Activo" else "Desactivado",
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              color = if (shareSymptoms) AppleHealthFertile else MaterialTheme.colorScheme.onSurfaceVariant
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
              colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AppleHealthFertile)
            )
          } else {
            Text(
              text = if (shareMood) "Activo" else "Desactivado",
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              color = if (shareMood) AppleHealthFertile else MaterialTheme.colorScheme.onSurfaceVariant
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
              colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AppleHealthFertile)
            )
          } else {
            Text(
              text = if (shareIntimacy) "Activo" else "Desactivado",
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              color = if (shareIntimacy) AppleHealthFertile else MaterialTheme.colorScheme.onSurfaceVariant
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
          colors = ButtonDefaults.buttonColors(containerColor = AppleSystemBlue)
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
    color = AppleHealthFollicularSubtle,
    modifier = modifier
      .fillMaxWidth()
      .border(0.5.dp, AppleHairlineBorder)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
    ) {
      Icon(
        imageVector = Icons.Default.PlayArrow,
        contentDescription = null,
        tint = AppleSystemBlue,
        modifier = Modifier.size(16.dp)
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = "Modo Demo Activo — Alterna arriba entre Ella 🌙 y Novio 🤝 para probar ambos roles",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Medium,
        color = AppleSystemBlue,
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
