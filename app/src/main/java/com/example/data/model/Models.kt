package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.time.LocalDate

enum class UserRole {
  PRIMARY_ELLA,
  SUPPORT_PARTNER
}

enum class SpaceStatus {
  UNPAIRED,
  PENDING,
  PAIRED,
  REVOKED
}

enum class CyclePhase {
  MENSTRUAL,
  FOLLICULAR,
  OVULATORY,
  LUTEAL,
  DELAYED
}

enum class FlowLevel {
  NONE,
  SPOTTING,
  LIGHT,
  MEDIUM,
  HEAVY
}

enum class Mood {
  HAPPY,
  CALM,
  SENSITIVE,
  ENERGETIC,
  CRAVINGS,
  IRRITABLE,
  ANXIOUS,
  SAD
}

enum class CervicalMucus {
  NONE,
  DRY,
  STICKY,
  CREAMY,
  EGG_WHITE // Highly fertile
}

enum class EncounterType {
  PROTECTED,
  UNPROTECTED,
  OUTERCOURSE
}

enum class PostContactEffect {
  NONE,
  AROUSAL_FLUID, // Mayor humedad o fluido de excitación natural (trasudado)
  CONTACT_SPOTTING, // Manchado leve por fricción o cuello uterino sensible
  FRICTION_SENSITIVITY, // Sensibilidad, sequedad o ardor leve por fricción
  PH_SHIFT // Alteración temporal de pH y consistencia (frecuente tras semen alcalino)
}

enum class PerceivedMood {
  CALM,
  TIRED,
  SAD,
  IRRITABLE,
  LOVING
}

enum class SpottingSuggestionStatus {
  PENDING,
  ACCEPTED,
  DISMISSED
}

enum class CheckInPromptType {
  HOW_ARE_YOU_FEELING,
  NEED_SPACE_OR_CUDDLES,
  ARE_YOU_UPSET_WITH_ME,
  CAN_I_BRING_YOU_SOMETHING
}

enum class CheckInStatus {
  PENDING,
  ANSWERED
}

enum class QuickResponseOption {
  ALL_GOOD_JUST_CRAMPS,
  NEED_SILENCE,
  LET_US_TALK,
  CUDDLES_PLEASE
}

@IgnoreExtraProperties
data class SharingPermissions(
  val shareSymptoms: Boolean = true,
  val shareMood: Boolean = true,
  val shareIntimacy: Boolean = false,
  val sharePartnerNotes: Boolean = true
)

