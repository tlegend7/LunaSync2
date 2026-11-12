package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.engine.CycleCalculationEngine
import com.example.data.model.CareTip
import com.example.data.model.CervicalMucus
import com.example.data.model.CheckIn
import com.example.data.model.CheckInPromptType
import com.example.data.model.CheckInStatus
import com.example.data.model.CoupleSpace
import com.example.data.model.CycleEntry
import com.example.data.model.CyclePhase
import com.example.data.model.CycleStatusData
import com.example.data.model.DailySymptom
import com.example.data.model.EncounterType
import com.example.data.model.FlowLevel
import com.example.data.model.IntimacyLog
import com.example.data.model.Mood
import com.example.data.model.PartnerObservation
import com.example.data.model.PairingInvite
import com.example.data.model.PerceivedMood
import com.example.data.model.PostContactEffect
import com.example.data.model.PredictedCycle
import com.example.data.model.QuickResponseOption
import com.example.data.model.SharingPermissions
import com.example.data.model.SpaceStatus
import com.example.data.model.SpottingSuggestionStatus
import com.example.data.model.UserRole
import com.example.data.repository.AuthRepository
import com.example.data.repository.CoupleSpaceRepository
import com.example.data.repository.CycleRepository
import com.example.data.repository.IntimacyRepository
import com.example.data.repository.PartnerSupportRepository
import com.example.data.repository.SpaceObservationResult
import com.example.data.preferences.AppearancePreferences
import com.example.data.preferences.AppAppearanceMode
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.example.data.model.DayCycleDetail
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

data class AppUiState(
  val currentUser: FirebaseUser? = null,
  val isGuestMode: Boolean = false,
  val currentRole: UserRole = UserRole.PRIMARY_ELLA,
  val space: CoupleSpace? = null,
  val isResolvingSpace: Boolean = false,
  val pairingRevoked: Boolean = false,
  val activePairingInvite: PairingInvite? = null,
  val pairingCodeError: String? = null,
  val cycleStatus: CycleStatusData? = null,
  val pastCycles: List<CycleEntry> = emptyList(),
  val predictedCycles: List<PredictedCycle> = emptyList(),
  val selectedDate: LocalDate = LocalDate.now(),
  val selectedDayDetail: DayCycleDetail? = null,
  val todaySymptom: DailySymptom? = null,
  val allSymptoms: List<DailySymptom> = emptyList(),
  val intimacyLogs: List<IntimacyLog> = emptyList(),
  val partnerObservations: List<PartnerObservation> = emptyList(),
  val latestCheckIn: CheckIn? = null,
  val partnerCareTips: List<CareTip> = emptyList(),
  val activeSpottingSuggestion: PartnerObservation? = null,
  val isLoading: Boolean = false,
  val errorMessage: String? = null,
  val successMessage: String? = null
) {
  val activePairingCode: String? get() = activePairingInvite?.inviteCode
}

