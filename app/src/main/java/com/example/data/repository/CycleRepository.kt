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
import java.time.LocalDate
import java.time.temporal.ChronoUnit
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
  protected val db: FirebaseFirestore = FirestoreProvider.getDb(context),
  internal var onAfterReadInSpottingTransactionHook: ((date: String, attempt: Int) -> Unit)? = null
) {

  internal var onBeforeRecordPeriodStartHook: (suspend (startDate: String) -> Unit)? = null
  internal var onBeforeUpdateCycleDatesHook: (suspend (spaceId: String, cycleId: String, newStartDate: String) -> Unit)? = null

  /**
   * Deriva de forma canónica y consistente las duraciones de ciclo (cycleLength)
   * a partir de la secuencia cronológica de inicios (startDate).
   * El ciclo más reciente siempre queda incompleto (cycleLength = null).
   */
  fun deriveCycleLengths(cycles: List<CycleEntry>): List<CycleEntry> {
    val sorted = cycles.sortedWith(compareBy({ it.startDate }, { it.id }))
    return sorted.mapIndexed { index, entry ->
      val derivedLen = if (index < sorted.size - 1) {
        val nextStart = LocalDate.parse(sorted[index + 1].startDate)
        val thisStart = LocalDate.parse(entry.startDate)
        ChronoUnit.DAYS.between(thisStart, nextStart).toInt()
      } else {
        null
      }
      entry.copy(cycleLength = derivedLen)
    }
  }

  fun observeCycles(spaceId: String): Flow<List<CycleEntry>> {
    return db.collection("couple_spaces").document(spaceId)
      .collection("cycles")
      .snapshots()
      .map { snapshot ->
        val raw = snapshot.toObjects(CycleEntry::class.java)
        deriveCycleLengths(raw).sortedByDescending { it.startDate }
      }
  }

  open suspend fun addCycleEntry(spaceId: String, startDate: String, periodDuration: Int = 5) {
    recordPeriodStart(spaceId, startDate, null, periodDuration)
  }

  /**
   * Registra un inicio menstrual real con unicidad canónica y protección transaccional.
   * - Previene duplicados del mismo inicio mediante clave determinista y verificación transaccional.
   * - Si se especifica endDate, conserva la duración real completa sin recortes artificiales.
   * - Recalcula atómicamente los intervalos de ciclo (cycleLength) entre inicios consecutivos.
   */
  open suspend fun recordPeriodStart(
    spaceId: String,
    startDate: String,
    endDate: String? = null,
    periodDuration: Int? = null
  ): Result<String> {
    return try {
      val parsedStart = LocalDate.parse(startDate)
      val parsedEnd = endDate?.let { LocalDate.parse(it) }

      if (parsedEnd != null && parsedEnd.isBefore(parsedStart)) {
        return Result.failure(IllegalArgumentException("La fecha de fin no puede ser anterior a la de inicio"))
      }

      // Prevenir duplicados del mismo inicio consultando la colección (incluye documentos legacy con ID aleatorio)
      val existingWithSameStart = db.collection("couple_spaces").document(spaceId)
        .collection("cycles")
        .whereEqualTo("startDate", startDate)
        .get()
        .await()

      if (!existingWithSameStart.isEmpty) {
        return Result.failure(IllegalArgumentException("Ya existe un ciclo registrado con inicio en $startDate"))
      }

      val actualDuration = if (parsedEnd != null) {
        (ChronoUnit.DAYS.between(parsedStart, parsedEnd) + 1).toInt()
      } else {
        periodDuration ?: 5
      }
      val isCalculated = parsedEnd == null

      // Hook de concurrencia para pruebas controladas
      onBeforeRecordPeriodStartHook?.invoke(startDate)

      val docRef = db.collection("couple_spaces").document(spaceId)
        .collection("cycles").document(startDate)

      db.runTransaction { transaction ->
        val snap = transaction.get(docRef)
        if (snap.exists()) {
          throw IllegalArgumentException("Ya existe un ciclo registrado con inicio en $startDate")
        }

        val payload = mapOf(
          "spaceId" to spaceId,
          "startDate" to startDate,
          "endDate" to endDate,
          "cycleLength" to null,
          "periodDuration" to actualDuration,
          "isCalculated" to isCalculated,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
        transaction.set(docRef, payload)
      }.await()

      Result.success(docRef.id)
    } catch (e: Exception) {
      val unwrapped = if (e is com.google.firebase.firestore.FirebaseFirestoreException && e.cause is IllegalArgumentException) {
        e.cause as IllegalArgumentException
      } else e
      Result.failure(unwrapped)
    }
  }

  open suspend fun recordPeriodStart(
    spaceId: String,
    startDate: LocalDate,
    endDate: LocalDate? = null,
    periodDuration: Int? = null
  ): Result<String> = recordPeriodStart(spaceId, startDate.toString(), endDate?.toString(), periodDuration)

  /**
   * Registra o corrige la fecha de fin de sangrado menstrual confirmado por la usuaria.
   * Conserva la duración real sin acotación arbitraria.
   */
  open suspend fun updatePeriodEnd(
    spaceId: String,
    cycleId: String,
    endDate: String
  ): Result<Unit> {
    return try {
      val docRef = db.collection("couple_spaces").document(spaceId)
        .collection("cycles").document(cycleId)

      db.runTransaction { transaction ->
        val snap = transaction.get(docRef)
        if (!snap.exists()) {
          throw IllegalArgumentException("El ciclo no existe")
        }
        val startDate = snap.getString("startDate")
          ?: throw IllegalStateException("El ciclo no tiene fecha de inicio")
        val parsedStart = LocalDate.parse(startDate)
        val parsedEnd = LocalDate.parse(endDate)

        if (parsedEnd.isBefore(parsedStart)) {
          throw IllegalArgumentException("La fecha de fin no puede ser anterior al inicio")
        }

        val duration = (ChronoUnit.DAYS.between(parsedStart, parsedEnd) + 1).toInt()
        transaction.update(
          docRef,
          mapOf(
            "endDate" to endDate,
            "periodDuration" to duration,
            "isCalculated" to false,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
      }.await()
      Result.success(Unit)
    } catch (e: Exception) {
      val unwrapped = if (e is com.google.firebase.firestore.FirebaseFirestoreException && e.cause is IllegalArgumentException) {
        e.cause as IllegalArgumentException
      } else e
      Result.failure(unwrapped)
    }
  }

  /**
   * Corrige las fechas de un registro de ciclo existente y sincroniza las duraciones de ciclos vecinos.
   * Evita colisiones de fecha y mantiene la consistencia atómica.
   */
  open suspend fun updateCycleDates(
    spaceId: String,
    cycleId: String,
    newStartDate: String,
    newEndDate: String?
  ): Result<Unit> {
    return try {
      val parsedStart = LocalDate.parse(newStartDate)
      val parsedEnd = newEndDate?.let { LocalDate.parse(it) }

      if (parsedEnd != null && parsedEnd.isBefore(parsedStart)) {
        return Result.failure(IllegalArgumentException("La fecha de fin no puede ser anterior al inicio"))
      }

      val conflicts = db.collection("couple_spaces").document(spaceId)
        .collection("cycles")
        .whereEqualTo("startDate", newStartDate)
        .get()
        .await()

      if (conflicts.documents.any { it.id != cycleId }) {
        return Result.failure(IllegalArgumentException("Ya existe otro ciclo con inicio en $newStartDate"))
      }

      val actualDuration = if (parsedEnd != null) {
        (ChronoUnit.DAYS.between(parsedStart, parsedEnd) + 1).toInt()
      } else {
        5
      }

      val oldDocRef = db.collection("couple_spaces").document(spaceId)
        .collection("cycles").document(cycleId)

      // Hook de concurrencia para pruebas controladas
      onBeforeUpdateCycleDatesHook?.invoke(spaceId, cycleId, newStartDate)

      db.runTransaction { transaction ->
        val oldSnap = transaction.get(oldDocRef)
        if (!oldSnap.exists()) {
          throw IllegalArgumentException("El ciclo a modificar no existe")
        }

        // Siempre garantizamos la unicidad canónica utilizando newStartDate como clave del documento.
        // Si cycleId != newStartDate (incluye documentos legacy con ID aleatorio que se migran de forma atómica),
        // verificamos si newStartDate ya está ocupado dentro de la transacción OCC.
        if (cycleId != newStartDate) {
          val newDocRef = db.collection("couple_spaces").document(spaceId)
            .collection("cycles").document(newStartDate)
          val targetSnap = transaction.get(newDocRef)
          if (targetSnap.exists() && targetSnap.id != cycleId) {
            throw IllegalArgumentException("Ya existe otro ciclo con inicio en $newStartDate")
          }

          val payload = mapOf(
            "spaceId" to spaceId,
            "startDate" to newStartDate,
            "endDate" to newEndDate,
            "cycleLength" to null,
            "periodDuration" to actualDuration,
            "isCalculated" to (parsedEnd == null),
            "createdAt" to (oldSnap.get("createdAt") ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
          )
          transaction.delete(oldDocRef)
          transaction.set(newDocRef, payload)
        } else {
          // cycleId coincide exactamente con newStartDate (solo se actualiza endDate o duración)
          transaction.update(
            oldDocRef,
            mapOf(
              "endDate" to newEndDate,
              "periodDuration" to actualDuration,
              "isCalculated" to (parsedEnd == null),
              "updatedAt" to FieldValue.serverTimestamp()
            )
          )
        }
      }.await()

      Result.success(Unit)
    } catch (e: Exception) {
      val unwrapped = if (e is com.google.firebase.firestore.FirebaseFirestoreException && e.cause is IllegalArgumentException) {
        e.cause as IllegalArgumentException
      } else e
      Result.failure(unwrapped)
    }
  }

  open suspend fun updatePeriodEnd(
    spaceId: String,
    cycleId: String,
    endDate: LocalDate
  ): Result<Unit> = updatePeriodEnd(spaceId, cycleId, endDate.toString())

  open suspend fun updateCycleDates(
    spaceId: String,
    cycleId: String,
    newStartDate: LocalDate,
    newEndDate: LocalDate? = null
  ): Result<Unit> = updateCycleDates(spaceId, cycleId, newStartDate.toString(), newEndDate?.toString())

  /**
   * Elimina un registro de inicio de ciclo.
   * La secuencia de duraciones entre ciclos restantes se calcula de forma derivada y canónica al observar.
   */
  open suspend fun deleteCycleEntry(spaceId: String, cycleId: String): Result<Unit> {
    return try {
      db.collection("couple_spaces").document(spaceId)
        .collection("cycles").document(cycleId)
        .delete()
        .await()

      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
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
            mood = mood?.mood.orEmpty(),
            energyLevel = mood?.energyLevel ?: 0,
            cervicalMucus = phys?.cervicalMucus ?: "NONE",
            tenderBreasts = phys?.tenderBreasts ?: false,
            cravings = phys?.cravings ?: false,
            acne = phys?.acne ?: false,
            publicNote = phys?.publicNote,
            privateNote = note?.privateNote,
            isPrivate = phys?.isPrivate ?: (mood?.isPrivate ?: false),
            isMoodPrivate = mood?.isPrivate,
            loggedBy = phys?.loggedBy ?: (mood?.loggedBy ?: ""),
            updatedAt = phys?.updatedAt ?: mood?.updatedAt,
            hasPhysicalRecord = phys != null,
            hasMoodRecord = mood != null
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
            updatedAt = phys?.updatedAt ?: mood?.updatedAt,
            hasPhysicalRecord = phys != null,
            hasMoodRecord = mood != null
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
      "isPrivate" to (symptom.isMoodPrivate ?: symptom.isPrivate),
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

    var transactionAttempt = 0
    return try {
      db.runTransaction { transaction ->
        transactionAttempt++
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
        onAfterReadInSpottingTransactionHook?.invoke(date, transactionAttempt)
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
