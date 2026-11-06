package com.example.data.repository

import android.content.Context
import com.example.data.model.EncounterType
import com.example.data.model.IntimacyLog
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class IntimacyRepository(context: Context) {
  private val db: FirebaseFirestore = FirestoreProvider.getDb(context)

  /**
   * Observes intimacy logs ensuring:
   * - Partner cannot download or query any logs if shareIntimacy is false.
   * - Partner query explicitly filters on the server with whereEqualTo("isPrivate", false)
   *   to comply with zero-trust Firestore security rules.
   */
  fun observeIntimacyLogs(
    spaceId: String,
    isElla: Boolean,
    shareIntimacyAllowed: Boolean
  ): Flow<List<IntimacyLog>> {
    val collectionRef = db.collection("couple_spaces").document(spaceId)
      .collection("intimacy_logs")

    if (!isElla && !shareIntimacyAllowed) {
      return flowOf(emptyList())
    }

    val query = if (isElla) {
      collectionRef.orderBy("date", Query.Direction.DESCENDING)
    } else {
      collectionRef.whereEqualTo("isPrivate", false)
    }

    return query.snapshots().map { snapshot ->
      val list = snapshot.toObjects(IntimacyLog::class.java)
      list.sortedByDescending { it.date }
    }
  }

  suspend fun saveIntimacyLog(
    spaceId: String,
    date: String,
    encounterType: EncounterType,
    usedLubricant: Boolean = false,
    hadOrgasm: Boolean = false,
    postContactEffect: com.example.data.model.PostContactEffect = com.example.data.model.PostContactEffect.NONE,
    notes: String?,
    isPrivate: Boolean,
    userId: String
  ) {
    val docRef = db.collection("couple_spaces").document(spaceId)
      .collection("intimacy_logs").document()

    val payload = mapOf(
      "spaceId" to spaceId,
      "date" to date,
      "encounterType" to encounterType.name,
      "usedLubricant" to usedLubricant,
      "hadOrgasm" to hadOrgasm,
      "postContactEffect" to postContactEffect.name,
      "notes" to notes,
      "isPrivate" to isPrivate,
      "loggedBy" to userId,
      "createdAt" to FieldValue.serverTimestamp()
    )
    docRef.set(payload).await()
  }

  suspend fun deleteIntimacyLog(spaceId: String, logId: String) {
    db.collection("couple_spaces").document(spaceId)
      .collection("intimacy_logs").document(logId)
      .delete().await()
  }
}