class MainViewModel @JvmOverloads constructor(
  application: Application,
  val authRepo: AuthRepository = AuthRepository(application),
  val spaceRepo: CoupleSpaceRepository = CoupleSpaceRepository(application),
  val cycleRepo: CycleRepository = CycleRepository(application),
  val intimacyRepo: IntimacyRepository = IntimacyRepository(application),
  val partnerRepo: PartnerSupportRepository = PartnerSupportRepository(application)
) : AndroidViewModel(application) {

  private val appearancePreferences = AppearancePreferences(application)
  val appearanceMode = appearancePreferences.mode
  fun selectAppearance(mode: AppAppearanceMode) = appearancePreferences.select(mode)

  private val _uiState = MutableStateFlow(AppUiState())
  val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

  // Reloj centralizado para pruebas controladas y fechas consistentes
  var clock: java.time.Clock = java.time.Clock.systemDefaultZone()
  fun getToday(): LocalDate = LocalDate.now(clock)

  // Track session identity strictly to protect against cross-session race conditions
  internal var currentSessionId: String = UUID.randomUUID().toString()
  internal var activeObservedUserId: String? = null
  private var userSpaceJob: Job? = null
  private val spaceDataJobs = mutableListOf<Job>()

  init {
    viewModelScope.launch {
      authRepo.currentUser.collectLatest { user ->
        _uiState.value = _uiState.value.copy(currentUser = user)
        if (user != null) {
          if (activeObservedUserId != user.uid) {
            cancelAllListeners()
            currentSessionId = UUID.randomUUID().toString()
            activeObservedUserId = user.uid
            _uiState.value = AppUiState(currentUser = user, selectedDate = getToday(), isResolvingSpace = true)
            observeUserData(user.uid, currentSessionId)
          }
        } else {
          currentSessionId = UUID.randomUUID().toString()
          activeObservedUserId = null
          cancelAllListeners()
          if (!_uiState.value.isGuestMode) {
            _uiState.value = AppUiState()
          }
        }
      }
    }
  }

  private fun cancelAllListeners() {
    userSpaceJob?.cancel()
    userSpaceJob = null
    spaceDataJobs.forEach { it.cancel() }
    spaceDataJobs.clear()
  }

  fun switchRole(role: UserRole) {
    if (!_uiState.value.isGuestMode) return
    authRepo.setRole(role)
    _uiState.value = _uiState.value.copy(currentRole = role)
  }

  fun selectOnboardingRole(role: UserRole) {
    if (_uiState.value.currentUser != null || authRepo.currentUser.value != null || _uiState.value.space != null) return
    authRepo.setRole(role)
    _uiState.value = _uiState.value.copy(currentRole = role)
  }

  private fun canManageSpace(): Boolean {
    val state = _uiState.value
    return if (state.isGuestMode) state.space != null && state.currentRole == UserRole.PRIMARY_ELLA
    else state.currentUser != null && state.currentUser.uid == state.space?.ownerId
  }

  fun selectDate(date: LocalDate) {
    _uiState.value = _uiState.value.copy(selectedDate = date)
    updateSelectedDayDetail()
  }

  fun updateSelectedDayDetail() {
    val space = _uiState.value.space
    val cycles = _uiState.value.pastCycles
    val lastCycle = cycles.firstOrNull()
    val lastPeriodStart = lastCycle?.let {
      try { LocalDate.parse(it.startDate) } catch (e: Exception) { null }
    }

    val symptomDates = _uiState.value.allSymptoms.mapNotNull {
      try { LocalDate.parse(it.date) } catch (e: Exception) { null }
    }.toSet()

    val intimacyDates = _uiState.value.intimacyLogs.mapNotNull {
      try { LocalDate.parse(it.date) } catch (e: Exception) { null }
    }.toSet()

    val confirmedBleedingDates = _uiState.value.allSymptoms.filter {
      it.flowLevel in listOf("LIGHT", "MEDIUM", "HEAVY")
    }.mapNotNull {
      try { LocalDate.parse(it.date) } catch (e: Exception) { null }
    }.toSet()

    val detail = CycleCalculationEngine.getDayDetail(
      date = _uiState.value.selectedDate,
      lastPeriodStart = lastPeriodStart,
      averagePeriodDays = space?.averagePeriodLength ?: 5,
      pastCycles = cycles,
      configuredAverageCycle = space?.averageCycleLength ?: 28,
      today = getToday(),
      symptomDates = symptomDates,
      intimacyDates = intimacyDates,
      confirmedBleedingDates = confirmedBleedingDates
    )

    _uiState.value = _uiState.value.copy(selectedDayDetail = detail)
  }

  fun clearMessages() {
    _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
  }

  internal fun isSessionValid(sessionId: String, expectedUserId: String? = null): Boolean {
    if (currentSessionId != sessionId) return false
    if (expectedUserId != null && activeObservedUserId != expectedUserId) return false
    return true
  }

  internal fun sanitizeUserErrorMessage(e: Throwable, defaultFallback: String): String {
    val msg = e.message ?: return defaultFallback
    return when {
      msg.contains("PERMISSION_DENIED", ignoreCase = true) || msg.contains("permiso", ignoreCase = true) ->
        "No tienes permisos suficientes para realizar esta acción."
      msg.contains("UNAVAILABLE", ignoreCase = true) || msg.contains("network", ignoreCase = true) || msg.contains("conexión", ignoreCase = true) ->
        "Problema de conexión con el servidor. Verifica tu internet e intenta nuevamente."
      msg.contains("DEADLINE_EXCEEDED", ignoreCase = true) || msg.contains("timeout", ignoreCase = true) ->
        "La operación tardó demasiado tiempo. Por favor, reintenta."
      msg.contains("NOT_FOUND", ignoreCase = true) ->
        "El registro no fue encontrado o ya no está disponible."
      msg.contains("ALREADY_EXISTS", ignoreCase = true) ->
        "Ya existe un registro para esta fecha."
      else -> defaultFallback
    }
  }

  internal fun setUiStateForTesting(update: (AppUiState) -> AppUiState) {
    _uiState.value = update(_uiState.value)
  }

  private fun observeUserData(userId: String, sessionId: String) {
    userSpaceJob?.cancel()
    _uiState.value = _uiState.value.copy(isResolvingSpace = true)

    userSpaceJob = viewModelScope.launch {
      spaceRepo.observeUserSpaceDetailed(userId).collectLatest { result ->
        if (!isSessionValid(sessionId, userId)) {
          return@collectLatest
        }

        when (result) {
          is SpaceObservationResult.Loading -> {
            _uiState.value = _uiState.value.copy(isResolvingSpace = true)
          }
          is SpaceObservationResult.Success -> {
            val space = result.space
            val resolvedRole = when {
              space.ownerId == userId -> UserRole.PRIMARY_ELLA
              space.partnerId == userId -> UserRole.SUPPORT_PARTNER
              else -> UserRole.PRIMARY_ELLA
            }
            authRepo.setRole(resolvedRole)
            _uiState.value = _uiState.value.copy(
              space = space,
              currentRole = resolvedRole,
              isResolvingSpace = false,
              pairingRevoked = false
            )
            observeSpaceData(space.id, resolvedRole, space.sharingPermissions, userId, sessionId)
          }
          is SpaceObservationResult.PairingLost -> {
            cancelSpaceDataListeners()
            _uiState.value = _uiState.value.copy(
              space = null,
              pastCycles = emptyList(),
              allSymptoms = emptyList(),
              predictedCycles = emptyList(),
              cycleStatus = null,
              selectedDayDetail = null,
              partnerCareTips = emptyList(),
              activeSpottingSuggestion = null,
              intimacyLogs = emptyList(),
              partnerObservations = emptyList(),
              todaySymptom = null,
              latestCheckIn = null,
              activePairingInvite = null,
              pairingCodeError = null,
              isLoading = false,
              isResolvingSpace = false,
              pairingRevoked = true,
              errorMessage = "Tu vinculación con el espacio de pareja ha sido revocada o finalizada."
            )
          }
          is SpaceObservationResult.NoSpaceFound -> {
            _uiState.value = _uiState.value.copy(
              space = null,
              isResolvingSpace = false
            )
          }
          is SpaceObservationResult.Error -> {
            _uiState.value = _uiState.value.copy(
              isResolvingSpace = false,
              errorMessage = "Error al conectar con tu espacio: ${result.exception.localizedMessage}"
            )
          }
        }
      }
    }
  }

  private fun cancelSpaceDataListeners() {
    spaceDataJobs.forEach { it.cancel() }
    spaceDataJobs.clear()
  }

  private fun observeSpaceData(
    spaceId: String,
    role: UserRole,
    permissions: SharingPermissions,
    sessionUserId: String,
    sessionId: String
  ) {
    cancelSpaceDataListeners()
    val isElla = role == UserRole.PRIMARY_ELLA

    val cyclesJob = viewModelScope.launch {
      cycleRepo.observeCycles(spaceId).collectLatest { cycles ->
        if (isSessionValid(sessionId, sessionUserId)) {
          _uiState.value = _uiState.value.copy(pastCycles = cycles)
          refreshCycleCalculations()
        }
      }
    }
    spaceDataJobs.add(cyclesJob)

    val symptomsJob = viewModelScope.launch {
      cycleRepo.observeDailySymptoms(
        spaceId = spaceId,
        isElla = isElla,
        shareSymptoms = permissions.shareSymptoms,
        shareMood = permissions.shareMood
      ).collectLatest { symptoms ->
        if (isSessionValid(sessionId, sessionUserId)) {
          val todayStr = getToday().toString()
          val todayEntry = symptoms.firstOrNull { it.date == todayStr }
          _uiState.value = _uiState.value.copy(
            allSymptoms = symptoms,
            todaySymptom = todayEntry
          )
          refreshCycleCalculations()
        }
      }
    }
    spaceDataJobs.add(symptomsJob)

    val intimacyJob = viewModelScope.launch {
      intimacyRepo.observeIntimacyLogs(
        spaceId = spaceId,
        isElla = isElla,
        shareIntimacyAllowed = permissions.shareIntimacy
      ).collectLatest { logs ->
        if (isSessionValid(sessionId, sessionUserId)) {
          _uiState.value = _uiState.value.copy(intimacyLogs = logs)
          updateSelectedDayDetail()
        }
      }
    }
    spaceDataJobs.add(intimacyJob)

    val observationsJob = viewModelScope.launch {
      partnerRepo.observeObservations(spaceId).collectLatest { observations ->
        if (isSessionValid(sessionId, sessionUserId)) {
          _uiState.value = _uiState.value.copy(partnerObservations = observations)
          val pendingSpotting = observations.firstOrNull {
            it.unloggedSpottingReported && it.spottingSuggestionStatus == SpottingSuggestionStatus.PENDING.name
          }
          _uiState.value = _uiState.value.copy(activeSpottingSuggestion = pendingSpotting)
        }
      }
    }
    spaceDataJobs.add(observationsJob)

    val checkInsJob = viewModelScope.launch {
      partnerRepo.observeLatestCheckIn(spaceId).collectLatest { checkIn ->
        if (isSessionValid(sessionId, sessionUserId)) {
          _uiState.value = _uiState.value.copy(latestCheckIn = checkIn)
        }
      }
    }
    spaceDataJobs.add(checkInsJob)
  }

  fun refreshCycleCalculations() {
    val space = _uiState.value.space ?: return
    val cycles = _uiState.value.pastCycles
    val today = getToday()

    if (cycles.isEmpty()) {
      // Estado sin registros reales: no rellenar silenciosamente con periodos ficticios
      _uiState.value = _uiState.value.copy(
        cycleStatus = null,
        predictedCycles = emptyList(),
        partnerCareTips = emptyList()
      )
      updateSelectedDayDetail()
      return
    }

    val lastCycle = cycles.firstOrNull()
    val lastPeriodStart = lastCycle?.let {
      try { LocalDate.parse(it.startDate) } catch (e: Exception) { null }
    }

    if (lastPeriodStart == null) {
      _uiState.value = _uiState.value.copy(
        cycleStatus = null,
        predictedCycles = emptyList(),
        partnerCareTips = emptyList()
      )
      updateSelectedDayDetail()
      return
    }

    val confirmedBleedingDates = _uiState.value.allSymptoms.filter {
      it.flowLevel in listOf("LIGHT", "MEDIUM", "HEAVY")
    }.mapNotNull {
      try { LocalDate.parse(it.date) } catch (e: Exception) { null }
    }.toSet()

    val status = CycleCalculationEngine.calculateCycleStatus(
      lastPeriodStart = lastPeriodStart,
      averagePeriodDays = space.averagePeriodLength,
      pastCycles = cycles,
      configuredAverageCycle = space.averageCycleLength,
      today = today,
      confirmedBleedingDates = confirmedBleedingDates
    )

    val futurePredictions = CycleCalculationEngine.predictFutureCycles(
      lastPeriodStart = lastPeriodStart,
      averagePeriodDays = space.averagePeriodLength,
      pastCycles = cycles,
      configuredAverageCycle = space.averageCycleLength,
      cyclesCount = 6
    )

    val tips = CycleCalculationEngine.getPartnerCareTips(status.currentPhase, status.currentDay)

    _uiState.value = _uiState.value.copy(
      cycleStatus = status,
      predictedCycles = futurePredictions,
      partnerCareTips = tips
    )
    updateSelectedDayDetail()
  }

  fun createOwnerSpace(): Job {
    val user = _uiState.value.currentUser ?: return Job().apply { complete() }
    val sessionId = currentSessionId
    return viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true)
      try {
        val spaceId = spaceRepo.createSpace(
          ownerId = user.uid,
          averageCycleLength = 28,
          averagePeriodLength = 5
        )
        // Corrección 2A.1: Crear espacio real NO crea menstruación ficticia
        if (isSessionValid(sessionId, user.uid)) {
          val createdSpace = CoupleSpace(
            id = spaceId,
            ownerId = user.uid,
            partnerId = null,
            status = SpaceStatus.UNPAIRED.name,
            averageCycleLength = 28,
            averagePeriodLength = 5,
            sharingPermissions = SharingPermissions()
          )
          _uiState.value = _uiState.value.copy(
            isLoading = false,
            space = createdSpace,
            currentRole = UserRole.PRIMARY_ELLA,
            successMessage = "¡Espacio personal creado con éxito!"
          )
        }
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        if (isSessionValid(sessionId, user.uid)) {
          _uiState.value = _uiState.value.copy(
            isLoading = false,
            errorMessage = e.localizedMessage ?: "Error al crear espacio"
          )
        }
      }
    }
  }

  fun enterGuestDemoMode() {
    currentSessionId = UUID.randomUUID().toString()
    val today = getToday()
    val periodStart = today.minusDays(13)
    val demoCycles = listOf(
      CycleEntry(
        id = "demo_cycle_1",
        spaceId = "demo_space",
        startDate = periodStart.toString(),
        cycleLength = null,
        periodDuration = 5
      )
    )

    val demoSpace = CoupleSpace(
      id = "demo_space",
      ownerId = "demo_ella_user",
      partnerId = "demo_partner_user",
      averageCycleLength = 28,
      averagePeriodLength = 5,
      status = SpaceStatus.PAIRED.name,
      sharingPermissions = SharingPermissions(
        shareSymptoms = true,
        shareMood = true,
        shareIntimacy = false,
        sharePartnerNotes = true
      )
    )

    val demoSymptom = DailySymptom(
      id = "demo_symptom_today",
      spaceId = "demo_space",
      date = today.toString(),
      flowLevel = FlowLevel.NONE.name,
      painLevel = 1,
      mood = Mood.CALM.name,
      energyLevel = 4,
      publicNote = "Día tranquilo, energía alta y antojo de chocolate 🍫",
      privateNote = "Nota privada personal",
      isPrivate = false,
      loggedBy = "demo_ella_user"
    )

    val demoIntimacy = listOf(
      IntimacyLog(
        id = "demo_intimacy_1",
        spaceId = "demo_space",
        date = today.minusDays(2).toString(),
        encounterType = EncounterType.PROTECTED.name,
        notes = "Cena romántica ✨",
        isPrivate = false,
        loggedBy = "demo_ella_user"
      )
    )

    val demoObservation = listOf(
      PartnerObservation(
        id = "demo_obs_1",
        spaceId = "demo_space",
        date = today.toString(),
        perceivedMood = PerceivedMood.CALM.name,
        apparentPainLevel = 1,
        unloggedSpottingReported = false,
        careActionTaken = "Le preparé té de manzanilla con miel 🫖",
        sweetMessage = "Te amo mucho, descansa hoy 💕",
        loggedBy = "demo_partner_user"
      )
    )

    val demoCheckIn = CheckIn(
      id = "demo_checkin_1",
      spaceId = "demo_space",
      senderUid = "demo_partner_user",
      promptType = CheckInPromptType.NEED_SPACE_OR_CUDDLES.name,
      customNote = "¿Te apetece ver una peli juntos o prefieres leer tranquila?",
      status = CheckInStatus.ANSWERED.name,
      responseOption = QuickResponseOption.CUDDLES_PLEASE.name,
      responseNote = "¡Peli y mimos por favor! 🥰",
      createdAt = Timestamp.now(),
      answeredAt = Timestamp.now()
    )

    val cycleStatus = CycleCalculationEngine.calculateCycleStatus(
      lastPeriodStart = periodStart,
      averagePeriodDays = 5,
      pastCycles = demoCycles,
      configuredAverageCycle = 28,
      today = today
    )

    val careTips = CycleCalculationEngine.getPartnerCareTips(
      cycleStatus.currentPhase,
      cycleStatus.currentDay
    )

    val demoPredictions = CycleCalculationEngine.predictFutureCycles(
      lastPeriodStart = periodStart,
      averagePeriodDays = 5,
      pastCycles = demoCycles,
      configuredAverageCycle = 28,
      cyclesCount = 6
    )

    val selectedRole = _uiState.value.currentRole
    _uiState.value = _uiState.value.copy(
      isGuestMode = true,
      currentRole = selectedRole,
      space = demoSpace,
      pastCycles = demoCycles,
      predictedCycles = demoPredictions,
      cycleStatus = cycleStatus,
      partnerCareTips = careTips,
      todaySymptom = demoSymptom,
      intimacyLogs = demoIntimacy,
      partnerObservations = demoObservation,
      latestCheckIn = demoCheckIn,
      activePairingInvite = PairingInvite(inviteCode = "LUNA-DEMO", spaceId = "demo_space", createdBy = "demo_ella_user",
        expiresAt = Timestamp(java.util.Date.from(clock.instant().plus(2, java.time.temporal.ChronoUnit.DAYS)))),
      successMessage = "¡Modo Demo activado! Puedes alternar entre Ella y Novio arriba."
    )
  }

  fun exitGuestMode() {
    currentSessionId = UUID.randomUUID().toString()
    activeObservedUserId = null
    cancelAllListeners()
    _uiState.value = AppUiState()
  }

  fun signOut(scope: kotlinx.coroutines.CoroutineScope) {
    currentSessionId = UUID.randomUUID().toString()
    activeObservedUserId = null
    cancelAllListeners()
    _uiState.value = AppUiState()
    authRepo.signOut(scope) {}
  }

  fun generatePairingCode(): Job {
    if (!canManageSpace()) {
      _uiState.value = _uiState.value.copy(pairingCodeError = "Solo la dueña puede generar una invitación.")
      return Job().apply { complete() }
    }
    if (_uiState.value.space?.status == SpaceStatus.PAIRED.name) {
      _uiState.value = _uiState.value.copy(pairingCodeError = "El vínculo ya está activo. Finalízalo antes de invitar a otra persona.")
      return Job().apply { complete() }
    }
    if (_uiState.value.isGuestMode) {
      val version = (_uiState.value.space?.activeInviteVersion ?: 1L) + 1
      _uiState.value = _uiState.value.copy(
        activePairingInvite = PairingInvite(inviteCode = "LUNA-DEMO", spaceId = "demo_space", createdBy = "demo_ella_user",
          version = version, expiresAt = Timestamp(java.util.Date.from(clock.instant().plus(2, java.time.temporal.ChronoUnit.DAYS)))),
        space = _uiState.value.space?.copy(activeInviteVersion = version),
        pairingCodeError = null,
        successMessage = "Código demo activo: LUNA-DEMO"
      )
      return Job().apply { complete() }
    }
    val space = _uiState.value.space ?: return Job().apply { complete() }
    val user = _uiState.value.currentUser ?: return Job().apply { complete() }
    if (space.status == SpaceStatus.PAIRED.name) {
      _uiState.value = _uiState.value.copy(pairingCodeError = "El vínculo ya está activo. Finalízalo antes de invitar a otra persona.")
      return Job().apply { complete() }
    }
    val sessionId = currentSessionId

    return viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true, activePairingInvite = null, pairingCodeError = null)
      try {
        val code = spaceRepo.generateInviteCode(space.id, user.uid)
        if (!isSessionValid(sessionId, user.uid) || _uiState.value.space?.id != space.id) return@launch
        val invite = spaceRepo.getInviteDetails(code)
        if (isSessionValid(sessionId, user.uid) && _uiState.value.space?.id == space.id) {
          check(invite.createdBy == user.uid && invite.spaceId == space.id && invite.status == "ACTIVE") {
            "El código ya no está disponible. Revisa el vínculo o genera uno nuevo."
          }
          _uiState.value = _uiState.value.copy(
            activePairingInvite = invite,
            space = _uiState.value.space?.let { current ->
              current.copy(activeInviteVersion = maxOf(current.activeInviteVersion, invite.version))
            },
            isLoading = false,
            successMessage = "¡Código de vinculación generado con éxito!"
          )
        }
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        if (isSessionValid(sessionId, user.uid)) {
          _uiState.value = _uiState.value.copy(
            isLoading = false,
            pairingCodeError = sanitizeUserErrorMessage(e, "No se pudo generar el código. Intenta nuevamente.")
          )
        }
      }
    }
  }

  fun joinWithCode(code: String): Job {
    if (_uiState.value.isGuestMode) {
      val state = _uiState.value
      val invite = state.activePairingInvite
      val valid = state.currentRole == UserRole.SUPPORT_PARTNER && state.space?.status == SpaceStatus.UNPAIRED.name &&
        invite != null && code.trim().uppercase(java.util.Locale.ROOT) == invite.inviteCode && invite.status == "ACTIVE" &&
        invite.version == state.space.activeInviteVersion && invite.expiresAt?.toDate()?.toInstant()?.isAfter(clock.instant()) == true
      _uiState.value = if (valid) state.copy(
        space = state.space?.copy(partnerId = "demo_partner_user", status = SpaceStatus.PAIRED.name),
        activePairingInvite = null, errorMessage = null,
        successMessage = "Vínculo simulado en la demo. No conecta cuentas reales."
      ) else state.copy(errorMessage = "Código demo inválido, vencido o vínculo ya activo. No se conectaron cuentas reales.")
      return Job().apply { complete() }
    }
    val user = _uiState.value.currentUser ?: return Job().apply { complete() }
    val sessionId = currentSessionId

    return viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true)
      val result = spaceRepo.joinWithCode(code, user.uid)
      if (isSessionValid(sessionId, user.uid)) {
        result.fold(
          onSuccess = { spaceId ->
            observeUserData(user.uid, sessionId)
            _uiState.value = _uiState.value.copy(
              isLoading = false,
              successMessage = "¡Vinculado exitosamente con tu pareja!"
            )
          },
          onFailure = { error ->
            _uiState.value = _uiState.value.copy(
              isLoading = false,
              errorMessage = error.localizedMessage ?: "Código inválido, expirado o ya consumido"
            )
          }
        )
      }
    }
  }

  fun revokePairing(onComplete: ((Boolean, String?) -> Unit)? = null): Job {
    if (_uiState.value.isGuestMode) {
      val currentSpace = _uiState.value.space
      if (currentSpace != null) {
        _uiState.value = _uiState.value.copy(
          space = currentSpace.copy(partnerId = null, status = SpaceStatus.UNPAIRED.name,
            activeInviteVersion = currentSpace.activeInviteVersion + 1),
          activePairingInvite = null,
          pairingCodeError = null,
          successMessage = "Pareja desvinculada en modo Demo"
        )
      }
      onComplete?.invoke(true, null)
      return Job().apply { complete() }
    }

    val space = _uiState.value.space
    val user = _uiState.value.currentUser
    if (space == null || user == null || user.uid !in listOf(space.ownerId, space.partnerId)) {
      onComplete?.invoke(false, "No hay un vínculo activo para finalizar.")
      return Job().apply { complete() }
    }
    val sessionId = currentSessionId

    return viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true)
      try {
        if (user.uid == space.ownerId) spaceRepo.revokePairing(space.id)
        else spaceRepo.leavePairing(space.id)
        if (isSessionValid(sessionId, user?.uid)) {
          _uiState.value = _uiState.value.copy(
            isLoading = false,
            activePairingInvite = null,
            pairingCodeError = null,
            successMessage = "El vínculo ha finalizado y el acceso compartido se ha detenido."
          )
          onComplete?.invoke(true, null)
        }
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        if (isSessionValid(sessionId, user?.uid)) {
          _uiState.value = _uiState.value.copy(
            isLoading = false,
            errorMessage = sanitizeUserErrorMessage(e, "No se pudo finalizar el vínculo. Intenta nuevamente.")
          )
          onComplete?.invoke(false, sanitizeUserErrorMessage(e, "No se pudo finalizar el vínculo. Intenta nuevamente."))
        }
      }
    }
  }

  fun saveSymptom(
    date: LocalDate = _uiState.value.selectedDate,
    flowLevel: FlowLevel,
    painLevel: Int,
    mood: Mood,
    energyLevel: Int,
    cramps: Boolean,
    headache: Boolean,
    backPain: Boolean,
    bloating: Boolean,
    cervicalMucus: CervicalMucus = CervicalMucus.NONE,
    tenderBreasts: Boolean = false,
    cravings: Boolean = false,
    acne: Boolean = false,
    publicNote: String?,
    privateNote: String?,
    isPrivate: Boolean,
    isMoodPrivate: Boolean = isPrivate,
    onComplete: (Boolean, String?) -> Unit
  ): Job {
    val today = getToday()
    if (date.isAfter(today)) {
      onComplete(false, "No se pueden registrar síntomas en fechas futuras")
      return Job().apply { complete() }
    }

    val dateStr = date.toString()
    val symptom = DailySymptom(
      spaceId = _uiState.value.space?.id ?: "demo_space",
      date = dateStr,
      flowLevel = flowLevel.name,
      painLevel = painLevel,
      cramps = cramps,
      headache = headache,
      backPain = backPain,
      bloating = bloating,
      mood = mood.name,
      energyLevel = energyLevel,
      cervicalMucus = cervicalMucus.name,
      tenderBreasts = tenderBreasts,
      cravings = cravings,
      acne = acne,
      publicNote = publicNote,
      privateNote = privateNote,
      isPrivate = isPrivate,
      isMoodPrivate = isMoodPrivate,
      loggedBy = _uiState.value.currentUser?.uid ?: "demo_ella_user"
    )

    if (_uiState.value.isGuestMode) {
      val updatedSymptoms = listOf(symptom) + _uiState.value.allSymptoms.filter { it.date != dateStr }
      val todayEntry = if (date == today) symptom else _uiState.value.todaySymptom
      _uiState.value = _uiState.value.copy(
        todaySymptom = todayEntry,
        allSymptoms = updatedSymptoms,
        successMessage = "Síntomas guardados correctamente"
      )
      refreshCycleCalculations()
      onComplete(true, null)
      return Job().apply { complete() }
    }

    val space = _uiState.value.space
    if (space == null) {
      onComplete(false, "No hay un espacio activo para guardar")
      return Job().apply { complete() }
    }

    val sessionId = currentSessionId
    val user = _uiState.value.currentUser
    var callbackDelivered = false
    val safeComplete: (Boolean, String?) -> Unit = { success, errorMsg ->
      if (!callbackDelivered) {
        callbackDelivered = true
        if (isSessionValid(sessionId, user?.uid)) {
          onComplete(success, errorMsg)
        }
      }
    }

    return viewModelScope.launch {
      try {
        cycleRepo.saveDailySymptom(symptom)
        if (isSessionValid(sessionId, user?.uid)) {
          val todayEntry = if (date == today) symptom else _uiState.value.todaySymptom
          val updatedSymptoms = listOf(symptom) + _uiState.value.allSymptoms.filter { it.date != dateStr }
          _uiState.value = _uiState.value.copy(
            todaySymptom = todayEntry,
            allSymptoms = updatedSymptoms,
            successMessage = "Síntomas guardados correctamente"
          )
          refreshCycleCalculations()
          safeComplete(true, null)
        }
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        val userFriendlyErr = sanitizeUserErrorMessage(e, "Error al guardar síntomas. Intenta nuevamente.")
        if (isSessionValid(sessionId, user?.uid)) {
          _uiState.value = _uiState.value.copy(errorMessage = userFriendlyErr)
          safeComplete(false, userFriendlyErr)
        }
      }
    }
  }

  fun logIntimacy(
    date: LocalDate = _uiState.value.selectedDate,
    encounterType: EncounterType,
    usedLubricant: Boolean = false,
    hadOrgasm: Boolean = false,
    postContactEffect: PostContactEffect = PostContactEffect.NONE,
    notes: String?,
    isPrivate: Boolean,
    onComplete: (Boolean, String?) -> Unit
  ): Job {
    val today = getToday()
    if (date.isAfter(today)) {
      onComplete(false, "No se pueden registrar encuentros en fechas futuras")
      return Job().apply { complete() }
    }

    val dateStr = date.toString()
    val log = IntimacyLog(
      id = "intimacy_${System.currentTimeMillis()}",
      spaceId = _uiState.value.space?.id ?: "demo_space",
      date = dateStr,
      encounterType = encounterType.name,
      usedLubricant = usedLubricant,
      hadOrgasm = hadOrgasm,
      postContactEffect = postContactEffect.name,
      notes = notes,
      isPrivate = isPrivate,
      loggedBy = _uiState.value.currentUser?.uid ?: "demo_ella_user"
    )

    if (_uiState.value.isGuestMode) {
      _uiState.value = _uiState.value.copy(
        intimacyLogs = listOf(log) + _uiState.value.intimacyLogs.filter { it.date != dateStr },
        successMessage = "Registro íntimo guardado"
      )
      updateSelectedDayDetail()
      onComplete(true, null)
      return Job().apply { complete() }
    }

    val space = _uiState.value.space
    val user = _uiState.value.currentUser
    if (space == null || user == null) {
      onComplete(false, "No hay usuario o espacio activo")
      return Job().apply { complete() }
    }

    val sessionId = currentSessionId
    var callbackDelivered = false
    val safeComplete: (Boolean, String?) -> Unit = { success, errorMsg ->
      if (!callbackDelivered) {
        callbackDelivered = true
        if (isSessionValid(sessionId, user.uid)) {
          onComplete(success, errorMsg)
        }
      }
    }

    return viewModelScope.launch {
      try {
        intimacyRepo.saveIntimacyLog(
          spaceId = space.id,
          date = dateStr,
          encounterType = encounterType,
          usedLubricant = usedLubricant,
          hadOrgasm = hadOrgasm,
          postContactEffect = postContactEffect,
          notes = notes,
          isPrivate = isPrivate,
          userId = user.uid
        )
        if (isSessionValid(sessionId, user.uid)) {
          _uiState.value = _uiState.value.copy(
            intimacyLogs = listOf(log) + _uiState.value.intimacyLogs.filter { it.date != dateStr },
            successMessage = "Registro íntimo guardado"
          )
          updateSelectedDayDetail()
          safeComplete(true, null)
        }
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        val userFriendlyErr = sanitizeUserErrorMessage(e, "Error al guardar intimidad. Intenta nuevamente.")
        if (isSessionValid(sessionId, user.uid)) {
          _uiState.value = _uiState.value.copy(errorMessage = userFriendlyErr)
          safeComplete(false, userFriendlyErr)
        }
      }
    }
  }

  private fun normalizeDemoCycles(cycles: List<CycleEntry>): List<CycleEntry> {
    val ordered = cycles.sortedBy { it.startDate }
    return ordered.mapIndexed { index, cycle ->
      cycle.copy(cycleLength = ordered.getOrNull(index + 1)?.let {
        ChronoUnit.DAYS.between(LocalDate.parse(cycle.startDate), LocalDate.parse(it.startDate)).toInt()
      })
    }.reversed()
  }

  fun recordPeriodStart(
    startDate: LocalDate,
    endDate: LocalDate? = null,
    onComplete: ((Boolean, String?) -> Unit)? = null
  ): Job {
    val today = getToday()
    if (startDate.isAfter(today)) {
      onComplete?.invoke(false, "No se puede registrar un inicio menstrual en el futuro")
      return Job().apply { complete() }
    }
    if (endDate != null && endDate.isAfter(today)) {
      onComplete?.invoke(false, "No se puede registrar un fin de sangrado en el futuro")
      return Job().apply { complete() }
    }
    if (endDate != null && endDate.isBefore(startDate)) {
      val msg = "La fecha de fin no puede ser anterior a la de inicio"
      _uiState.value = _uiState.value.copy(errorMessage = msg)
      onComplete?.invoke(false, msg)
      return Job().apply { complete() }
    }

    if (!canManageSpace()) {
      onComplete?.invoke(false, "Solo la dueña puede modificar su historial")
      return Job().apply { complete() }
    }
    if (_uiState.value.isGuestMode) {
      if (_uiState.value.pastCycles.any { it.startDate == startDate.toString() }) {
        onComplete?.invoke(false, "Ya existe un inicio menstrual en esa fecha")
        return Job().apply { complete() }
      }
      val periodDuration = if (endDate != null) {
        (ChronoUnit.DAYS.between(startDate, endDate) + 1).toInt().coerceIn(1, 120)
      } else 5
      val newCycle = CycleEntry(
        id = "demo_cycle_${System.currentTimeMillis()}",
        spaceId = "demo_space",
        startDate = startDate.toString(),
        endDate = endDate?.toString(),
        cycleLength = null,
        periodDuration = periodDuration,
        isCalculated = endDate == null
      )
      val updatedCycles = normalizeDemoCycles(listOf(newCycle) + _uiState.value.pastCycles)
      _uiState.value = _uiState.value.copy(
        pastCycles = updatedCycles,
        successMessage = "Inicio menstrual registrado (Modo Demo)"
      )
      refreshCycleCalculations()
      onComplete?.invoke(true, null)
      return Job().apply { complete() }
    }

    val space = _uiState.value.space
    val user = _uiState.value.currentUser
    if (space == null || user == null) {
      onComplete?.invoke(false, "No hay espacio o usuario activo")
      return Job().apply { complete() }
    }

    val sessionId = currentSessionId

    return viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true)
      val res = cycleRepo.recordPeriodStart(
        spaceId = space.id,
        startDate = startDate.toString(),
        endDate = endDate?.toString(),
        periodDuration = space.averagePeriodLength
      )
      if (isSessionValid(sessionId, user.uid)) {
        res.fold(
          onSuccess = {
            _uiState.value = _uiState.value.copy(
              isLoading = false,
              successMessage = "¡Inicio menstrual registrado con éxito!"
            )
            refreshCycleCalculations()
            onComplete?.invoke(true, null)
          },
          onFailure = { error ->
            val msg = sanitizeUserErrorMessage(error, "Error al registrar inicio menstrual. Intenta nuevamente.")
            _uiState.value = _uiState.value.copy(
              isLoading = false,
              errorMessage = msg
            )
            onComplete?.invoke(false, msg)
          }
        )
      }
    }
  }

  fun updatePeriodEnd(
    cycleId: String,
    endDate: LocalDate,
    onComplete: ((Boolean, String?) -> Unit)? = null
  ): Job {
    val today = getToday()
    if (endDate.isAfter(today)) {
      onComplete?.invoke(false, "No se puede registrar un fin de sangrado en el futuro")
      return Job().apply { complete() }
    }

    if (_uiState.value.isGuestMode) {
      val cycle = _uiState.value.pastCycles.firstOrNull { it.id == cycleId }
      if (cycle == null) {
        onComplete?.invoke(false, "No se encontró el ciclo")
        return Job().apply { complete() }
      }
      return updateCycleDates(cycleId, LocalDate.parse(cycle.startDate), endDate, onComplete)
    }
    val space = _uiState.value.space
    val user = _uiState.value.currentUser
    if (space == null || user == null) {
      onComplete?.invoke(false, "No hay espacio o usuario activo")
      return Job().apply { complete() }
    }

    val sessionId = currentSessionId

    return viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true)
      val res = cycleRepo.updatePeriodEnd(
        spaceId = space.id,
        cycleId = cycleId,
        endDate = endDate.toString()
      )
      if (isSessionValid(sessionId, user.uid)) {
        res.fold(
          onSuccess = {
            _uiState.value = _uiState.value.copy(
              isLoading = false,
              successMessage = "Fin de sangrado confirmado"
            )
            refreshCycleCalculations()
            onComplete?.invoke(true, null)
          },
          onFailure = { error ->
            val msg = sanitizeUserErrorMessage(error, "Error al actualizar fin de sangrado. Intenta nuevamente.")
            _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = msg)
            onComplete?.invoke(false, msg)
          }
        )
      }
    }
  }

  fun updateCycleDates(
    cycleId: String,
    newStartDate: LocalDate,
    newEndDate: LocalDate?,
    onComplete: ((Boolean, String?) -> Unit)? = null
  ): Job {
    val today = getToday()
    if (newStartDate.isAfter(today) || (newEndDate != null && newEndDate.isAfter(today))) {
      onComplete?.invoke(false, "Las fechas no pueden ser futuras")
      return Job().apply { complete() }
    }
    if (newEndDate != null && newEndDate.isBefore(newStartDate)) {
      onComplete?.invoke(false, "La fecha de fin no puede ser anterior al inicio")
      return Job().apply { complete() }
    }

    if (!canManageSpace()) {
      onComplete?.invoke(false, "Solo la dueña puede modificar su historial")
      return Job().apply { complete() }
    }
    if (_uiState.value.isGuestMode) {
      val cycles = _uiState.value.pastCycles
      if (cycles.none { it.id == cycleId } || cycles.any { it.id != cycleId && it.startDate == newStartDate.toString() }) {
        onComplete?.invoke(false, "No existe el ciclo o esa fecha ya tiene un inicio menstrual")
        return Job().apply { complete() }
      }
      _uiState.value = _uiState.value.copy(pastCycles = normalizeDemoCycles(cycles.map {
        if (it.id != cycleId) it else it.copy(startDate = newStartDate.toString(), endDate = newEndDate?.toString(),
          periodDuration = newEndDate?.let { end -> (ChronoUnit.DAYS.between(newStartDate, end) + 1).toInt() } ?: it.periodDuration,
          isCalculated = newEndDate == null)
      }), successMessage = "Fechas actualizadas (Modo Demo)")
      refreshCycleCalculations()
      onComplete?.invoke(true, null)
      return Job().apply { complete() }
    }
    val space = _uiState.value.space
    val user = _uiState.value.currentUser
    if (space == null || user == null) {
      onComplete?.invoke(false, "No hay espacio activo")
      return Job().apply { complete() }
    }

    val sessionId = currentSessionId

    return viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true)
      val res = cycleRepo.updateCycleDates(
        spaceId = space.id,
        cycleId = cycleId,
        newStartDate = newStartDate.toString(),
        newEndDate = newEndDate?.toString()
      )
      if (isSessionValid(sessionId, user.uid)) {
        res.fold(
          onSuccess = {
            _uiState.value = _uiState.value.copy(
              isLoading = false,
              successMessage = "Fechas de ciclo actualizadas"
            )
            refreshCycleCalculations()
            onComplete?.invoke(true, null)
          },
          onFailure = { error ->
            val msg = sanitizeUserErrorMessage(error, "Error al actualizar fechas. Intenta nuevamente.")
            _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = msg)
            onComplete?.invoke(false, msg)
          }
        )
      }
    }
  }

  fun deleteCycle(
    cycleId: String,
    onComplete: ((Boolean, String?) -> Unit)? = null
  ): Job {
    if (!canManageSpace()) {
      onComplete?.invoke(false, "Solo la dueña puede modificar su historial")
      return Job().apply { complete() }
    }
    if (_uiState.value.isGuestMode) {
      if (_uiState.value.pastCycles.none { it.id == cycleId }) {
        onComplete?.invoke(false, "No se encontró el ciclo")
        return Job().apply { complete() }
      }
      _uiState.value = _uiState.value.copy(pastCycles = normalizeDemoCycles(_uiState.value.pastCycles.filterNot { it.id == cycleId }),
        successMessage = "Ciclo eliminado (Modo Demo)")
      refreshCycleCalculations()
      onComplete?.invoke(true, null)
      return Job().apply { complete() }
    }
    val space = _uiState.value.space
    val user = _uiState.value.currentUser
    if (space == null || user == null) {
      onComplete?.invoke(false, "No hay espacio activo")
      return Job().apply { complete() }
    }

    val sessionId = currentSessionId

    return viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true)
      val res = cycleRepo.deleteCycleEntry(space.id, cycleId)
      if (isSessionValid(sessionId, user.uid)) {
        res.fold(
          onSuccess = {
            _uiState.value = _uiState.value.copy(
              isLoading = false,
              successMessage = "Ciclo eliminado correctamente"
            )
            refreshCycleCalculations()
            onComplete?.invoke(true, null)
          },
          onFailure = { error ->
            val msg = sanitizeUserErrorMessage(error, "Error al eliminar ciclo. Intenta nuevamente.")
            _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = msg)
            onComplete?.invoke(false, msg)
          }
        )
      }
    }
  }

  fun logPartnerObservation(
    perceivedMood: PerceivedMood?,
    apparentPainLevel: Int?,
    unloggedSpottingReported: Boolean,
    careActionTaken: String?,
    sweetMessage: String?,
    onComplete: ((Boolean, String?) -> Unit)? = null
  ): Job {
    val dateStr = getToday().toString()
    val obs = PartnerObservation(
      id = "obs_${System.currentTimeMillis()}",
      spaceId = _uiState.value.space?.id ?: "demo_space",
      date = dateStr,
      perceivedMood = perceivedMood?.name,
      apparentPainLevel = apparentPainLevel,
      unloggedSpottingReported = unloggedSpottingReported,
      spottingSuggestionStatus = if (unloggedSpottingReported) SpottingSuggestionStatus.PENDING.name else SpottingSuggestionStatus.DISMISSED.name,
      careActionTaken = careActionTaken,
      sweetMessage = sweetMessage,
      loggedBy = _uiState.value.currentUser?.uid ?: "demo_partner_user"
    )

    if (_uiState.value.isGuestMode) {
      _uiState.value = _uiState.value.copy(
        partnerObservations = listOf(obs) + _uiState.value.partnerObservations,
        activeSpottingSuggestion = if (unloggedSpottingReported) obs else null,
        successMessage = if (unloggedSpottingReported) "¡Observación guardada! (Sugerencia de manchado enviada a Ella)" else "¡Observación y cuidado registrados!"
      )
      onComplete?.invoke(true, null)
      return Job().apply { complete() }
    }

    val space = _uiState.value.space
    val user = _uiState.value.currentUser
    if (space == null || user == null) {
      onComplete?.invoke(false, "No hay un espacio o usuario activo para guardar")
      return Job().apply { complete() }
    }
    val sessionId = currentSessionId
    var callbackDelivered = false
    val safeComplete: (Boolean, String?) -> Unit = { success, errorMsg ->
      if (!callbackDelivered) {
        callbackDelivered = true
        if (isSessionValid(sessionId, user.uid)) {
          onComplete?.invoke(success, errorMsg)
        }
      }
    }

    return viewModelScope.launch {
      try {
        partnerRepo.logObservation(
          spaceId = space.id,
          date = dateStr,
          perceivedMood = perceivedMood,
          apparentPainLevel = apparentPainLevel,
          unloggedSpottingReported = unloggedSpottingReported,
          careActionTaken = careActionTaken,
          sweetMessage = sweetMessage,
          userId = user.uid
        )
        if (isSessionValid(sessionId, user.uid)) {
          _uiState.value = _uiState.value.copy(successMessage = "¡Observación y cuidado registrados!")
          safeComplete(true, null)
        }
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        val userFriendlyErr = sanitizeUserErrorMessage(e, "Error al registrar la observación. Intenta nuevamente.")
        if (isSessionValid(sessionId, user.uid)) {
          _uiState.value = _uiState.value.copy(errorMessage = userFriendlyErr)
          safeComplete(false, userFriendlyErr)
        }
      }
    }
  }

  fun sendCheckIn(
    promptType: CheckInPromptType,
    customNote: String?,
    onComplete: ((Boolean, String?) -> Unit)? = null
  ): Job {
    if (_uiState.value.isGuestMode) {
      val checkIn = CheckIn(
        id = "checkin_${System.currentTimeMillis()}",
        spaceId = "demo_space",
        senderUid = "demo_partner_user",
        promptType = promptType.name,
        customNote = customNote,
        status = CheckInStatus.PENDING.name,
        createdAt = Timestamp.now()
      )
      _uiState.value = _uiState.value.copy(
        latestCheckIn = checkIn,
        successMessage = "¡Check-in enviado! Cambia a 'Ella 🌙' arriba para responderlo con 1 toque."
      )
      onComplete?.invoke(true, null)
      return Job().apply { complete() }
    }

    val space = _uiState.value.space
    val user = _uiState.value.currentUser
    if (space == null || user == null) {
      onComplete?.invoke(false, "No hay un espacio o usuario activo para enviar check-in")
      return Job().apply { complete() }
    }
    val sessionId = currentSessionId
    var callbackDelivered = false
    val safeComplete: (Boolean, String?) -> Unit = { success, errorMsg ->
      if (!callbackDelivered) {
        callbackDelivered = true
        if (isSessionValid(sessionId, user.uid)) {
          onComplete?.invoke(success, errorMsg)
        }
      }
    }

    return viewModelScope.launch {
      try {
        partnerRepo.sendCheckIn(
          spaceId = space.id,
          senderUid = user.uid,
          promptType = promptType,
          customNote = customNote
        )
        if (isSessionValid(sessionId, user.uid)) {
          _uiState.value = _uiState.value.copy(successMessage = "¡Check-in afectivo enviado a tu pareja!")
          safeComplete(true, null)
        }
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        val userFriendlyErr = sanitizeUserErrorMessage(e, "Error al enviar el check-in. Intenta nuevamente.")
        if (isSessionValid(sessionId, user.uid)) {
          _uiState.value = _uiState.value.copy(errorMessage = userFriendlyErr)
          safeComplete(false, userFriendlyErr)
        }
      }
    }
  }

  fun answerCheckIn(
    responseOption: QuickResponseOption,
    responseNote: String? = null,
    checkInId: String? = null,
    onComplete: ((Boolean, String?) -> Unit)? = null
  ): Job {
    val targetId = checkInId ?: _uiState.value.latestCheckIn?.id
    if (targetId.isNullOrBlank() || !canManageSpace()) {
      onComplete?.invoke(false, "No hay un mensaje disponible para responder.")
      return Job().apply { complete() }
    }
    if (_uiState.value.isGuestMode) {
      val current = _uiState.value.latestCheckIn
      if (current != null && current.id == targetId) {
        _uiState.value = _uiState.value.copy(
          latestCheckIn = current.copy(
            status = CheckInStatus.ANSWERED.name,
            responseOption = responseOption.name,
            responseNote = responseNote,
            answeredAt = Timestamp.now()
          ),
          successMessage = "Respuesta enviada en la demostración."
        )
        onComplete?.invoke(true, null)
      } else {
        onComplete?.invoke(false, "Este mensaje ya no está disponible en la demostración.")
      }
      return Job().apply { complete() }
    }

    val space = _uiState.value.space
    val user = _uiState.value.currentUser
    if (space == null || user == null) {
      onComplete?.invoke(false, "La sesión ha cambiado. Abre el mensaje nuevamente.")
      return Job().apply { complete() }
    }
    val sessionId = currentSessionId
    var callbackDelivered = false
    val complete: (Boolean, String?) -> Unit = { success, error ->
      if (!callbackDelivered && isSessionValid(sessionId, user.uid)) {
        callbackDelivered = true
        onComplete?.invoke(success, error)
      }
    }

    return viewModelScope.launch {
      try {
        partnerRepo.answerCheckIn(
          spaceId = space.id,
          checkInId = targetId,
          responseOption = responseOption,
          responseNote = responseNote
        )
        if (isSessionValid(sessionId, user.uid)) {
          _uiState.value = _uiState.value.copy(successMessage = "¡Respuesta enviada a tu pareja!")
          complete(true, null)
        }
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        if (isSessionValid(sessionId, user.uid)) {
          val message = sanitizeUserErrorMessage(e, "No se pudo enviar la respuesta. Intenta nuevamente.")
          _uiState.value = _uiState.value.copy(errorMessage = message)
          complete(false, message)
        }
      }
    }
  }

  fun acceptSpottingSuggestion(): Job {
    if (_uiState.value.isGuestMode) {
      _uiState.value = _uiState.value.copy(
        activeSpottingSuggestion = null,
        successMessage = "Manchado confirmado y añadido a tu registro de hoy"
      )
      return Job().apply { complete() }
    }

    val space = _uiState.value.space ?: return Job().apply { complete() }
    val user = _uiState.value.currentUser ?: return Job().apply { complete() }
    val suggestion = _uiState.value.activeSpottingSuggestion ?: return Job().apply { complete() }
    val sessionId = currentSessionId

    return viewModelScope.launch {
      val result = cycleRepo.acceptSpottingSuggestion(
        spaceId = space.id,
        observationId = suggestion.id,
        date = suggestion.date,
        userId = user.uid
      )
      if (isSessionValid(sessionId, user.uid)) {
        result.fold(
          onSuccess = {
            _uiState.value = _uiState.value.copy(
              activeSpottingSuggestion = null,
              successMessage = "Manchado agregado a tu registro diario (conservando tu privacidad)"
            )
          },
          onFailure = { error ->
            _uiState.value = _uiState.value.copy(errorMessage = error.localizedMessage ?: "Error al aceptar sugerencia")
          }
        )
      }
    }
  }

  fun dismissSpottingSuggestion() {
    if (_uiState.value.isGuestMode) {
      _uiState.value = _uiState.value.copy(activeSpottingSuggestion = null)
      return
    }

    val space = _uiState.value.space ?: return
    val suggestion = _uiState.value.activeSpottingSuggestion ?: return
    val sessionId = currentSessionId
    val user = _uiState.value.currentUser

    viewModelScope.launch {
      try {
        cycleRepo.dismissSpottingSuggestion(space.id, suggestion.id)
        if (isSessionValid(sessionId, user?.uid)) {
          _uiState.value = _uiState.value.copy(activeSpottingSuggestion = null)
        }
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        if (isSessionValid(sessionId, user?.uid)) {
          _uiState.value = _uiState.value.copy(errorMessage = e.localizedMessage ?: "Error al descartar sugerencia")
        }
      }
    }
  }

  suspend fun updateSharingPermissions(permissions: SharingPermissions): Result<Unit> {
    if (!canManageSpace()) {
      return Result.failure(IllegalStateException("Solo la dueña puede cambiar lo que comparte."))
    }
    if (_uiState.value.isGuestMode) {
      val currentSpace = _uiState.value.space
      if (currentSpace != null) {
        _uiState.value = _uiState.value.copy(
          space = currentSpace.copy(sharingPermissions = permissions),
          successMessage = "Permisos de privacidad actualizados"
        )
      }
      return Result.success(Unit)
    }

    val space = _uiState.value.space ?: return Result.failure(IllegalStateException("No hay espacio activo"))
    val sessionId = currentSessionId
    val user = _uiState.value.currentUser

    return try {
      spaceRepo.updatePermissions(space.id, permissions)
      if (!isSessionValid(sessionId, user?.uid) || _uiState.value.space?.id != space.id) {
        return Result.failure(CancellationException("La sesión ha cambiado."))
      }
      _uiState.value = _uiState.value.copy(
        space = _uiState.value.space?.copy(sharingPermissions = permissions),
        successMessage = "Permisos de privacidad actualizados en el servidor"
      )
      Result.success(Unit)
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      if (isSessionValid(sessionId, user?.uid)) {
        _uiState.value = _uiState.value.copy(errorMessage = sanitizeUserErrorMessage(e, "No se pudieron guardar los permisos. Intenta nuevamente."))
      }
      Result.failure(e)
    }
  }
}
