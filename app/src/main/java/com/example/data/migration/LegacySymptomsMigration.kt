package com.example.data.migration

import com.example.data.model.FlowLevel
import com.example.data.model.Mood
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class MigrationResult(
  val totalLegacyRead: Int,
  val physicalMigrated: Int,
  val physicalSkipped: Int,
  val moodMigrated: Int,
  val moodSkipped: Int,
  val notesMigrated: Int,
  val notesSkipped: Int,
  val errorsCount: Int,
  val errorDetails: List<String> = emptyList()
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
 * 4. Privacy preservation: private notes are kept strictly inside `/private_notes`.
 * 5. Dry-run simulation mode delivers exact categorization counters without mutating database.
 * 6. Anonymized error logging without printing intimate notes.
 */
class LegacySymptomsMigration(private val db: FirebaseFirestore) {

  suspend fun migrateSpaceSymptoms(spaceId: String, dryRun: Boolean = true): MigrationResult {
    val legacyCollection = db.collection("couple_spaces").document(spaceId).collection("symptoms")
    val legacyDocs = legacyCollection.get().await().documents

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
          // Check existence individually per category
          val physExists = physRef.get().await().exists()
          if (physExists) physicalSkipped++ else physicalMigrated++

          val moodExists = moodRef.get().await().exists()
          if (moodExists) moodSkipped++ else moodMigrated++

          if (!privateNote.isNullOrBlank()) {
            val noteExists = noteRef.get().await().exists()
            if (noteExists) notesSkipped++ else notesMigrated++
          }
        } else {
          // Real transaction ensuring no concurrent overwrite and category independence
          db.runTransaction { transaction ->
            val physSnap = transaction.get(physRef)
            val moodSnap = transaction.get(moodRef)
            val noteSnap = transaction.get(noteRef)

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
              physicalMigrated++
            } else {
              physicalSkipped++
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
              moodMigrated++
            } else {
              moodSkipped++
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
                notesMigrated++
              } else {
                notesSkipped++
              }
            }
          }.await()
        }
      } catch (e: Exception) {
        errorsCount++
        errorDetails.add("Error migrando fecha $date: ${e.message}")
      }
    }

    return MigrationResult(
      totalLegacyRead = legacyDocs.size,
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
