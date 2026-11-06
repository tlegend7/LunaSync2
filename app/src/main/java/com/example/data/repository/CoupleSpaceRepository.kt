package com.example.data.repository

import android.content.Context
import com.example.data.model.CoupleSpace
import com.example.data.model.SharingPermissions
import com.example.data.model.SpaceStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import java.util.Date

sealed class SpaceObservationResult {
  object Loading : SpaceObservationResult()
  data class Success(val space: CoupleSpace) : SpaceObservationResult()
  object NoSpaceFound : SpaceObservationResult()
  object PairingLost : SpaceObservationResult()
  data class Error(val exception: Throwable) : SpaceObservationResult()
}

class CoupleSpaceRepository(context: Context) {
  private val db: FirebaseFirestore = FirestoreProvider.getDb(context)
  private val secureRandom = SecureRandom()

  fun observeSpace(spaceId: String): Flow<CoupleSpace?> {
    return db.collection("couple_spaces").document(spaceId)
      .snapshots()
      .map { snapshot ->
        if (snapshot.exists()) snapshot.toObject(CoupleSpace::class.java) else null
      }
  }

  /**
   * Robustly observes space association for a given userId:
   * - Emits Loading initially
   * - Combines ownerQuery and partnerQuery asynchronously
   * - Explicitly distinguishes between Loading, Success, NoSpaceFound, PairingLost, and Error
   * - Handles PERMISSION_DENIED cleanly when access is revoked
   */
  fun observeUserSpaceDetailed(userId: String): Flow<SpaceObservationResult> = callbackFlow {
    trySend(SpaceObservationResult.Loading)

    var ownerSpace: CoupleSpace? = null
    var ownerLoaded = false
    var partnerSpace: CoupleSpace? = null
    var partnerLoaded = false
    var hadPairedPartnerSpaceBefore = false

    fun evaluateAndEmit() {
      if (!ownerLoaded || !partnerLoaded) {
        // Wait for both queries to complete initial snapshot
        return
      }

      val activeSpace = ownerSpace ?: partnerSpace
      if (activeSpace != null) {
        if (activeSpace.partnerId == userId && activeSpace.status == SpaceStatus.PAIRED.name) {
          hadPairedPartnerSpaceBefore = true
        }
        trySend(SpaceObservationResult.Success(activeSpace))
      } else {
        if (hadPairedPartnerSpaceBefore) {
          trySend(SpaceObservationResult.PairingLost)
        } else {
          trySend(SpaceObservationResult.NoSpaceFound)
        }
      }
    }

    val ownerQuery = db.collection("couple_spaces").whereEqualTo("ownerId", userId)
    val partnerQuery = db.collection("couple_spaces")
      .whereEqualTo("partnerId", userId)
      .whereEqualTo("status", SpaceStatus.PAIRED.name)

    val listenerOwner = ownerQuery.addSnapshotListener { snapshot, error ->
      if (error != null) {
        val isPermissionDenied = error.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true
        if (isPermissionDenied && hadPairedPartnerSpaceBefore) {
          trySend(SpaceObservationResult.PairingLost)
        } else {
          trySend(SpaceObservationResult.Error(error))
        }
        return@addSnapshotListener
      }
      ownerSpace = snapshot?.documents?.firstOrNull()?.toObject(CoupleSpace::class.java)
      ownerLoaded = true
      evaluateAndEmit()
    }

    val listenerPartner = partnerQuery.addSnapshotListener { snapshot, error ->
      if (error != null) {
        val isPermissionDenied = error.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true
        if (isPermissionDenied && hadPairedPartnerSpaceBefore) {
          trySend(SpaceObservationResult.PairingLost)
        } else {
          trySend(SpaceObservationResult.Error(error))
        }
        return@addSnapshotListener
      }
      partnerSpace = snapshot?.documents?.firstOrNull()?.toObject(CoupleSpace::class.java)
      partnerLoaded = true
      evaluateAndEmit()
    }

    awaitClose {
      listenerOwner.remove()
      listenerPartner.remove()
    }
  }

