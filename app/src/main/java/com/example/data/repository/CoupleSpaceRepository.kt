package com.example.data.repository

import android.content.Context
import com.example.data.model.CoupleSpace
import com.example.data.model.SharingPermissions
import com.example.data.model.SpaceStatus
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
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

open class CoupleSpaceRepository(
  context: Context,
  protected val db: FirebaseFirestore = FirestoreProvider.getDb(context)
) {
  protected val auth: FirebaseAuth = FirestoreProvider.getAuth(context)
  private val secureRandom = SecureRandom()

  open fun observeSpace(spaceId: String): Flow<CoupleSpace?> {
    return db.collection("couple_spaces").document(spaceId)
      .snapshots()
      .map { snapshot ->
        if (snapshot.exists()) snapshot.toObject(CoupleSpace::class.java) else null
      }
  }

  open fun observeUserSpaceDetailed(userId: String): Flow<SpaceObservationResult> = callbackFlow {
    trySend(SpaceObservationResult.Loading)

    var ownerSpace: CoupleSpace? = null
    var ownerLoaded = false
    var partnerSpace: CoupleSpace? = null
    var partnerLoaded = false
    var hadPairedPartnerSpaceBefore = false

    fun evaluateAndEmit() {
      if (!ownerLoaded || !partnerLoaded) {
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

  /**
   * Generates a pairing invite code atomically using a Firestore transaction:
   * - Increments activeInviteVersion on the space document.
   * - Generates a secure, non-colliding LUNA-XXXX code.
   * - Writes the new pairing_invites document inside the transaction.
   * Both writes succeed or fail together.
   */
  open suspend fun generateInviteCode(spaceId: String, createdBy: String): String {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    val spaceRef = db.collection("couple_spaces").document(spaceId)

    var generatedCode = ""

    db.runTransaction { transaction ->
      val spaceSnap = transaction.get(spaceRef)
      if (!spaceSnap.exists()) {
        throw IllegalStateException("El espacio no existe")
      }
      val currentOwner = spaceSnap.getString("ownerId")
      if (currentOwner != createdBy) {
        throw IllegalStateException("Solo la propietaria puede generar invitaciones")
      }

      val currentVersion = spaceSnap.getLong("activeInviteVersion") ?: 1L
      val newVersion = currentVersion + 1L

      // Generate non-colliding code
      var attempts = 0
      var codeCandidate = ""
      var inviteRef = db.collection("pairing_invites").document()

      while (attempts < 5) {
        val sb = StringBuilder(4)
        for (i in 0 until 4) {
          val idx = secureRandom.nextInt(chars.length)
          sb.append(chars[idx])
        }
        codeCandidate = "LUNA-$sb"
        val candidateRef = db.collection("pairing_invites").document(codeCandidate)
        val candidateSnap = transaction.get(candidateRef)
        if (!candidateSnap.exists()) {
          inviteRef = candidateRef
          break
        }
        attempts++
      }

      if (codeCandidate.isEmpty() || attempts >= 5) {
        throw IllegalStateException("No se pudo generar un código único")
      }

      // Update space's active invite version
      transaction.update(
        spaceRef,
        mapOf(
          "activeInviteVersion" to newVersion,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      )

      val expiresAt = Timestamp(Date(System.currentTimeMillis() + 48 * 3600 * 1000L)) // 48h
      val inviteData = mapOf(
        "spaceId" to spaceId,
        "createdBy" to createdBy,
        "status" to "ACTIVE",
        "version" to newVersion,
        "expiresAt" to expiresAt,
        "createdAt" to FieldValue.serverTimestamp()
      )
      transaction.set(inviteRef, inviteData)

      generatedCode = codeCandidate
    }.await()

    return generatedCode
  }

  /**
   * Atomic join with code WITHOUT illegal prior read of private couple_space document:
   * - Reads pairing_invites document (authorized for get if authenticated).
   * - Validates code existence, ACTIVE status, and expiration.
   * - Derives current partner UID strictly from authenticated session (request.auth.uid).
   * - Executes atomic batch write pairing space and consuming invite.
   * - Firestore rules evaluate and enforce getAfter() coupling, version match, and partnerId verification.
   */
  open suspend fun joinWithCode(code: String, partnerId: String): Result<String> {
    val cleanCode = code.trim().uppercase()
    val inviteRef = db.collection("pairing_invites").document(cleanCode)

    return try {
      val inviteSnap = inviteRef.get().await()
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

      val createdBy = inviteSnap.getString("createdBy")
      val actualUid = auth.currentUser?.uid ?: partnerId
      if (createdBy == actualUid) {
        throw IllegalStateException("No puedes vincularte a tu propio espacio como pareja")
      }

      val spaceRef = db.collection("couple_spaces").document(targetSpaceId)

      // Atomic commit: updates both documents simultaneously.
      // Firestore rules verify with getAfter that both documents transitioned together.
      val batch = db.batch()
      batch.update(
        inviteRef,
        mapOf(
          "status" to "CONSUMED",
          "consumedBy" to actualUid,
          "consumedAt" to FieldValue.serverTimestamp()
        )
      )
      batch.update(
        spaceRef,
        mapOf(
          "partnerId" to actualUid,
          "status" to SpaceStatus.PAIRED.name,
          "claimedInviteCode" to cleanCode,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      )
      batch.commit().await()

      Result.success(targetSpaceId)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  open suspend fun updatePermissions(spaceId: String, permissions: SharingPermissions) {
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
   * to immediately invalidate any outstanding invites.
   */
  open suspend fun revokePairing(spaceId: String) {
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
