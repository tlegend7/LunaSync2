package com.example.data.repository

import android.content.Context
import com.example.data.model.CheckIn
import com.example.data.model.CheckInPromptType
import com.example.data.model.CheckInStatus
import com.example.data.model.PartnerObservation
import com.example.data.model.PerceivedMood
import com.example.data.model.QuickResponseOption
import com.example.data.model.SpottingSuggestionStatus
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

open class PartnerSupportRepository(
  context: Context,
  private val db: FirebaseFirestore = FirestoreProvider.getDb(context)
) {

  open fun observeObservations(spaceId: String): Flow<List<PartnerObservation>> {
    return db.collection("couple_spaces").document(spaceId)
      .collection("observations")
      .orderBy("createdAt", Query.Direction.DESCENDING)
      .snapshots()
      .map { snapshot -> snapshot.toObjects(PartnerObservation::class.java) }
  }

  open suspend fun logObservation(
    spaceId: String,
    date: String,
    perceivedMood: PerceivedMood?,
    apparentPainLevel: Int?,
    unloggedSpottingReported: Boolean,
    careActionTaken: String?,
    sweetMessage: String?,
    userId: String
  ) {
    require(apparentPainLevel == null || apparentPainLevel in 0..5) { "La molestia debe estar entre 0 y 5." }
    val docRef = db.collection("couple_spaces").document(spaceId)
      .collection("observations").document()

    val payload = mapOf(
      "spaceId" to spaceId,
      "date" to date,
      "perceivedMood" to perceivedMood?.name,
      "apparentPainLevel" to apparentPainLevel,
      "unloggedSpottingReported" to unloggedSpottingReported,
      "spottingSuggestionStatus" to SpottingSuggestionStatus.PENDING.name,
      "careActionTaken" to careActionTaken,
      "sweetMessage" to sweetMessage,
      "loggedBy" to userId,
      "createdAt" to FieldValue.serverTimestamp()
    )
    docRef.set(payload).await()
  }

  open fun observeLatestCheckIn(spaceId: String): Flow<CheckIn?> {
    return db.collection("couple_spaces").document(spaceId)
      .collection("check_ins")
      .orderBy("createdAt", Query.Direction.DESCENDING)
      .limit(1)
      .snapshots()
      .map { snapshot ->
        snapshot.documents.firstOrNull()?.toObject(CheckIn::class.java)
      }
  }

  open suspend fun sendCheckIn(
    spaceId: String,
    senderUid: String,
    promptType: CheckInPromptType,
    customNote: String? = null
  ) {
    val docRef = db.collection("couple_spaces").document(spaceId)
      .collection("check_ins").document()

    val payload = mapOf(
      "spaceId" to spaceId,
      "senderUid" to senderUid,
      "promptType" to promptType.name,
      "customNote" to customNote,
      "status" to CheckInStatus.PENDING.name,
      "responseOption" to null,
      "responseNote" to null,
      "answeredAt" to null,
      "createdAt" to FieldValue.serverTimestamp()
    )
    docRef.set(payload).await()
  }

  suspend fun answerCheckIn(
    spaceId: String,
    checkInId: String,
    responseOption: QuickResponseOption,
    responseNote: String? = null
  ) {
    db.collection("couple_spaces").document(spaceId)
      .collection("check_ins").document(checkInId)
      .update(
        mapOf(
          "status" to CheckInStatus.ANSWERED.name,
          "responseOption" to responseOption.name,
          "responseNote" to responseNote,
          "answeredAt" to FieldValue.serverTimestamp()
        )
      ).await()
  }
}