  suspend fun createSpace(
    ownerId: String,
    averageCycleLength: Int,
    averagePeriodLength: Int
  ): String {
    val spaceDoc = db.collection("couple_spaces").document()
    val space = CoupleSpace(
      id = spaceDoc.id,
      ownerId = ownerId,
      partnerId = null,
      status = SpaceStatus.UNPAIRED.name,
      averageCycleLength = averageCycleLength,
      averagePeriodLength = averagePeriodLength,
      sharingPermissions = SharingPermissions()
    )
    val data = mapOf(
      "ownerId" to space.ownerId,
      "partnerId" to null,
      "status" to space.status,
      "averageCycleLength" to space.averageCycleLength,
      "averagePeriodLength" to space.averagePeriodLength,
      "sharingPermissions" to mapOf(
        "shareSymptoms" to space.sharingPermissions.shareSymptoms,
        "shareMood" to space.sharingPermissions.shareMood,
        "shareIntimacy" to space.sharingPermissions.shareIntimacy,
        "sharePartnerNotes" to space.sharingPermissions.sharePartnerNotes
      ),
      "activeInviteVersion" to 1,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    spaceDoc.set(data).await()
    return spaceDoc.id
  }

  suspend fun generateInviteCode(spaceId: String, createdBy: String): String {
    val spaceRef = db.collection("couple_spaces").document(spaceId)
    val spaceSnap = spaceRef.get().await()
    val currentVersion = spaceSnap.getLong("activeInviteVersion") ?: 1L
    val newVersion = currentVersion + 1

    // Update space's active invite version first or concurrently so any stale invites are invalidated
    spaceRef.update("activeInviteVersion", newVersion).await()

    // Generate cryptographically secure invite code: LUNA-XXXX
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    val sb = StringBuilder(4)
    for (i in 0 until 4) {
      val idx = secureRandom.nextInt(chars.length)
      sb.append(chars[idx])
    }
    val inviteCode = "LUNA-$sb"

    val expiresAt = Timestamp(Date(System.currentTimeMillis() + 48 * 3600 * 1000L)) // 48h
    val newInviteRef = db.collection("pairing_invites").document(inviteCode)
    val inviteData = mapOf(
      "spaceId" to spaceId,
      "createdBy" to createdBy,
      "status" to "ACTIVE",
      "version" to newVersion,
      "expiresAt" to expiresAt,
      "createdAt" to FieldValue.serverTimestamp()
    )
    newInviteRef.set(inviteData).await()

    return inviteCode
  }

  /**
   * Atomic Firestore transaction for joining with code:
   * - Validates existence, status ACTIVE, and expiration
   * - Prevents owner from self-pairing
   * - Validates space is UNPAIRED and not already taken
   * - Validates invite version matches space's active version
   * - Atomically marks invite CONSUMED and space PAIRED
   */
  suspend fun joinWithCode(code: String, partnerId: String): Result<String> {
    val cleanCode = code.trim().uppercase()
    val inviteRef = db.collection("pairing_invites").document(cleanCode)

    return try {
      val spaceId = db.runTransaction { transaction ->
        val inviteSnap = transaction.get(inviteRef)
        if (!inviteSnap.exists()) {
          throw IllegalArgumentException("Código de invitación no encontrado")
        }

        val status = inviteSnap.getString("status") ?: "EXPIRED"
        if (status != "ACTIVE") {
          throw IllegalStateException("Este código ya fue utilizado o ha sido revocado")
        }

        val expiresAt = inviteSnap.getTimestamp("expiresAt")
        if (expiresAt == null || expiresAt.toDate().before(Date())) {
          throw IllegalStateException("Este código de invitación ha expirado")
        }

        val targetSpaceId = inviteSnap.getString("spaceId")
          ?: throw IllegalStateException("Espacio no asociado a este código")

        val inviteVersion = inviteSnap.getLong("version") ?: 1L

        val spaceRef = db.collection("couple_spaces").document(targetSpaceId)
        val spaceSnap = transaction.get(spaceRef)

        if (!spaceSnap.exists()) {
          throw IllegalStateException("El espacio de pareja no existe")
        }

        val ownerId = spaceSnap.getString("ownerId")
        if (ownerId == partnerId) {
          throw IllegalStateException("No puedes vincularte a tu propio espacio como pareja")
        }

        val currentPartner = spaceSnap.getString("partnerId")
        val currentStatus = spaceSnap.getString("status")
        if (!currentPartner.isNullOrBlank() || currentStatus == SpaceStatus.PAIRED.name) {
          throw IllegalStateException("Este espacio ya cuenta con una pareja vinculada")
        }

        val spaceVersion = spaceSnap.getLong("activeInviteVersion") ?: 1L
        if (inviteVersion != spaceVersion) {
          throw IllegalStateException("Esta invitación ha sido reemplazada por una más reciente")
        }

        // Apply atomic updates
        transaction.update(
          inviteRef,
          mapOf(
            "status" to "CONSUMED",
            "consumedBy" to partnerId,
            "consumedAt" to FieldValue.serverTimestamp()
          )
        )

        transaction.update(
          spaceRef,
          mapOf(
            "partnerId" to partnerId,
            "status" to SpaceStatus.PAIRED.name,
            "claimedInviteCode" to cleanCode,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )

        targetSpaceId
      }.await()

      Result.success(spaceId)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun updatePermissions(spaceId: String, permissions: SharingPermissions) {
    db.collection("couple_spaces").document(spaceId).update(
      "sharingPermissions",
      mapOf(
        "shareSymptoms" to permissions.shareSymptoms,
        "shareMood" to permissions.shareMood,
        "shareIntimacy" to permissions.shareIntimacy,
        "sharePartnerNotes" to permissions.sharePartnerNotes
      ),
      "updatedAt", FieldValue.serverTimestamp()
    ).await()
  }

  /**
   * Atomic revocation:
   * Sets partnerId to null, marks space UNPAIRED, increments activeInviteVersion
   * to immediately invalidate any outstanding invites concurrently or offline.
   */
  suspend fun revokePairing(spaceId: String) {
    val spaceRef = db.collection("couple_spaces").document(spaceId)
    db.runTransaction { transaction ->
      val snapshot = transaction.get(spaceRef)
      val currentVersion = snapshot.getLong("activeInviteVersion") ?: 1L

      transaction.update(
        spaceRef,
        mapOf(
          "partnerId" to null,
          "status" to SpaceStatus.UNPAIRED.name,
          "activeInviteVersion" to currentVersion + 1L,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      )
    }.await()
  }
}
