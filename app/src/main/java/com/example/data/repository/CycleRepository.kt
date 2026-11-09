package com.example.data.repository

import android.content.Context
import com.example.data.model.CycleEntry
import com.example.data.model.DailySymptom
import com.example.data.model.FlowLevel
import com.example.data.model.Mood
import com.example.data.model.MoodRecord
import com.example.data.model.PhysicalSymptomsRecord
import com.example.data.model.PrivateDailyNote
import com.example.data.model.SpottingSuggestionStatus
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

data class SharedSymptomState(
  val hasPhysicalRecorded: Boolean = false,
  val hasMoodRecorded: Boolean = false,
  val flowLevel: String = FlowLevel.NONE.name,
  val painLevel: Int = 0,
  val cramps: Boolean = false,
  val headache: Boolean = false,
  val backPain: Boolean = false,
  val bloating: Boolean = false,
  val cervicalMucus: String = "NONE",
  val tenderBreasts: Boolean = false,
  val cravings: Boolean = false,
  val acne: Boolean = false,
  val mood: String? = null,
  val energyLevel: Int? = null,
  val publicNote: String? = null,
  val isPrivate: Boolean = false,
  val date: String = ""
)

open class CycleRepository(
  context: Context,
  protected val db: FirebaseFirestore = FirestoreProvider.getDb(context)
) {

  fun observeCycles(spaceId: String): Flow<List<CycleEntry>> {
    return db.collection("couple_spaces").document(spaceId)
      .collection("cycles")
      .orderBy("startDate", Query.Direction.DESCENDING)
      .snapshots()
      .map { snapshot -> snapshot.toObjects(CycleEntry::class.java) }
  }

  suspend fun addCycleEntry(spaceId: String, startDate: String, periodDuration: Int = 5) {
    val docRef = db.collection("couple_spaces").document(spaceId)
      .collection("cycles").document()

    val payload = mapOf(
      "spaceId" to spaceId,
      "startDate" to startDate,
      "endDate" to null,
      "cycleLength" to null,
      "periodDuration" to periodDuration,
      "isCalculated" to false,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    docRef.set(payload).await()
  }

  /**
   * Observes symptoms respecting zero-trust privacy boundaries:
   * - Ella reads physical_symptoms, mood_records, and private_notes (all merged).
   * - Partner can ONLY read physical_symptoms if shareSymptoms == true, and mood_records if shareMood == true.
   * - Partner queries strictly include whereEqualTo("isPrivate", false).
   * - Partner NEVER reads private_notes.
   */
  fun observeDailySymptoms(
    spaceId: String,
    isElla: Boolean = true,
    shareSymptoms: Boolean = true,
    shareMood: Boolean = true
  ): Flow<List<DailySymptom>> {
    val spaceDoc = db.collection("couple_spaces").document(spaceId)

    if (!isElla && !shareSymptoms && !shareMood) {
      return flowOf(emptyList())
    }

    if (isElla) {
      val physicalFlow = spaceDoc.collection("physical_symptoms")
        .snapshots()
        .map { it.toObjects(PhysicalSymptomsRecord::class.java) }

      val moodFlow = spaceDoc.collection("mood_records")
        .snapshots()
        .map { it.toObjects(MoodRecord::class.java) }

      val notesFlow = spaceDoc.collection("private_notes")
        .snapshots()
        .map { it.toObjects(PrivateDailyNote::class.java) }

      return combine(physicalFlow, moodFlow, notesFlow) { physicalList, moodList, notesList ->
        val moodMap = moodList.associateBy { it.date }
        val notesMap = notesList.associateBy { it.date }
        val allDates = (physicalList.map { it.date } + moodList.map { it.date } + notesList.map { it.date }).toSet()

        allDates.sortedDescending().map { date ->
          val phys = physicalList.firstOrNull { it.date == date }
          val mood = moodMap[date]
          val note = notesMap[date]

          DailySymptom(
            id = date,
            spaceId = spaceId,
            date = date,
            flowLevel = phys?.flowLevel ?: FlowLevel.NONE.name,
            painLevel = phys?.painLevel ?: 0,
            cramps = phys?.cramps ?: false,
            headache = phys?.headache ?: false,
            backPain = phys?.backPain ?: false,
            bloating = phys?.bloating ?: false,
            mood = mood?.mood ?: Mood.CALM.name,
            energyLevel = mood?.energyLevel ?: 3,
            cervicalMucus = phys?.cervicalMucus ?: "NONE",
            tenderBreasts = phys?.tenderBreasts ?: false,
            cravings = phys?.cravings ?: false,
            acne = phys?.acne ?: false,
            publicNote = phys?.publicNote,
            privateNote = note?.privateNote,
            isPrivate = phys?.isPrivate ?: (mood?.isPrivate ?: false),
            loggedBy = phys?.loggedBy ?: (mood?.loggedBy ?: ""),
            updatedAt = phys?.updatedAt ?: mood?.updatedAt
          )
        }
      }
    } else {
      // Partner Flow: strictly non-private and gated by individual sharing permissions
      val physicalFlow: Flow<List<PhysicalSymptomsRecord>> = if (shareSymptoms) {
        spaceDoc.collection("physical_symptoms")
          .whereEqualTo("isPrivate", false)
          .snapshots()
          .map { it.toObjects(PhysicalSymptomsRecord::class.java) }
      } else {
        flowOf(emptyList())
      }

      val moodFlow: Flow<List<MoodRecord>> = if (shareMood) {
        spaceDoc.collection("mood_records")
          .whereEqualTo("isPrivate", false)
          .snapshots()
          .map { it.toObjects(MoodRecord::class.java) }
      } else {
        flowOf(emptyList())
      }

      return combine(physicalFlow, moodFlow) { physicalList, moodList ->
        val moodMap = moodList.associateBy { it.date }
        val allDates = (physicalList.map { it.date } + moodList.map { it.date }).toSet()

        allDates.sortedDescending().map { date ->
          val phys = physicalList.firstOrNull { it.date == date }
          val mood = moodMap[date]

          DailySymptom(
            id = date,
            spaceId = spaceId,
            date = date,
            flowLevel = if (shareSymptoms && phys != null) phys.flowLevel else FlowLevel.NONE.name,
            painLevel = if (shareSymptoms && phys != null) phys.painLevel else 0,
            cramps = if (shareSymptoms && phys != null) phys.cramps else false,
            headache = if (shareSymptoms && phys != null) phys.headache else false,
            backPain = if (shareSymptoms && phys != null) phys.backPain else false,
            bloating = if (shareSymptoms && phys != null) phys.bloating else false,
            // Keep null or domain sentinel distinction so UI knows whether mood was actually reported
            mood = if (shareMood && mood != null) mood.mood else "",
            energyLevel = if (shareMood && mood != null) mood.energyLevel else 0,
            cervicalMucus = if (shareSymptoms && phys != null) phys.cervicalMucus else "NONE",
            tenderBreasts = if (shareSymptoms && phys != null) phys.tenderBreasts else false,
            cravings = if (shareSymptoms && phys != null) phys.cravings else false,
            acne = if (shareSymptoms && phys != null) phys.acne else false,
            publicNote = if (shareSymptoms && phys != null) phys.publicNote else null,
            privateNote = null, // PARTNER NEVER SEES PRIVATE NOTE UNDER ANY CIRCUMSTANCES
            isPrivate = false,
            loggedBy = phys?.loggedBy ?: (mood?.loggedBy ?: ""),
            updatedAt = phys?.updatedAt ?: mood?.updatedAt
          )
        }
      }
    }
  }

  /**
   * Atomic batch write ensuring physical symptoms, mood, and private notes are decoupled
   * into their respective protected subcollections.
   */
  open suspend fun saveDailySymptom(symptom: DailySymptom) {
    val batch = db.batch()
    val spaceDoc = db.collection("couple_spaces").document(symptom.spaceId)

    // 1. Physical symptoms document (NO privateNote)
    val physRef = spaceDoc.collection("physical_symptoms").document(symptom.date)
    val physPayload = mapOf(
      "spaceId" to symptom.spaceId,
      "date" to symptom.date,
      "flowLevel" to symptom.flowLevel,
      "painLevel" to symptom.painLevel,
      "cramps" to symptom.cramps,
      "headache" to symptom.headache,
      "backPain" to symptom.backPain,
      "bloating" to symptom.bloating,
      "cervicalMucus" to symptom.cervicalMucus,
      "tenderBreasts" to symptom.tenderBreasts,
      "cravings" to symptom.cravings,
      "acne" to symptom.acne,
      "publicNote" to symptom.publicNote,
      "isPrivate" to symptom.isPrivate,
      "loggedBy" to symptom.loggedBy,
      "updatedAt" to FieldValue.serverTimestamp()
    )
    batch.set(physRef, physPayload)

    // 2. Mood and energy document (NO privateNote)
    val moodRef = spaceDoc.collection("mood_records").document(symptom.date)
    val moodPayload = mapOf(
      "spaceId" to symptom.spaceId,
      "date" to symptom.date,
      "mood" to symptom.mood,
      "energyLevel" to symptom.energyLevel,
      "isPrivate" to symptom.isPrivate,
      "loggedBy" to symptom.loggedBy,
      "updatedAt" to FieldValue.serverTimestamp()
    )
    batch.set(moodRef, moodPayload)

    // 3. Private notes document (Accessible ONLY by owner)
    val noteRef = spaceDoc.collection("private_notes").document(symptom.date)
    if (!symptom.privateNote.isNullOrBlank()) {
      val notePayload = mapOf(
        "spaceId" to symptom.spaceId,
        "date" to symptom.date,
        "privateNote" to symptom.privateNote,
        "loggedBy" to symptom.loggedBy,
        "updatedAt" to FieldValue.serverTimestamp()
      )
      batch.set(noteRef, notePayload)
    } else {
      batch.delete(noteRef)
    }

    batch.commit().await()
  }

  /**
   * Atomic acceptance of spotting suggestion:
   * - Verifies observation exists, is PENDING, and matches date & space
   * - Updates observation status to ACCEPTED
   * - Preserves isPrivate if document already exists
   * - If non-existent, creates a valid document with conservative privacy (isPrivate = true) and painLevel = 0
   * - Never downgrades higher bleeding (LIGHT, MEDIUM, HEAVY) without explicit consent
   */
  open suspend fun acceptSpottingSuggestion(spaceId: String, observationId: String, date: String, userId: String): Result<Unit> {
    val obsRef = db.collection("couple_spaces").document(spaceId)
      .collection("observations").document(observationId)
    val physRef = db.collection("couple_spaces").document(spaceId)
      .collection("physical_symptoms").document(date)

    return try {
      db.runTransaction { transaction ->
        val obsSnap = transaction.get(obsRef)
        if (!obsSnap.exists()) {
          throw IllegalArgumentException("La observación no existe")
        }

        val currentStatus = obsSnap.getString("spottingSuggestionStatus")
        if (currentStatus != SpottingSuggestionStatus.PENDING.name) {
          // Already accepted or dismissed, idempotent no-op
          return@runTransaction
        }

        val obsSpaceId = obsSnap.getString("spaceId")
        val obsDate = obsSnap.getString("date")
        if (obsSpaceId != spaceId || obsDate != date) {
          throw IllegalStateException("Discrepancia de espacio o fecha en la sugerencia")
        }

        val physSnap = transaction.get(physRef)
        val existingFlow = physSnap.getString("flowLevel")
        val existingIsPrivate = physSnap.getBoolean("isPrivate")
        val existingPainLevel = physSnap.getLong("painLevel")?.toInt() ?: 0

        // If existing flow is already heavier than SPOTTING (LIGHT, MEDIUM, HEAVY), don't downgrade
        val finalFlow = if (existingFlow in listOf("LIGHT", "MEDIUM", "HEAVY")) {
          existingFlow!!
        } else {
          FlowLevel.SPOTTING.name
        }

        // Preserve privacy of existing document; for brand new, use conservative default (true)
        val finalIsPrivate = existingIsPrivate ?: true

        // 1. Update observation
        transaction.update(obsRef, "spottingSuggestionStatus", SpottingSuggestionStatus.ACCEPTED.name)

        // 2. Update physical symptoms atomically
        if (physSnap.exists()) {
          transaction.update(
            physRef,
            mapOf(
              "flowLevel" to finalFlow,
              "isPrivate" to finalIsPrivate,
              "updatedAt" to FieldValue.serverTimestamp()
            )
          )
        } else {
          val newPayload = mapOf(
            "spaceId" to spaceId,
            "date" to date,
            "flowLevel" to finalFlow,
            "painLevel" to existingPainLevel,
            "cramps" to false,
            "headache" to false,
            "backPain" to false,
            "bloating" to false,
            "cervicalMucus" to "NONE",
            "tenderBreasts" to false,
            "cravings" to false,
            "acne" to false,
            "publicNote" to null,
            "isPrivate" to finalIsPrivate,
            "loggedBy" to userId,
            "updatedAt" to FieldValue.serverTimestamp()
          )
          transaction.set(physRef, newPayload)
        }
      }.await()

      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun dismissSpottingSuggestion(spaceId: String, observationId: String) {
    db.collection("couple_spaces").document(spaceId)
      .collection("observations").document(observationId)
      .update(
        "spottingSuggestionStatus", SpottingSuggestionStatus.DISMISSED.name
      ).await()
  }
}
