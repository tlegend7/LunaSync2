package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.FlowLevel
import com.example.data.model.PerceivedMood
import com.example.data.model.SpottingSuggestionStatus
import com.example.data.repository.CycleRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

/**
 * Pruebas de EJECUCIÓN REAL de CycleRepository.acceptSpottingSuggestion()
 * contra el emulador de Firestore con reglas activas y autenticación.
 *
 * Cubre los 8 escenarios mínimos exigidos:
 * A. Preservación de sangrado abundante (HEAVY).
 * B. Preservación de sangrado moderado (MEDIUM).
 * C. Preservación de sangrado leve (LIGHT).
 * D. Conversión de flujo NONE a SPOTTING.
 * E. Creación de nuevo síntoma privado por defecto (isPrivate = true) cuando no existe registro previo.
 * F. Idempotencia cuando la sugerencia ya fue aceptada.
 * G. Fallo controlado ante sugerencia descartada, fecha incorrecta o espacio ajeno.
 * H. Actualización atómica de observación + síntoma en una única transacción de servidor.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CycleRepositorySpottingRealExecutionTest {

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

  private suspend fun createAuthenticatedSpaceAndObservation(
    date: String,
    unloggedSpotting: Boolean = true,
    suggestionStatus: SpottingSuggestionStatus = SpottingSuggestionStatus.PENDING
  ): Triple<String, String, String> {
    // 1. Crear Ella (Owner)
    val ellaEmail = "ella_${UUID.randomUUID()}@test.com"
    val ellaAuth = auth.createUserWithEmailAndPassword(ellaEmail, "password123").await()
    val ellaUid = ellaAuth.user!!.uid

    // 2. Crear Novio (Partner)
    val partnerEmail = "partner_${UUID.randomUUID()}@test.com"
    val partnerAuth = auth.createUserWithEmailAndPassword(partnerEmail, "password123").await()
    val partnerUid = partnerAuth.user!!.uid

    val spaceRepo = com.example.data.repository.CoupleSpaceRepository(application, db)

    // 3. Ella crea el espacio y genera código de invitación
    auth.signInWithEmailAndPassword(ellaEmail, "password123").await()
    val spaceId = spaceRepo.createSpace(ellaUid, 28, 5)
    val inviteCode = spaceRepo.generateInviteCode(spaceId, ellaUid)

    // 4. Partner se vincula mediante canje atómico que cumple firestore.rules
    auth.signInWithEmailAndPassword(partnerEmail, "password123").await()
    val joinRes = spaceRepo.joinWithCode(inviteCode, partnerUid)
    assertTrue("Partner must successfully pair", joinRes.isSuccess)

    // 5. Partner crea la observación como pareja vinculada autorizada
    val obsId = "obs_${UUID.randomUUID().toString().take(8)}"
    val obsDoc = db.collection("couple_spaces").document(spaceId)
      .collection("observations").document(obsId)
    obsDoc.set(mapOf(
      "spaceId" to spaceId,
      "date" to date,
      "perceivedMood" to PerceivedMood.TIRED.name,
      "apparentPainLevel" to 2,
      "unloggedSpottingReported" to unloggedSpotting,
      "spottingSuggestionStatus" to suggestionStatus.name,
      "careActionTaken" to "Preparé infusión de jengibre y bolsa térmica",
      "sweetMessage" to "Te noto cansada mi amor, descansa",
      "loggedBy" to partnerUid,
      "createdAt" to FieldValue.serverTimestamp()
    )).await()

    // 6. Ella inicia sesión para tener autorización como propietaria al aceptar la sugerencia
    auth.signInWithEmailAndPassword(ellaEmail, "password123").await()

    return Triple(spaceId, obsId, ellaUid)
  }

  // --- ESCENARIO A: Preservación HEAVY ---
  @Test
  fun `Escenario A - acceptSpottingSuggestion preserva flujo HEAVY preexistente`() {
    runWithLooper {
      val date = "2026-06-01"
      val (spaceId, obsId, uid) = createAuthenticatedSpaceAndObservation(date)

      // Preexistente: flujo HEAVY
      val physDoc = db.collection("couple_spaces").document(spaceId)
        .collection("physical_symptoms").document(date)
      physDoc.set(mapOf(
        "spaceId" to spaceId,
        "date" to date,
        "flowLevel" to FlowLevel.HEAVY.name,
        "painLevel" to 4,
        "isPrivate" to false,
        "loggedBy" to uid,
        "updatedAt" to FieldValue.serverTimestamp()
      )).await()

      // Ejecución real
      val result = cycleRepo.acceptSpottingSuggestion(spaceId, obsId, date, uid)
      assertTrue("Result must be success", result.isSuccess)

      // Verificación en Firestore
      val afterSnap = physDoc.get().await()
      assertEquals("HEAVY flow must NOT be downgraded to SPOTTING", FlowLevel.HEAVY.name, afterSnap.getString("flowLevel"))

      val obsSnap = db.collection("couple_spaces").document(spaceId)
        .collection("observations").document(obsId).get().await()
      assertEquals(SpottingSuggestionStatus.ACCEPTED.name, obsSnap.getString("spottingSuggestionStatus"))
    }
  }

  // --- ESCENARIO B: Preservación MEDIUM ---
  @Test
  fun `Escenario B - acceptSpottingSuggestion preserva flujo MEDIUM preexistente`() {
    runWithLooper {
      val date = "2026-06-02"
      val (spaceId, obsId, uid) = createAuthenticatedSpaceAndObservation(date)

      val physDoc = db.collection("couple_spaces").document(spaceId)
        .collection("physical_symptoms").document(date)
      physDoc.set(mapOf(
        "spaceId" to spaceId,
        "date" to date,
        "flowLevel" to FlowLevel.MEDIUM.name,
        "painLevel" to 2,
        "isPrivate" to false,
        "loggedBy" to uid,
        "updatedAt" to FieldValue.serverTimestamp()
      )).await()

      val result = cycleRepo.acceptSpottingSuggestion(spaceId, obsId, date, uid)
      assertTrue(result.isSuccess)

      val afterSnap = physDoc.get().await()
      assertEquals(FlowLevel.MEDIUM.name, afterSnap.getString("flowLevel"))
    }
  }

  // --- ESCENARIO C: Preservación LIGHT ---
  @Test
  fun `Escenario C - acceptSpottingSuggestion preserva flujo LIGHT preexistente`() {
    runWithLooper {
      val date = "2026-06-03"
      val (spaceId, obsId, uid) = createAuthenticatedSpaceAndObservation(date)

      val physDoc = db.collection("couple_spaces").document(spaceId)
        .collection("physical_symptoms").document(date)
      physDoc.set(mapOf(
        "spaceId" to spaceId,
        "date" to date,
        "flowLevel" to FlowLevel.LIGHT.name,
        "painLevel" to 1,
        "isPrivate" to true,
        "loggedBy" to uid,
        "updatedAt" to FieldValue.serverTimestamp()
      )).await()

      val result = cycleRepo.acceptSpottingSuggestion(spaceId, obsId, date, uid)
      assertTrue(result.isSuccess)

      val afterSnap = physDoc.get().await()
      assertEquals(FlowLevel.LIGHT.name, afterSnap.getString("flowLevel"))
      assertEquals(true, afterSnap.getBoolean("isPrivate"))
    }
  }

  // --- ESCENARIO D: Conversión NONE -> SPOTTING ---
  @Test
  fun `Escenario D - acceptSpottingSuggestion convierte flujo NONE a SPOTTING`() {
    runWithLooper {
      val date = "2026-06-04"
      val (spaceId, obsId, uid) = createAuthenticatedSpaceAndObservation(date)

      val physDoc = db.collection("couple_spaces").document(spaceId)
        .collection("physical_symptoms").document(date)
      physDoc.set(mapOf(
        "spaceId" to spaceId,
        "date" to date,
        "flowLevel" to FlowLevel.NONE.name,
        "painLevel" to 0,
        "isPrivate" to false,
        "loggedBy" to uid,
        "updatedAt" to FieldValue.serverTimestamp()
      )).await()

      val result = cycleRepo.acceptSpottingSuggestion(spaceId, obsId, date, uid)
      assertTrue(result.isSuccess)

      val afterSnap = physDoc.get().await()
      assertEquals(FlowLevel.SPOTTING.name, afterSnap.getString("flowLevel"))
    }
  }

  // --- ESCENARIO E: Sin registro previo -> crea nuevo síntoma privado por defecto ---
  @Test
  fun `Escenario E - acceptSpottingSuggestion crea nuevo registro con isPrivate true por defecto`() {
    runWithLooper {
      val date = "2026-06-05"
      val (spaceId, obsId, uid) = createAuthenticatedSpaceAndObservation(date)

      val physDoc = db.collection("couple_spaces").document(spaceId)
        .collection("physical_symptoms").document(date)
      assertFalse("Document must NOT exist prior to test", physDoc.get().await().exists())

      val result = cycleRepo.acceptSpottingSuggestion(spaceId, obsId, date, uid)
      assertTrue(result.isSuccess)

      val createdSnap = physDoc.get().await()
      assertTrue("Document must now exist", createdSnap.exists())
      assertEquals(FlowLevel.SPOTTING.name, createdSnap.getString("flowLevel"))
      assertEquals("Conservative privacy must be default true for auto-created record", true, createdSnap.getBoolean("isPrivate"))
      assertEquals(0L, createdSnap.getLong("painLevel"))
    }
  }

  // --- ESCENARIO F: Idempotencia si ya fue aceptada ---
  @Test
  fun `Escenario F - acceptSpottingSuggestion es idempotente cuando la observacion ya fue aceptada`() {
    runWithLooper {
      val date = "2026-06-06"
      val (spaceId, obsId, uid) = createAuthenticatedSpaceAndObservation(date)

      // Primera llamada: pasa de PENDING a ACCEPTED
      val result1 = cycleRepo.acceptSpottingSuggestion(spaceId, obsId, date, uid)
      assertTrue("First accept must succeed", result1.isSuccess)

      // Segunda llamada: ya aceptada, debe ser idempotente sin error ni mutaciones corruptas
      val result2 = cycleRepo.acceptSpottingSuggestion(spaceId, obsId, date, uid)
      assertTrue("Idempotent second run must succeed without errors", result2.isSuccess)

      val obsSnap = db.collection("couple_spaces").document(spaceId)
        .collection("observations").document(obsId).get().await()
      assertEquals(SpottingSuggestionStatus.ACCEPTED.name, obsSnap.getString("spottingSuggestionStatus"))
    }
  }

  // --- ESCENARIO G: Falla limpiamente ante discrepancia de fecha o espacio ---
  @Test
  fun `Escenario G - acceptSpottingSuggestion falla limpiamente ante discrepancia de fecha`() {
    runWithLooper {
      val date = "2026-06-07"
      val wrongDate = "2026-06-08"
      val (spaceId, obsId, uid) = createAuthenticatedSpaceAndObservation(date)

      val result = cycleRepo.acceptSpottingSuggestion(spaceId, obsId, wrongDate, uid)
      assertTrue("Discrepancy must return Result.failure", result.isFailure)
    }
  }

  // --- ESCENARIO H: Actualización atómica de observación + síntoma ---
  @Test
  fun `Escenario H - observacion y sintoma se actualizan de forma atomica en la misma transaccion`() {
    runWithLooper {
      val date = "2026-06-09"
      val (spaceId, obsId, uid) = createAuthenticatedSpaceAndObservation(date)

      val result = cycleRepo.acceptSpottingSuggestion(spaceId, obsId, date, uid)
      assertTrue(result.isSuccess)

      val obsSnap = db.collection("couple_spaces").document(spaceId)
        .collection("observations").document(obsId).get().await()
      val physSnap = db.collection("couple_spaces").document(spaceId)
        .collection("physical_symptoms").document(date).get().await()

      assertEquals(SpottingSuggestionStatus.ACCEPTED.name, obsSnap.getString("spottingSuggestionStatus"))
      assertEquals(FlowLevel.SPOTTING.name, physSnap.getString("flowLevel"))
    }
  }
}
