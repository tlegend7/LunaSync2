package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
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
  LUTEAL
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
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PairingInvite(
  @DocumentId val inviteCode: String = "",
  val spaceId: String = "",
  val createdBy: String = "",
  val status: String = "ACTIVE",
  val expiresAt: Timestamp? = null,
  @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CycleEntry(
  @DocumentId val id: String = "",
  val spaceId: String = "",
  val startDate: String = "", // ISO YYYY-MM-DD
  val endDate: String? = null,
  val cycleLength: Int? = null,
  val periodDuration: Int = 5,
  val isCalculated: Boolean = false,
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
  val isPrivate: Boolean = false,
  val loggedBy: String = "",
  @ServerTimestamp val updatedAt: Timestamp? = null
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
  val isPrivate: Boolean = false,
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
  val isPrivate: Boolean = false,
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
  val isPrivate: Boolean = false,
  val loggedBy: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PartnerObservation(
  @DocumentId val id: String = "",
  val spaceId: String = "",
  val date: String = "", // ISO YYYY-MM-DD
  val perceivedMood: String = PerceivedMood.CALM.name,
  val apparentPainLevel: Int = 0,
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
  val confidenceIntervalDays: Double
)

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
  val isPredicted: Boolean = false
)
