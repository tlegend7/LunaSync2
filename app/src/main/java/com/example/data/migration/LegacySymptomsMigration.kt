package com.example.data.migration

import com.example.data.model.FlowLevel
import com.example.data.model.Mood
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Migration outcome reporting structured counters:
 * - totalLegacyRead: Total number of examined legacy documents.
 * - categoriesCreated: Total new subdocuments created in target collections (physical, mood, private_notes).
 * - categoriesSkippedExisting: Total categories omitted because the target document already existed.
 * - categoriesSimulated: Total categories that would be created during a dry-run simulation.
 * - physicalMigrated, physicalSkipped: Counters for physical_symptoms.
 * - moodMigrated, moodSkipped: Counters for mood_records.
 * - notesMigrated, notesSkipped: Counters for private_notes.
 * - errorsCount: Total failed transactions or unhandled exceptions.
 * - errorDetails: Anonymized list of error messages (no private/intimate notes included).
 */
data class MigrationResult(
  val totalLegacyRead: Int,
  val categoriesCreated: Int,
  val categoriesSkippedExisting: Int,
  val categoriesSimulated: Int,
  val physicalMigrated: Int,
  val physicalSkipped: Int,
  val moodMigrated: Int,
  val moodSkipped: Int,
  val notesMigrated: Int,
  val notesSkipped: Int,
  val errorsCount: Int,
  val errorDetails: List<String> = emptyList()
)

private data class CategoryOutcome(
  val physicalCreated: Boolean = false,
  val physicalSkipped: Boolean = false,
  val moodCreated: Boolean = false,
  val moodSkipped: Boolean = false,
  val noteCreated: Boolean = false,
  val noteSkipped: Boolean = false
)

/**
 * Safe, idempotent, category-independent migration from legacy
 * `/couple_spaces/{spaceId}/symptoms` to:
 * - `/physical_symptoms/{date}`
 * - `/mood_records/{date}`
 * - `/private_notes/{date}`
 *
 * Guarantees:
 * 1. Category-independent evaluation: existing physical symptoms do NOT block missing mood or private notes.
 * 2. Strict non-overwrite: existing target documents are NEVER replaced or truncated.
 * 3. Atomic per-record write inside transactions or precondition checks.
 * 4. Counters are NEVER modified inside the transactional lambda; outcomes are returned and accumulated ONLY after commit confirmation.
 * 5. Privacy preservation: private notes are kept strictly inside `/private_notes`.
 * 6. Dry-run simulation mode delivers exact categorization counters without mutating database.
 * 7. Anonymized error logging without printing intimate notes.
 */
class LegacySymptomsMigration(private val db: FirebaseFirestore) {