@IgnoreExtraProperties
data class CoupleSpace(
  @DocumentId val id: String = "",
  val ownerId: String = "",
  val partnerId: String? = null,
  val status: String = SpaceStatus.UNPAIRED.name,
  val averageCycleLength: Int = 28,
  val averagePeriodLength: Int = 5,
  val sharingPermissions: SharingPermissions = SharingPermissions(),
  val activeInviteVersion: Long = 1L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PairingInvite(
  @DocumentId val inviteCode: String = "",
  val spaceId: String = "",
  val createdBy: String = "",
  val status: String = "ACTIVE",
  val version: Long = 1L,
  val expiresAt: Timestamp? = null,
  @ServerTimestamp val createdAt: Timestamp? = null
)

/**
 * Registro de inicio de ciclo menstrual.
 *
 * Definición inequívoca de campos:
 * - [startDate]: Fecha (ISO YYYY-MM-DD) de inicio del sangrado / menstruación (Día 1 del ciclo).
 * - [endDate]: Fecha (ISO YYYY-MM-DD) en que la usuaria confirmó el fin del sangrado menstrual (último día de sangrado).
 *              null si no ha sido confirmado aún por la usuaria. NUNCA representa el fin del ciclo.
 * - [cycleLength]: Duración en días del ciclo menstrual completo, derivada exclusivamente de la distancia entre dos
 *                  inicios consecutivos (siguiente.startDate - este.startDate). null para el ciclo activo más reciente
 *                  sin inicio posterior (incompleto).
 * - [periodDuration]: Duración en días del sangrado. Si [endDate] != null, es la duración real confirmada; si [endDate] == null,
 *                     es una estimación predeterminada (con [isCalculated] = true).
 * - [isCalculated]: true si [periodDuration] o el ciclo se basan en valores estimados o sin confirmación de fin de sangrado;
 *                   false si el sangrado fue explícitamente confirmado con fecha de fin.
 */
@IgnoreExtraProperties
data class CycleEntry(
  @DocumentId val id: String = "",
  val spaceId: String = "",
  val startDate: String = "", // ISO YYYY-MM-DD: Inicio menstrual registrado (Día 1 del sangrado)
  val endDate: String? = null, // ISO YYYY-MM-DD: Fin del sangrado confirmado por la usuaria (null si aún no finalizó/confirmó)
  val cycleLength: Int? = null, // Duración total del ciclo completo derivada de 2 inicios consecutivos (null si es el ciclo en curso)
  val periodDuration: Int = 5, // Días de sangrado (real si endDate != null, estimado si endDate == null)
  val isCalculated: Boolean = false, // true si es una estimación no confirmada, false si fue confirmada por la usuaria
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DailySymptom(
  @DocumentId val id: String = "",
  val spaceId: String = "",
  val date: String = "", // ISO YYYY-MM-DD
  val flowLevel: String = FlowLevel.NONE.name,
  val painLevel: Int = 0, // 0 to 5
  val cramps: Boolean = false,
  val headache: Boolean = false,
  val backPain: Boolean = false,
  val bloating: Boolean = false,
  val mood: String = Mood.CALM.name,
  val energyLevel: Int = 3, // 1 to 5
  val cervicalMucus: String = CervicalMucus.NONE.name,
  val tenderBreasts: Boolean = false,
  val cravings: Boolean = false,
  val acne: Boolean = false,
  val publicNote: String? = null,
  val privateNote: String? = null,
  @get:PropertyName("isPrivate") val isPrivate: Boolean = false,
  val loggedBy: String = "",
  @ServerTimestamp val updatedAt: Timestamp? = null,
  @get:Exclude val hasPhysicalRecord: Boolean = true,
  @get:Exclude val hasMoodRecord: Boolean = true,
  // Null preserves the single privacy flag of legacy records until their split documents are read.
  @get:Exclude val isMoodPrivate: Boolean? = null
)

@IgnoreExtraProperties
data class PhysicalSymptomsRecord(
  @DocumentId val id: String = "",
  val spaceId: String = "",
  val date: String = "",
  val flowLevel: String = FlowLevel.NONE.name,
  val painLevel: Int = 0,
  val cramps: Boolean = false,
  val headache: Boolean = false,
  val backPain: Boolean = false,
  val bloating: Boolean = false,
  val cervicalMucus: String = CervicalMucus.NONE.name,
  val tenderBreasts: Boolean = false,
  val cravings: Boolean = false,
  val acne: Boolean = false,
  val publicNote: String? = null,
  @get:PropertyName("isPrivate") val isPrivate: Boolean = false,
  val loggedBy: String = "",
  val migratedAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MoodRecord(
  @DocumentId val id: String = "",
  val spaceId: String = "",
  val date: String = "",
  val mood: String = Mood.CALM.name,
  val energyLevel: Int = 3,
  @get:PropertyName("isPrivate") val isPrivate: Boolean = false,
  val loggedBy: String = "",
  val migratedAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PrivateDailyNote(
  @DocumentId val id: String = "",
  val spaceId: String = "",
  val date: String = "",
  val privateNote: String = "",
  val loggedBy: String = "",
  val migratedAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class IntimacyLog(
  @DocumentId val id: String = "",
  val spaceId: String = "",
  val date: String = "", // ISO YYYY-MM-DD
  val encounterType: String = EncounterType.PROTECTED.name,
  val usedLubricant: Boolean = false,
  val hadOrgasm: Boolean = false,
  val postContactEffect: String = PostContactEffect.NONE.name,
  val notes: String? = null,
  @get:PropertyName("isPrivate") val isPrivate: Boolean = false,
  val loggedBy: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PartnerObservation(
  @DocumentId val id: String = "",
  val spaceId: String = "",
  val date: String = "", // ISO YYYY-MM-DD
  val perceivedMood: String? = null,
  val apparentPainLevel: Int? = null,
  val unloggedSpottingReported: Boolean = false,
  val spottingSuggestionStatus: String = SpottingSuggestionStatus.PENDING.name,
  val careActionTaken: String? = null,
  val sweetMessage: String? = null,
  val loggedBy: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CheckIn(
  @DocumentId val id: String = "",
  val spaceId: String = "",
  val senderUid: String = "",
  val promptType: String = CheckInPromptType.HOW_ARE_YOU_FEELING.name,
  val customNote: String? = null,
  val status: String = CheckInStatus.PENDING.name,
  val responseOption: String? = null,
  val responseNote: String? = null,
  @ServerTimestamp val answeredAt: Timestamp? = null,
  @ServerTimestamp val createdAt: Timestamp? = null
)

data class CareTip(
  val title: String,
  val description: String,
  val categoryIcon: String // e.g. "tea", "massage", "heart", "food"
)

data class FertileWindow(
  val startDate: LocalDate,
  val endDate: LocalDate,
  val ovulationDate: LocalDate
)

data class CycleStatusData(
  val currentDay: Int,
  val totalDays: Int,
  val currentPhase: CyclePhase,
  val daysUntilNextPeriod: Int,
  val fertileWindow: FertileWindow,
  val nextPeriodDate: LocalDate,
  val confidenceIntervalDays: Double? = null,
  val isDelayed: Boolean = daysUntilNextPeriod < 0,
  val delayedDays: Int = if (daysUntilNextPeriod < 0) -daysUntilNextPeriod else 0,
  val isBasedOnHistory: Boolean = true,
  val historyCyclesUsed: Int = 0,
  val estimationNote: String? = null
)

fun DailySymptom.isConfirmedBleeding(): Boolean = flowLevel in listOf("LIGHT", "MEDIUM", "HEAVY")

data class PredictedCycle(
  val cycleNumber: Int,
  val expectedStartDate: LocalDate,
  val expectedEndDate: LocalDate,
  val expectedOvulationDate: LocalDate,
  val fertileWindowStart: LocalDate,
  val fertileWindowEnd: LocalDate,
  val confidenceDays: Double
)

enum class DayHighlightType {
  NONE,
  ACTUAL_PERIOD,
  PREDICTED_PERIOD,
  OVULATION_DAY,
  FERTILE_WINDOW,
  FOLLICULAR,
  LUTEAL
}

data class DayCycleDetail(
  val date: LocalDate,
  val cycleDay: Int?,
  val highlightType: DayHighlightType,
  val phaseName: String,
  val hormoneInsight: String,
  val hasSymptomLogged: Boolean = false,
  val hasIntimacyLogged: Boolean = false,
  val isPredicted: Boolean = false,
  val isPeriodConfirmed: Boolean = false
)
