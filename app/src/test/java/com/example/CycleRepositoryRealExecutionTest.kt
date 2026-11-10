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
import kotlinx.coroutines.flow.first
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

    // Verify Cycle 1 cycleLength is now derived canonically as 28 days (from Jan 1 to Jan 29)
    val cycles = cycleRepo.observeCycles(spaceId).first()
    val c1 = cycles.first { it.startDate == "2026-01-01" }
    val c2 = cycles.first { it.startDate == "2026-01-29" }
    assertEquals(28, c1.cycleLength)
    assertEquals(5, c1.periodDuration)
    assertFalse(c1.isCalculated)

    // Verify Cycle 2 is the latest cycle, so cycleLength is null (active cycle)
    assertNull(c2.cycleLength)
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
    var cycles = cycleRepo.observeCycles(spaceId).first()
    assertEquals(28, cycles.first { it.startDate == "2026-01-01" }.cycleLength)

    // Shift Cycle 2 start date from Jan 29 to Jan 31
    val updateRes = cycleRepo.updateCycleDates(
      spaceId = spaceId,
      cycleId = id2,
      newStartDate = LocalDate.of(2026, 1, 31),
      newEndDate = LocalDate.of(2026, 2, 4)
    )
    assertTrue("Update must succeed: ${updateRes.exceptionOrNull()?.message}", updateRes.isSuccess)

    val docOld = db.collection("couple_spaces").document(spaceId).collection("cycles").document(id2).get().await()
    assertFalse("Old date doc must be removed", docOld.exists())
    val docNew = db.collection("couple_spaces").document(spaceId).collection("cycles").document("2026-01-31").get().await()
    assertTrue("New date doc must exist", docNew.exists())

    // Now Cycle 1 length must recalibrate to 30 days (Jan 1 to Jan 31)
    cycles = cycleRepo.observeCycles(spaceId).first()
    assertEquals(30, cycles.first { it.startDate == "2026-01-01" }.cycleLength)
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
    val cycles = cycleRepo.observeCycles(spaceId).first()
    assertEquals(56, cycles.first { it.startDate == "2026-01-01" }.cycleLength)
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

  @Test
  fun testConcurrentRecordPeriodStartDuplicatePrevention() = runWithLooper {
    val spaceId = "space_${UUID.randomUUID()}"
    createSpaceWithElla(spaceId)

    val targetDate = "2026-07-01"
    var secondCallResult: Result<String>? = null

    // Interleave execution: before the first write commits to Firestore, trigger a concurrent attempt
    cycleRepo.onBeforeRecordPeriodStartHook = { date ->
      // Temporarily clear hook to avoid recursion
      cycleRepo.onBeforeRecordPeriodStartHook = null
      secondCallResult = cycleRepo.recordPeriodStart(spaceId, date, null)
    }

    val firstCallResult = cycleRepo.recordPeriodStart(spaceId, targetDate, null)

    assertNotNull("Second call must have executed", secondCallResult)
    val oneSucceeded = firstCallResult.isSuccess xor secondCallResult!!.isSuccess
    assertTrue("Exactly one of the concurrent writes must succeed and the other fail", oneSucceeded)

    val docs = db.collection("couple_spaces").document(spaceId).collection("cycles")
      .whereEqualTo("startDate", targetDate).get().await()
    assertEquals("Exactly one document must exist for the date", 1, docs.size())
  }

  @Test
  fun testRecordPeriodStartPreservesUnclippedDurationOver15Days() = runWithLooper {
    val spaceId = "space_${UUID.randomUUID()}"
    createSpaceWithElla(spaceId)

    // Bleeding for 20 days: Aug 1 to Aug 20 (20 days inclusive)
    val res = cycleRepo.recordPeriodStart(
      spaceId = spaceId,
      startDate = LocalDate.of(2026, 8, 1),
      endDate = LocalDate.of(2026, 8, 20)
    )
    assertTrue("Record with 20 days duration must succeed", res.isSuccess)
    val cycleId = res.getOrThrow()

    val doc = db.collection("couple_spaces").document(spaceId).collection("cycles").document(cycleId).get().await()
    assertEquals(20L, doc.getLong("periodDuration"))
    assertEquals("2026-08-20", doc.getString("endDate"))
    assertFalse(doc.getBoolean("isCalculated") ?: true)
  }

  @Test
  fun testUpdateCycleDatesRejectsMovingToExistingStartDate() = runWithLooper {
    val spaceId = "space_${UUID.randomUUID()}"
    createSpaceWithElla(spaceId)

    val id1 = cycleRepo.recordPeriodStart(spaceId, LocalDate.of(2026, 9, 1), null).getOrThrow()
    val id2 = cycleRepo.recordPeriodStart(spaceId, LocalDate.of(2026, 9, 28), null).getOrThrow()

    // Attempting to move cycle 2's start date to 2026-09-01 (occupied by cycle 1) MUST fail
    val updateRes = cycleRepo.updateCycleDates(
      spaceId = spaceId,
      cycleId = id2,
      newStartDate = LocalDate.of(2026, 9, 1),
      newEndDate = null
    )
    assertTrue("Moving to already occupied startDate must fail", updateRes.isFailure)
    assertTrue(updateRes.exceptionOrNull()?.message?.contains("Ya existe") == true)
  }

  @Test
  fun testTwoLegacyRecordsMovedConcurrentlyToSameDate() = runWithLooper {
    val spaceId = "space_${UUID.randomUUID()}"
    createSpaceWithElla(spaceId)

    // Seed two legacy records with random UUIDs directly into Firestore
    val legacyId1 = "legacy_${UUID.randomUUID()}"
    val legacyId2 = "legacy_${UUID.randomUUID()}"
    val cyclesCol = db.collection("couple_spaces").document(spaceId).collection("cycles")

    cyclesCol.document(legacyId1).set(mapOf(
      "spaceId" to spaceId,
      "startDate" to "2026-10-01",
      "periodDuration" to 5,
      "isCalculated" to true
    )).await()

    cyclesCol.document(legacyId2).set(mapOf(
      "spaceId" to spaceId,
      "startDate" to "2026-10-15",
      "periodDuration" to 5,
      "isCalculated" to true
    )).await()

    val targetDate = LocalDate.of(2026, 11, 1)
    var secondCallResult: Result<Unit>? = null

    // Interleave execution: before the first update commits, trigger the second update to the same target date
    cycleRepo.onBeforeUpdateCycleDatesHook = { _, _, _ ->
      cycleRepo.onBeforeUpdateCycleDatesHook = null
      secondCallResult = cycleRepo.updateCycleDates(
        spaceId = spaceId,
        cycleId = legacyId2,
        newStartDate = targetDate,
        newEndDate = null
      )
    }

    val firstCallResult = cycleRepo.updateCycleDates(
      spaceId = spaceId,
      cycleId = legacyId1,
      newStartDate = targetDate,
      newEndDate = null
    )

    assertNotNull("Second update call must have executed", secondCallResult)
    val oneSucceeded = firstCallResult.isSuccess xor secondCallResult!!.isSuccess
    assertTrue("Exactly one concurrent move to target date must succeed, the other must fail", oneSucceeded)

    // Exactly one cycle document must exist for the target date
    val docsTarget = cyclesCol.whereEqualTo("startDate", "2026-11-01").get().await()
    assertEquals("Exactly one document must exist for target date", 1, docsTarget.size())

    // The losing legacy document must still exist with valid data (not corrupted or orphaned)
    val loserId = if (firstCallResult.isSuccess) legacyId2 else legacyId1
    val loserDoc = cyclesCol.document(loserId).get().await()
    assertTrue("Losing legacy document must retain its valid state", loserDoc.exists())
  }

  @Test
  fun testConcurrentCreationAndLegacyEditToSameDate() = runWithLooper {
    val spaceId = "space_${UUID.randomUUID()}"
    createSpaceWithElla(spaceId)

    val legacyId = "legacy_${UUID.randomUUID()}"
    val cyclesCol = db.collection("couple_spaces").document(spaceId).collection("cycles")
    cyclesCol.document(legacyId).set(mapOf(
      "spaceId" to spaceId,
      "startDate" to "2026-10-01",
      "periodDuration" to 5,
      "isCalculated" to true
    )).await()

    val targetDateStr = "2026-12-01"
    val targetDate = LocalDate.of(2026, 12, 1)
    var legacyEditResult: Result<Unit>? = null

    // When recordPeriodStart is about to commit, interleave legacy edit moving to the exact same date
    cycleRepo.onBeforeRecordPeriodStartHook = {
      cycleRepo.onBeforeRecordPeriodStartHook = null
      legacyEditResult = cycleRepo.updateCycleDates(
        spaceId = spaceId,
        cycleId = legacyId,
        newStartDate = targetDate,
        newEndDate = null
      )
    }

    val createResult = cycleRepo.recordPeriodStart(spaceId, targetDateStr, null)

    assertNotNull("Legacy edit must have executed", legacyEditResult)
    val oneSucceeded = createResult.isSuccess xor legacyEditResult!!.isSuccess
    assertTrue("Exactly one of creation or legacy move must succeed and the other fail", oneSucceeded)

    val docs = cyclesCol.whereEqualTo("startDate", targetDateStr).get().await()
    assertEquals("Exactly one document must exist for target date", 1, docs.size())
  }

  @Test
  fun testDateReleasedAndReusedAfterMoveOrDelete() = runWithLooper {
    val spaceId = "space_${UUID.randomUUID()}"
    createSpaceWithElla(spaceId)

    val date1 = LocalDate.of(2026, 5, 10)
    val date2 = LocalDate.of(2026, 5, 20)

    // 1. Create a cycle at date1
    val id1 = cycleRepo.recordPeriodStart(spaceId, date1, null).getOrThrow()

    // 2. Move id1 from date1 to date2
    val moveRes = cycleRepo.updateCycleDates(spaceId, id1, date2, null)
    assertTrue("Move must succeed", moveRes.isSuccess)

    // 3. Now date1 is released. We should be able to create a new cycle at date1!
    val reuseRes = cycleRepo.recordPeriodStart(spaceId, date1, null)
    assertTrue("Date1 should be reusable after being moved", reuseRes.isSuccess)

    // 4. Delete the reused cycle at date1
    val delRes = cycleRepo.deleteCycleEntry(spaceId, reuseRes.getOrThrow())
    assertTrue("Delete must succeed", delRes.isSuccess)

    // 5. date1 is released again after deletion. We can record period start at date1 again!
    val reuseRes2 = cycleRepo.recordPeriodStart(spaceId, date1, null)
    assertTrue("Date1 should be reusable after deletion", reuseRes2.isSuccess)
  }
}