  suspend fun migrateSpaceSymptoms(spaceId: String, dryRun: Boolean = true): MigrationResult {
    val legacyCollection = db.collection("couple_spaces").document(spaceId).collection("symptoms")
    val legacyDocs = try {
      legacyCollection.get().await().documents
    } catch (e: Exception) {
      return MigrationResult(
        totalLegacyRead = 0,
        categoriesCreated = 0,
        categoriesSkippedExisting = 0,
        categoriesSimulated = 0,
        physicalMigrated = 0,
        physicalSkipped = 0,
        moodMigrated = 0,
        moodSkipped = 0,
        notesMigrated = 0,
        notesSkipped = 0,
        errorsCount = 1,
        errorDetails = listOf("Error al leer síntomas legacy: ${e.message}")
      )
    }

    var physicalMigrated = 0
    var physicalSkipped = 0
    var moodMigrated = 0
    var moodSkipped = 0
    var notesMigrated = 0
    var notesSkipped = 0
    var errorsCount = 0
    val errorDetails = mutableListOf<String>()

    val spaceDoc = db.collection("couple_spaces").document(spaceId)

    for (doc in legacyDocs) {
      val date = doc.getString("date") ?: doc.id
      try {
        val flowLevel = doc.getString("flowLevel") ?: FlowLevel.NONE.name
        val painLevel = doc.getLong("painLevel")?.toInt() ?: 0
        val cramps = doc.getBoolean("cramps") ?: false
        val headache = doc.getBoolean("headache") ?: false
        val backPain = doc.getBoolean("backPain") ?: false
        val bloating = doc.getBoolean("bloating") ?: false
        val mucus = doc.getString("cervicalMucus") ?: "NONE"
        val tenderBreasts = doc.getBoolean("tenderBreasts") ?: false
        val cravings = doc.getBoolean("cravings") ?: false
        val acne = doc.getBoolean("acne") ?: false
        val publicNote = doc.getString("publicNote")
        val privateNote = doc.getString("privateNote")
        val mood = doc.getString("mood") ?: Mood.CALM.name
        val energyLevel = doc.getLong("energyLevel")?.toInt() ?: 3
        val isPrivate = doc.getBoolean("isPrivate") ?: true // Conservative default
        val loggedBy = doc.getString("loggedBy") ?: ""

        val physRef = spaceDoc.collection("physical_symptoms").document(date)
        val moodRef = spaceDoc.collection("mood_records").document(date)
        val noteRef = spaceDoc.collection("private_notes").document(date)

        if (dryRun) {
          // Check existence individually per category without mutating any document
          val physExists = physRef.get().await().exists()
          if (physExists) physicalSkipped++ else physicalMigrated++

          val moodExists = moodRef.get().await().exists()
          if (moodExists) moodSkipped++ else moodMigrated++

          if (!privateNote.isNullOrBlank()) {
            val noteExists = noteRef.get().await().exists()
            if (noteExists) notesSkipped++ else notesMigrated++
          }
        } else {
          // Real transaction: DO NOT mutate outer counters inside lambda.
          // Lambda returns CategoryOutcome which is accumulated ONLY after await() succeeds.
          val outcome = db.runTransaction { transaction ->
            val physSnap = transaction.get(physRef)
            val moodSnap = transaction.get(moodRef)
            val noteSnap = transaction.get(noteRef)

            var physCreated = false
            var physSkipped = false
            var moodCreated = false
            var moodSkipped = false
            var noteCreated = false
            var noteSkipped = false

            // Category 1: Physical symptoms
            if (!physSnap.exists()) {
              val physPayload = mapOf(
                "spaceId" to spaceId,
                "date" to date,
                "flowLevel" to flowLevel,
                "painLevel" to painLevel,
                "cramps" to cramps,
                "headache" to headache,
                "backPain" to backPain,
                "bloating" to bloating,
                "cervicalMucus" to mucus,
                "tenderBreasts" to tenderBreasts,
                "cravings" to cravings,
                "acne" to acne,
                "publicNote" to publicNote,
                "isPrivate" to isPrivate,
                "loggedBy" to loggedBy,
                "migratedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
              )
              transaction.set(physRef, physPayload)
              physCreated = true
            } else {
              physSkipped = true
            }

            // Category 2: Mood and energy
            if (!moodSnap.exists()) {
              val moodPayload = mapOf(
                "spaceId" to spaceId,
                "date" to date,
                "mood" to mood,
                "energyLevel" to energyLevel,
                "isPrivate" to isPrivate,
                "loggedBy" to loggedBy,
                "migratedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
              )
              transaction.set(moodRef, moodPayload)
              moodCreated = true
            } else {
              moodSkipped = true
            }

            // Category 3: Private note (strictly isolated)
            if (!privateNote.isNullOrBlank()) {
              if (!noteSnap.exists()) {
                val notePayload = mapOf(
                  "spaceId" to spaceId,
                  "date" to date,
                  "privateNote" to privateNote,
                  "loggedBy" to loggedBy,
                  "migratedAt" to FieldValue.serverTimestamp(),
                  "updatedAt" to FieldValue.serverTimestamp()
                )
                transaction.set(noteRef, notePayload)
                noteCreated = true
              } else {
                noteSkipped = true
              }
            }

            CategoryOutcome(
              physicalCreated = physCreated,
              physicalSkipped = physSkipped,
              moodCreated = moodCreated,
              moodSkipped = moodSkipped,
              noteCreated = noteCreated,
              noteSkipped = noteSkipped
            )
          }.await()

          // Transaction confirmed by server: accumulate verified outcome
          if (outcome.physicalCreated) physicalMigrated++
          if (outcome.physicalSkipped) physicalSkipped++
          if (outcome.moodCreated) moodMigrated++
          if (outcome.moodSkipped) moodSkipped++
          if (outcome.noteCreated) notesMigrated++
          if (outcome.noteSkipped) notesSkipped++
        }
      } catch (e: Exception) {
        errorsCount++
        errorDetails.add("Error migrando fecha $date: ${e.message}")
      }
    }

    val totalCreated = if (dryRun) 0 else (physicalMigrated + moodMigrated + notesMigrated)
    val totalSkipped = physicalSkipped + moodSkipped + notesSkipped
    val totalSimulated = if (dryRun) (physicalMigrated + moodMigrated + notesMigrated) else 0

    return MigrationResult(
      totalLegacyRead = legacyDocs.size,
      categoriesCreated = totalCreated,
      categoriesSkippedExisting = totalSkipped,
      categoriesSimulated = totalSimulated,
      physicalMigrated = physicalMigrated,
      physicalSkipped = physicalSkipped,
      moodMigrated = moodMigrated,
      moodSkipped = moodSkipped,
      notesMigrated = notesMigrated,
      notesSkipped = notesSkipped,
      errorsCount = errorsCount,
      errorDetails = errorDetails
    )
  }
}
