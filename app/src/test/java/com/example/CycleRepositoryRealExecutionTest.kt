package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CycleEntry
import com.example.data.repository.CycleRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.time.LocalDate
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CycleRepositoryRealExecutionTest {

  private lateinit var application: Application
  private lateinit var db: FirebaseFirestore
  private lateinit var auth: FirebaseAuth
  private lateinit var cycleRepo: CycleRepository

  @Before
  fun setup() {
    application = ApplicationProvider.getApplicationContext()
    if (FirebaseApp.getApps(application).isEmpty()) {
      val options = FirebaseOptions.Builder()
        .setApplicationId("com.aistudio.lunasync.kpmvtw")
        .setProjectId("demo-no-project")
        .setApiKey("fakeApiKeyForTesting123")
        .build()
      FirebaseApp.initializeApp(application, options)
    }
    db = FirebaseFirestore.getInstance()
    try {
      db.useEmulator("127.0.0.1", 8085)
    } catch (_: Exception) {}

    auth = FirebaseAuth.getInstance()
    try {
      auth.useEmulator("127.0.0.1", 9099)
    } catch (_: Exception) {}

    cycleRepo = CycleRepository(application, db)
  }

  private fun <T> runWithLooper(timeoutMs: Long = 25000, block: suspend () -> T): T {
    val deferred = CoroutineScope(Dispatchers.IO).async { block() }
    val start = System.currentTimeMillis()
    while (!deferred.isCompleted && (System.currentTimeMillis() - start < timeoutMs)) {
      ShadowLooper.idleMainLooper()
      Thread.sleep(40)
    }
    if (!deferred.isCompleted) {
      throw java.util.concurrent.TimeoutException("runWithLooper timed out after ${timeoutMs}ms")
    }
    return runBlocking { deferred.await() }
  }

  private suspend fun createSpaceWithElla(spaceId: String): String {
    val ellaEmail = "ella_${UUID.randomUUID()}@test.com"
    val ellaAuth = auth.createUserWithEmailAndPassword(ellaEmail, "password123").await()
    val ellaUid = ellaAuth.user!!.uid

    val spaceData = hashMapOf(
      "ownerId" to ellaUid,
      "partnerId" to null,
      "status" to "UNPAIRED",
      "averageCycleLength" to 28,
      "averagePeriodLength" to 5,
      "activeInviteVersion" to 1,
      "sharingPermissions" to hashMapOf(
        "shareSymptoms" to true,
        "shareMood" to true,
        "shareIntimacy" to false,
        "sharePartnerNotes" to true
      )
    )
    db.collection("couple_spaces").document(spaceId).set(spaceData).await()
    return ellaUid
  }

  @Test
  fun testRecordPeriodStartAndSyncLengths() = runWithLooper {
    val spaceId = "space_${UUID.randomUUID()}"
    createSpaceWithElla(spaceId)

    // 1. Record Cycle 1: 2026-01-01 -> 2026-01-05
    val res1 = cycleRepo.recordPeriodStart(
      spaceId = spaceId,
      startDate = LocalDate.of(2026, 1, 1),
      endDate = LocalDate.of(2026, 1, 5)
    )
    assertTrue("Cycle 1 should be recorded", res1.isSuccess)
    val id1 = res1.getOrThrow()

    // 2. Record Cycle 2: 2026-01-29 -> 2026-02-02 (28 days later)
    val res2 = cycleRepo.recordPeriodStart(
      spaceId = spaceId,
      startDate = LocalDate.of(2026, 1, 29),
      endDate = LocalDate.of(2026, 2, 2)
    )
    assertTrue("Cycle 2 should be recorded", res2.isSuccess)
    val id2 = res2.getOrThrow()

    // Verify Cycle 1 cycleLength is now atomically derived as 28 days (from Jan 1 to Jan 29)
    val doc1 = db.collection("couple_spaces").document(spaceId).collection("cycles").document(id1).get().await()
    assertEquals(28L, doc1.getLong("cycleLength"))
    assertEquals(5L, doc1.getLong("periodDuration"))
    assertFalse(doc1.getBoolean("isCalculated") ?: true)

    // Verify Cycle 2 is the latest cycle, so cycleLength is null (active cycle)
    val doc2 = db.collection("couple_spaces").document(spaceId).collection("cycles").document(id2).get().await()
    assertNull(doc2.get("cycleLength"))
  }

  @Test
  fun testPreventDuplicateStartDate() = runWithLooper {
    val spaceId = "space_${UUID.randomUUID()}"
    createSpaceWithElla(spaceId)

    val res1 = cycleRepo.recordPeriodStart(
      spaceId = spaceId,
      startDate = LocalDate.of(2026, 4, 10),
      endDate = null
    )
    assertTrue(res1.isSuccess)

    // Attempting to record duplicate startDate for same space MUST fail
    val resDuplicate = cycleRepo.recordPeriodStart(
      spaceId = spaceId,
      startDate = LocalDate.of(2026, 4, 10),
      endDate = null
    )
    assertTrue("Duplicate start date must fail", resDuplicate.isFailure)
    assertTrue(resDuplicate.exceptionOrNull()?.message?.contains("Ya existe") == true)
  }

  @Test
  fun testUpdatePeriodEnd() = runWithLooper {
    val spaceId = "space_${UUID.randomUUID()}"
    createSpaceWithElla(spaceId)

    val res = cycleRepo.recordPeriodStart(
      spaceId = spaceId,
      startDate = LocalDate.of(2026, 5, 1),
      endDate = null
    )
    val cycleId = res.getOrThrow()

    // Update end date to 2026-05-06 (6 days duration inclusive: 1, 2, 3, 4, 5, 6)
    val updateRes = cycleRepo.updatePeriodEnd(
      spaceId = spaceId,
      cycleId = cycleId,
      endDate = LocalDate.of(2026, 5, 6)
    )
    assertTrue(updateRes.isSuccess)

    val doc = db.collection("couple_spaces").document(spaceId).collection("cycles").document(cycleId).get().await()
    assertEquals("2026-05-06", doc.getString("endDate"))
    assertEquals(6L, doc.getLong("periodDuration"))
    assertFalse(doc.getBoolean("isCalculated") ?: true)
  }

  @Test
  fun testUpdateCycleDatesAndRecalibrateNeighbors() = runWithLooper {
    val spaceId = "space_${UUID.randomUUID()}"
    createSpaceWithElla(spaceId)

    val id1 = cycleRepo.recordPeriodStart(spaceId, LocalDate.of(2026, 1, 1), null).getOrThrow()
    val id2 = cycleRepo.recordPeriodStart(spaceId, LocalDate.of(2026, 1, 29), null).getOrThrow()

    // Initially, cycle 1 length is 28 days
    var doc1 = db.collection("couple_spaces").document(spaceId).collection("cycles").document(id1).get().await()
    assertEquals(28L, doc1.getLong("cycleLength"))

    // Shift Cycle 2 start date from Jan 29 to Jan 31
    val updateRes = cycleRepo.updateCycleDates(
      spaceId = spaceId,
      cycleId = id2,
      newStartDate = LocalDate.of(2026, 1, 31),
      newEndDate = LocalDate.of(2026, 2, 4)
    )
    assertTrue(updateRes.isSuccess)

    // Now Cycle 1 length must recalibrate to 30 days (Jan 1 to Jan 31)
    doc1 = db.collection("couple_spaces").document(spaceId).collection("cycles").document(id1).get().await()
    assertEquals(30L, doc1.getLong("cycleLength"))
  }

  @Test
  fun testDeleteCycleEntryAndRecalibrate() = runWithLooper {
    val spaceId = "space_${UUID.randomUUID()}"
    createSpaceWithElla(spaceId)

    val id1 = cycleRepo.recordPeriodStart(spaceId, LocalDate.of(2026, 1, 1), null).getOrThrow()
    val id2 = cycleRepo.recordPeriodStart(spaceId, LocalDate.of(2026, 1, 29), null).getOrThrow()
    val id3 = cycleRepo.recordPeriodStart(spaceId, LocalDate.of(2026, 2, 26), null).getOrThrow()

    // Delete middle cycle (id2)
    val delRes = cycleRepo.deleteCycleEntry(spaceId, id2)
    assertTrue(delRes.isSuccess)

    // id2 document should not exist
    val doc2 = db.collection("couple_spaces").document(spaceId).collection("cycles").document(id2).get().await()
    assertFalse(doc2.exists())

    // Cycle 1 length must now be derived directly from Cycle 1 to Cycle 3: Jan 1 to Feb 26 = 56 days
    val doc1 = db.collection("couple_spaces").document(spaceId).collection("cycles").document(id1).get().await()
    assertEquals(56L, doc1.getLong("cycleLength"))
  }

  @Test
  fun testPartnerCannotWriteCycles() = runWithLooper {
    val ellaEmail = "ella_${UUID.randomUUID()}@test.com"
    val ellaAuth = auth.createUserWithEmailAndPassword(ellaEmail, "password123").await()
    val ellaUid = ellaAuth.user!!.uid

    val partnerEmail = "partner_${UUID.randomUUID()}@test.com"
    val partnerAuth = auth.createUserWithEmailAndPassword(partnerEmail, "password123").await()
    val partnerUid = partnerAuth.user!!.uid

    val spaceRepo = com.example.data.repository.CoupleSpaceRepository(application, db)

    // 1. Ella creates space and generates invite
    auth.signInWithEmailAndPassword(ellaEmail, "password123").await()
    val spaceId = spaceRepo.createSpace(ellaUid, 28, 5)
    val inviteCode = spaceRepo.generateInviteCode(spaceId, ellaUid)

    // 2. Partner signs in and joins with invite code
    auth.signInWithEmailAndPassword(partnerEmail, "password123").await()
    val joinRes = spaceRepo.joinWithCode(inviteCode, partnerUid)
    assertTrue("Partner must successfully pair", joinRes.isSuccess)

    // 3. Partner attempts to write to cycles collection -> MUST fail with permission denied
    val cycleMap = mapOf(
      "spaceId" to spaceId,
      "startDate" to "2026-06-01",
      "periodDuration" to 5,
      "isCalculated" to true
    )

    var failedWithPermission = false
    try {
      db.collection("couple_spaces").document(spaceId).collection("cycles").document("hacked_cycle")
        .set(cycleMap).await()
    } catch (e: Exception) {
      failedWithPermission = true
    }
    assertTrue("Partner must not have permission to write to cycles subcollection", failedWithPermission)
  }
}
