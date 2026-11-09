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
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

data class AppUiState(
  val currentUser: FirebaseUser? = null,
  val isGuestMode: Boolean = false,
  val currentRole: UserRole = UserRole.PRIMARY_ELLA,
  val space: CoupleSpace? = null,
  val isResolvingSpace: Boolean = false,
  val pairingRevoked: Boolean = false,
  val activePairingCode: String? = null,
  val cycleStatus: CycleStatusData? = null,
  val pastCycles: List<CycleEntry> = emptyList(),
  val predictedCycles: List<PredictedCycle> = emptyList(),
  val selectedDate: LocalDate = LocalDate.now(),
  val todaySymptom: DailySymptom? = null,
  val intimacyLogs: List<IntimacyLog> = emptyList(),
  val partnerObservations: List<PartnerObservation> = emptyList(),
  val latestCheckIn: CheckIn? = null,
  val partnerCareTips: List<CareTip> = emptyList(),
  val activeSpottingSuggestion: PartnerObservation? = null,
  val isLoading: Boolean = false,
  val errorMessage: String? = null,
  val successMessage: String? = null
)

class MainViewModel(
  application: Application,
  val authRepo: AuthRepository = AuthRepository(application),
  val spaceRepo: CoupleSpaceRepository = CoupleSpaceRepository(application),
  val cycleRepo: CycleRepository = CycleRepository(application),
  val intimacyRepo: IntimacyRepository = IntimacyRepository(application),
  val partnerRepo: PartnerSupportRepository = PartnerSupportRepository(application)
) : AndroidViewModel(application) {

  private val _uiState = MutableStateFlow(AppUiState())
  val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

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
          currentSessionId = UUID.randomUUID().toString()
          activeObservedUserId = user.uid
          observeUserData(user.uid, currentSessionId)
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
    if (_uiState.value.isGuestMode) {
      authRepo.setRole(role)
      _uiState.value = _uiState.value.copy(currentRole = role)
    }
  }

  fun selectDate(date: LocalDate) {
    _uiState.value = _uiState.value.copy(selectedDate = date)
    refreshCycleCalculations()
  }

  fun clearMessages() {
    _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
  }

  internal fun isSessionValid(sessionId: String, expectedUserId: String? = null): Boolean {
    if (currentSessionId != sessionId) return false
    if (expectedUserId != null && activeObservedUserId != expectedUserId) return false
    return true
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
              intimacyLogs = emptyList(),
              partnerObservations = emptyList(),
              todaySymptom = null,
              latestCheckIn = null,
              activePairingCode = null,
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
          val todayStr = LocalDate.now().toString()
          val todayEntry = symptoms.firstOrNull { it.date == todayStr }
          _uiState.value = _uiState.value.copy(todaySymptom = todayEntry)
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
    val lastCycle = cycles.firstOrNull()
    val lastPeriodStart = if (lastCycle != null) {
      try { LocalDate.parse(lastCycle.startDate) } catch (e: Exception) { LocalDate.now().minusDays(14) }
    } else {
      LocalDate.now().minusDays(14)
    }

    val status = CycleCalculationEngine.calculateCycleStatus(
      lastPeriodStart = lastPeriodStart,
      averagePeriodDays = space.averagePeriodLength,
      pastCycles = cycles,
      configuredAverageCycle = space.averageCycleLength,
      today = _uiState.value.selectedDate
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
  }

  fun createOwnerSpace() {
    val user = _uiState.value.currentUser ?: return
    val sessionId = currentSessionId
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true)
      try {
        val spaceId = spaceRepo.createSpace(
          ownerId = user.uid,
          averageCycleLength = 28,
          averagePeriodLength = 5
        )
        val periodStart = LocalDate.now().minusDays(12).toString()
        cycleRepo.addCycleEntry(spaceId, periodStart, 5)
        if (isSessionValid(sessionId, user.uid)) {
          _uiState.value = _uiState.value.copy(
            isLoading = false,
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
    val today = LocalDate.now()
    val periodStart = today.minusDays(13)
    val demoCycles = listOf(
      CycleEntry(
        id = "demo_cycle_1",
        spaceId = "demo_space",
        startDate = periodStart.toString(),
        cycleLength = 28,
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

    _uiState.value = _uiState.value.copy(
      isGuestMode = true,
      currentRole = UserRole.PRIMARY_ELLA,
      space = demoSpace,
      pastCycles = demoCycles,
      predictedCycles = demoPredictions,
      cycleStatus = cycleStatus,
      partnerCareTips = careTips,
      todaySymptom = demoSymptom,
      intimacyLogs = demoIntimacy,
      partnerObservations = demoObservation,
      latestCheckIn = demoCheckIn,
      activePairingCode = "LUNA-DEMO",
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
    if (_uiState.value.isGuestMode) {
      _uiState.value = _uiState.value.copy(
        activePairingCode = "LUNA-DEMO",
        successMessage = "Código demo activo: LUNA-DEMO"
      )
      return Job().apply { complete() }
    }
    val space = _uiState.value.space ?: return Job().apply { complete() }
    val user = _uiState.value.currentUser ?: return Job().apply { complete() }
    val sessionId = currentSessionId

    return viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true)
      try {
        val code = spaceRepo.generateInviteCode(space.id, user.uid)
        if (isSessionValid(sessionId, user.uid)) {
          _uiState.value = _uiState.value.copy(
            activePairingCode = code,
            isLoading = false,
            successMessage = "¡Código de vinculación generado con éxito!"
          )
        }
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        if (isSessionValid(sessionId, user.uid)) {
          _uiState.value = _uiState.value.copy(
            isLoading = false,
            errorMessage = e.localizedMessage ?: "Error al generar código"
          )
        }
      }
    }
  }

  fun joinWithCode(code: String): Job {
    if (_uiState.value.isGuestMode) {
      _uiState.value = _uiState.value.copy(
        successMessage = "¡Vinculado en Modo Demo con éxito!"
      )
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

  fun revokePairing() {
    if (_uiState.value.isGuestMode) {
      val currentSpace = _uiState.value.space
      if (currentSpace != null) {
        _uiState.value = _uiState.value.copy(
          space = currentSpace.copy(partnerId = null, status = SpaceStatus.UNPAIRED.name),
          activePairingCode = null,
          successMessage = "Pareja desvinculada en modo Demo"
        )
      }
      return
    }

    val space = _uiState.value.space ?: return
    val sessionId = currentSessionId
    val user = _uiState.value.currentUser

    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true)
      try {
        spaceRepo.revokePairing(space.id)
        if (isSessionValid(sessionId, user?.uid)) {
          _uiState.value = _uiState.value.copy(
            isLoading = false,
            activePairingCode = null,
            successMessage = "Has desvinculado a tu pareja. El acceso ha sido revocado en el servidor."
          )
        }
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        if (isSessionValid(sessionId, user?.uid)) {
          _uiState.value = _uiState.value.copy(
            isLoading = false,
            errorMessage = e.localizedMessage ?: "Error al desvincular pareja"
          )
        }
      }
    }
  }

  fun saveSymptom(
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
    onComplete: (Boolean, String?) -> Unit
  ): Job {
    val dateStr = _uiState.value.selectedDate.toString()
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
      loggedBy = _uiState.value.currentUser?.uid ?: "demo_ella_user"
    )

    if (_uiState.value.isGuestMode) {
      _uiState.value = _uiState.value.copy(
        todaySymptom = symptom,
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

    return viewModelScope.launch {
      try {
        cycleRepo.saveDailySymptom(symptom)
        if (isSessionValid(sessionId, user?.uid)) {
          _uiState.value = _uiState.value.copy(
            todaySymptom = symptom,
            successMessage = "Síntomas guardados correctamente"
          )
          refreshCycleCalculations()
          onComplete(true, null)
        } else {
          onComplete(false, "Sesión no válida")
        }
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        val err = e.localizedMessage ?: "Error al guardar síntomas"
        if (isSessionValid(sessionId, user?.uid)) {
          _uiState.value = _uiState.value.copy(errorMessage = err)
        }
        onComplete(false, err)
      }
    }
  }

  fun logIntimacy(
    encounterType: EncounterType,
    usedLubricant: Boolean = false,
    hadOrgasm: Boolean = false,
    postContactEffect: PostContactEffect = PostContactEffect.NONE,
    notes: String?,
    isPrivate: Boolean,
    onComplete: (Boolean, String?) -> Unit
  ) {
    val dateStr = _uiState.value.selectedDate.toString()
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
        intimacyLogs = listOf(log) + _uiState.value.intimacyLogs,
        successMessage = "Registro íntimo guardado"
      )
      onComplete(true, null)
      return
    }

    val space = _uiState.value.space
    val user = _uiState.value.currentUser
    if (space == null || user == null) {
      onComplete(false, "No hay usuario o espacio activo")
      return
    }

    val sessionId = currentSessionId

    viewModelScope.launch {
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
          _uiState.value = _uiState.value.copy(successMessage = "Registro íntimo guardado")
          onComplete(true, null)
        } else {
          onComplete(false, "Sesión no válida")
        }
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        val err = e.localizedMessage ?: "Error al guardar intimidad"
        if (isSessionValid(sessionId, user.uid)) {
          _uiState.value = _uiState.value.copy(errorMessage = err)
        }
        onComplete(false, err)
      }
    }
  }

  fun logPartnerObservation(
    perceivedMood: PerceivedMood,
    apparentPainLevel: Int,
    unloggedSpottingReported: Boolean,
    careActionTaken: String?,
    sweetMessage: String?
  ) {
    val dateStr = LocalDate.now().toString()
    val obs = PartnerObservation(
      id = "obs_${System.currentTimeMillis()}",
      spaceId = _uiState.value.space?.id ?: "demo_space",
      date = dateStr,
      perceivedMood = perceivedMood.name,
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
      return
    }

    val space = _uiState.value.space ?: return
    val user = _uiState.value.currentUser ?: return
    val sessionId = currentSessionId

    viewModelScope.launch {
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
        }
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        if (isSessionValid(sessionId, user.uid)) {
          _uiState.value = _uiState.value.copy(errorMessage = e.localizedMessage ?: "Error al registrar observación")
        }
      }
    }
  }

  fun sendCheckIn(promptType: CheckInPromptType, customNote: String?) {
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
      return
    }

    val space = _uiState.value.space ?: return
    val user = _uiState.value.currentUser ?: return
    val sessionId = currentSessionId

    viewModelScope.launch {
      try {
        partnerRepo.sendCheckIn(
          spaceId = space.id,
          senderUid = user.uid,
          promptType = promptType,
          customNote = customNote
        )
        if (isSessionValid(sessionId, user.uid)) {
          _uiState.value = _uiState.value.copy(successMessage = "¡Check-in afectivo enviado a tu pareja!")
        }
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        if (isSessionValid(sessionId, user.uid)) {
          _uiState.value = _uiState.value.copy(errorMessage = e.localizedMessage ?: "Error al enviar check-in")
        }
      }
    }
  }

  fun answerCheckIn(responseOption: QuickResponseOption, responseNote: String? = null) {
    if (_uiState.value.isGuestMode) {
      val current = _uiState.value.latestCheckIn
      if (current != null) {
        _uiState.value = _uiState.value.copy(
          latestCheckIn = current.copy(
            status = CheckInStatus.ANSWERED.name,
            responseOption = responseOption.name,
            responseNote = responseNote,
            answeredAt = Timestamp.now()
          ),
          successMessage = "¡Respuesta enviada! Cambia a 'Novio 🤝' para ver el feedback en su dashboard."
        )
      }
      return
    }

    val space = _uiState.value.space ?: return
    val checkIn = _uiState.value.latestCheckIn ?: return
    val sessionId = currentSessionId
    val user = _uiState.value.currentUser

    viewModelScope.launch {
      try {
        partnerRepo.answerCheckIn(
          spaceId = space.id,
          checkInId = checkIn.id,
          responseOption = responseOption,
          responseNote = responseNote
        )
        if (isSessionValid(sessionId, user?.uid)) {
          _uiState.value = _uiState.value.copy(successMessage = "¡Respuesta enviada a tu pareja!")
        }
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        if (isSessionValid(sessionId, user?.uid)) {
          _uiState.value = _uiState.value.copy(errorMessage = e.localizedMessage ?: "Error al responder check-in")
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
      if (isSessionValid(sessionId, user?.uid)) {
        _uiState.value = _uiState.value.copy(
          space = space.copy(sharingPermissions = permissions),
          successMessage = "Permisos de privacidad actualizados en el servidor"
        )
      }
      Result.success(Unit)
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      if (isSessionValid(sessionId, user?.uid)) {
        _uiState.value = _uiState.value.copy(errorMessage = e.localizedMessage ?: "Error al actualizar permisos")
      }
      Result.failure(e)
    }
  }
}
