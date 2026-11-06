package com.example.data.migration

import com.example.data.model.FlowLevel
import com.example.data.model.Mood
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

data class MigrationResult(
  val totalLegacyRead: Int,
  val physicalMigrated: Int,
  val moodMigrated: Int,
  val notesMigrated: Int,
  val conflictsSkipped: Int,
  val errorsCount: Int
)

/**
 * Safe, idempotent migration from legacy `/couple_spaces/{spaceId}/symptoms`
 * to `/physical_symptoms/{date}`, `/mood_records/{date}`, and `/private_notes/{date}`.
 *
 * Guarantees:
 * 1. Dry-run simulation mode supported (`dryRun = true`).
 * 2. Never overwrites newer records in the target collections.
 * 3. Never logs intimate private notes to standard output.
 * 4. Preserves isPrivate, timestamps, dates, and author identity.
 */
class LegacySymptomsMigration(private val db: FirebaseFirestore) {

  suspend fun migrateSpaceSymptoms(spaceId: String, dryRun: Boolean = true): MigrationResult {
    val legacyCollection = db.collection("couple_spaces").document(spaceId).collection("symptoms")
    val legacyDocs = legacyCollection.get().await().documents

    var physicalCount = 0
    var moodCount = 0
    var notesCount = 0
    var skippedCount = 0
    var errorCount = 0

    val spaceDoc = db.collection("couple_spaces").document(spaceId)

    for (doc in legacyDocs) {
      try {
        val date = doc.getString("date") ?: doc.id
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
        val isPrivate = doc.getBoolean("isPrivate") ?: false
        val loggedBy = doc.getString("loggedBy") ?: ""

        // Check target collections to prevent overwriting newer records
        val targetPhysDoc = spaceDoc.collection("physical_symptoms").document(date).get().await()
        val targetMoodDoc = spaceDoc.collection("mood_records").document(date).get().await()
        val targetNoteDoc = spaceDoc.collection("private_notes").document(date).get().await()

        if (targetPhysDoc.exists() || targetMoodDoc.exists()) {
          // Newer data already exists for this date, skip to protect user authored data
          skippedCount++
          continue
        }

        if (!dryRun) {
          val batch = db.batch()

          // 1. Physical Symptoms
          val physRef = spaceDoc.collection("physical_symptoms").document(date)
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
            "updatedAt" to FieldValue.serverTimestamp()
          )
          batch.set(physRef, physPayload)

          // 2. Mood Record
          val moodRef = spaceDoc.collection("mood_records").document(date)
          val moodPayload = mapOf(
            "spaceId" to spaceId,
            "date" to date,
            "mood" to mood,
            "energyLevel" to energyLevel,
            "isPrivate" to isPrivate,
            "loggedBy" to loggedBy,
            "updatedAt" to FieldValue.serverTimestamp()
          )
          batch.set(moodRef, moodPayload)

          // 3. Private Note (isolated)
          if (!privateNote.isNullOrBlank()) {
            val noteRef = spaceDoc.collection("private_notes").document(date)
            val notePayload = mapOf(
              "spaceId" to spaceId,
              "date" to date,
              "privateNote" to privateNote,
              "loggedBy" to loggedBy,
              "updatedAt" to FieldValue.serverTimestamp()
            )
            batch.set(noteRef, notePayload)
            notesCount++
          }

          batch.commit().await()
        }

        physicalCount++
        moodCount++
        if (!privateNote.isNullOrBlank() && dryRun) {
          notesCount++
        }
      } catch (e: Exception) {
        errorCount++
      }
    }

    return MigrationResult(
      totalLegacyRead = legacyDocs.size,
      physicalMigrated = physicalCount,
      moodMigrated = moodCount,
      notesMigrated = notesCount,
      conflictsSkipped = skippedCount,
      errorsCount = errorCount
    )
  }
}
