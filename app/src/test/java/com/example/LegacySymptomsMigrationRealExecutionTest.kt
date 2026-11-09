package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.migration.LegacySymptomsMigration
import com.example.data.model.FlowLevel
import com.example.data.model.Mood
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

/**
 * Pruebas de EJECUCIÓN REAL de LegacySymptomsMigration contra el emulador de Firestore
 * con reglas activas (firestore.rules) y autenticación real.
 *
 * Cubre los escenarios mínimos exigidos:
 * A. Migración normal con 3 docs destino (physical, mood, note).
 * B. Nota privada existente preservada (strict non-overwrite).
 * C. Categorías migradas parcialmente no bloquean a las restantes.
 * D. Idempotencia en segunda pasada (no duplica ni incrementa erróneamente).
 * E. dryRun=true (no escrituras, categoriesSimulated).
 * F. Rechazo o fallo controlado (no escrituras parciales, contadores exactos).
 * G. Concurrencia y contadores acumulados post-commit.
 * H. Autorización: regla estricta sobre private_notes.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LegacySymptomsMigrationRealExecutionTest {

  private lateinit var application: Application
  private lateinit var db: FirebaseFirestore
  private lateinit var auth: FirebaseAuth

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

  private suspend fun createAuthenticatedSpace(ownerEmail: String = "ella_${UUID.randomUUID()}@test.com"): Pair<String, String> {
    val authResult = auth.createUserWithEmailAndPassword(ownerEmail, "password123").await()
    val uid = authResult.user!!.uid
    val spaceId = "space_${UUID.randomUUID().toString().take(8)}"
    val spaceDoc = db.collection("couple_spaces").document(spaceId)
    val spacePayload = mapOf(
      "ownerId" to uid,
      "partnerId" to null,
      "status" to "UNPAIRED",
      "averageCycleLength" to 28,
      "averagePeriodLength" to 5,
      "sharingPermissions" to mapOf(
        "shareSymptoms" to true,
        "shareMood" to true,
        "shareIntimacy" to false,
        "sharePartnerNotes" to true
      ),
      "activeInviteVersion" to 1,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    spaceDoc.set(spacePayload).await()
    return Pair(spaceId, uid)
  }

  // --- ESCENARIO A: Migración normal con 3 docs destino ---
  @Test
  fun `Escenario A - migracion normal crea physical, mood y private note reales con migratedAt`() {
    runWithLooper {
      val (spaceId, uid) = createAuthenticatedSpace()
      val date = "2026-05-10"

      // Insertar síntoma legacy en el espacio
      val legacyDoc = db.collection("couple_spaces").document(spaceId)
        .collection("symptoms").document(date)
      legacyDoc.set(mapOf(
        "spaceId" to spaceId,
        "date" to date,
        "flowLevel" to FlowLevel.MEDIUM.name,
        "painLevel" to 3,
        "cramps" to true,
        "mood" to Mood.ANXIOUS.name,
        "energyLevel" to 2,
        "publicNote" to "Nota visible",
        "privateNote" to "Nota muy íntima",
        "isPrivate" to false,
        "loggedBy" to uid
      )).await()

      // Ejecutar la migración real en modo productivo (dryRun = false)
      val migration = LegacySymptomsMigration(db)
      val result = migration.migrateSpaceSymptoms(spaceId, dryRun = false)

      // Verificación de contadores de resultado
      assertEquals(1, result.totalLegacyRead)
      assertEquals(3, result.categoriesCreated)
      assertEquals(0, result.categoriesSkippedExisting)
      assertEquals(0, result.categoriesSimulated)
      assertEquals(1, result.physicalMigrated)
      assertEquals(1, result.moodMigrated)
      assertEquals(1, result.notesMigrated)
      assertEquals(0, result.errorsCount)

      // Verificación directa en base de datos de los 3 documentos destino
      val physDoc = db.collection("couple_spaces").document(spaceId)
        .collection("physical_symptoms").document(date).get().await()
      assertTrue("Physical symptoms document must exist in DB", physDoc.exists())
      assertEquals(FlowLevel.MEDIUM.name, physDoc.getString("flowLevel"))
      assertEquals(3L, physDoc.getLong("painLevel"))
      assertEquals(true, physDoc.getBoolean("cramps"))
      assertNotNull("migratedAt must be stamped on physical symptom", physDoc.getTimestamp("migratedAt"))

      val moodDoc = db.collection("couple_spaces").document(spaceId)
        .collection("mood_records").document(date).get().await()
      assertTrue("Mood record document must exist in DB", moodDoc.exists())
      assertEquals(Mood.ANXIOUS.name, moodDoc.getString("mood"))
      assertEquals(2L, moodDoc.getLong("energyLevel"))
      assertNotNull("migratedAt must be stamped on mood record", moodDoc.getTimestamp("migratedAt"))

      val noteDoc = db.collection("couple_spaces").document(spaceId)
        .collection("private_notes").document(date).get().await()
      assertTrue("Private note document must exist in DB", noteDoc.exists())
      assertEquals("Nota muy íntima", noteDoc.getString("privateNote"))
      assertNotNull("migratedAt must be stamped on private note", noteDoc.getTimestamp("migratedAt"))
    }
  }

  // --- ESCENARIO B: Nota privada existente preservada (strict non-overwrite) ---
  @Test
  fun `Escenario B - nota privada existente nunca es sobreescrita por la migracion`() {
    runWithLooper {
      val (spaceId, uid) = createAuthenticatedSpace()
      val date = "2026-05-11"

      // Pre-crear la nota privada en la nueva colección destino
      val noteDoc = db.collection("couple_spaces").document(spaceId)
        .collection("private_notes").document(date)
      noteDoc.set(mapOf(
        "spaceId" to spaceId,
        "date" to date,
        "privateNote" to "Texto original altamente confidencial preexistente",
        "loggedBy" to uid,
        "updatedAt" to FieldValue.serverTimestamp()
      )).await()

      // Insertar legacy con otro texto de nota
      val legacyDoc = db.collection("couple_spaces").document(spaceId)
        .collection("symptoms").document(date)
      legacyDoc.set(mapOf(
        "spaceId" to spaceId,
        "date" to date,
        "flowLevel" to FlowLevel.LIGHT.name,
        "privateNote" to "Texto legacy que NO debe sobreescribir",
        "loggedBy" to uid
      )).await()

      val migration = LegacySymptomsMigration(db)
      val result = migration.migrateSpaceSymptoms(spaceId, dryRun = false)

      // Verificación de contadores: nota privada fue SKIPPED
      assertEquals(1, result.notesSkipped)
      assertEquals(0, result.notesMigrated)
      assertEquals(1, result.physicalMigrated) // Físico sí se migró

      // Verificación en Firestore: el texto original NO ha cambiado
      val noteAfter = noteDoc.get().await()
      assertEquals(
        "Texto original altamente confidencial preexistente",
        noteAfter.getString("privateNote")
      )
    }
  }

  // --- ESCENARIO C: Categorías migradas parcialmente no bloquean las restantes ---
  @Test
  fun `Escenario C - categoria fisica existente no bloquea migracion de animo y notas`() {
    runWithLooper {
      val (spaceId, uid) = createAuthenticatedSpace()
      val date = "2026-05-12"

      // Pre-crear únicamente physical_symptoms
      val physDoc = db.collection("couple_spaces").document(spaceId)
        .collection("physical_symptoms").document(date)
      physDoc.set(mapOf(
        "spaceId" to spaceId,
        "date" to date,
        "flowLevel" to FlowLevel.NONE.name,
        "painLevel" to 0,
        "isPrivate" to true,
        "loggedBy" to uid,
        "updatedAt" to FieldValue.serverTimestamp()
      )).await()

      // Legacy contiene físico, ánimo y nota privada
      val legacyDoc = db.collection("couple_spaces").document(spaceId)
        .collection("symptoms").document(date)
      legacyDoc.set(mapOf(
        "spaceId" to spaceId,
        "date" to date,
        "flowLevel" to FlowLevel.HEAVY.name,
        "mood" to Mood.ENERGETIC.name,
        "energyLevel" to 5,
        "privateNote" to "Nueva nota privada a migrar",
        "loggedBy" to uid
      )).await()

      val migration = LegacySymptomsMigration(db)
      val result = migration.migrateSpaceSymptoms(spaceId, dryRun = false)

      // Físico debe saltarse, pero ánimo y nota DEBEN crearse con éxito
      assertEquals(1, result.physicalSkipped)
      assertEquals(0, result.physicalMigrated)
      assertEquals(1, result.moodMigrated)
      assertEquals(1, result.notesMigrated)
      assertEquals(2, result.categoriesCreated)
      assertEquals(1, result.categoriesSkippedExisting)

      // Verificar en BD que mood y note existen
      val moodSnap = db.collection("couple_spaces").document(spaceId)
        .collection("mood_records").document(date).get().await()
      assertTrue(moodSnap.exists())
      assertEquals(Mood.ENERGETIC.name, moodSnap.getString("mood"))

      val noteSnap = db.collection("couple_spaces").document(spaceId)
        .collection("private_notes").document(date).get().await()
      assertTrue(noteSnap.exists())
      assertEquals("Nueva nota privada a migrar", noteSnap.getString("privateNote"))
    }
  }

  // --- ESCENARIO D: Idempotencia en segunda pasada ---
  @Test
  fun `Escenario D - segunda pasada de migracion es estrictamente idempotente`() {
    runWithLooper {
      val (spaceId, uid) = createAuthenticatedSpace()
      val date = "2026-05-13"

      val legacyDoc = db.collection("couple_spaces").document(spaceId)
        .collection("symptoms").document(date)
      legacyDoc.set(mapOf(
        "spaceId" to spaceId,
        "date" to date,
        "flowLevel" to FlowLevel.SPOTTING.name,
        "mood" to Mood.SENSITIVE.name,
        "privateNote" to "Nota única",
        "loggedBy" to uid
      )).await()

      val migration = LegacySymptomsMigration(db)

      // Primera pasada
      val pass1 = migration.migrateSpaceSymptoms(spaceId, dryRun = false)
      assertEquals(3, pass1.categoriesCreated)
      assertEquals(0, pass1.categoriesSkippedExisting)

      // Segunda pasada idéntica
      val pass2 = migration.migrateSpaceSymptoms(spaceId, dryRun = false)
      assertEquals(0, pass2.categoriesCreated)
      assertEquals(3, pass2.categoriesSkippedExisting)
      assertEquals(0, pass2.categoriesSimulated)
      assertEquals(0, pass2.errorsCount)
    }
  }

  // --- ESCENARIO E: dryRun = true no escribe en la base de datos ---
  @Test
  fun `Escenario E - dryRun true no realiza escrituras y calcula categoriesSimulated`() {
    runWithLooper {
      val (spaceId, uid) = createAuthenticatedSpace()
      val date = "2026-05-14"

      val legacyDoc = db.collection("couple_spaces").document(spaceId)
        .collection("symptoms").document(date)
      legacyDoc.set(mapOf(
        "spaceId" to spaceId,
        "date" to date,
        "flowLevel" to FlowLevel.LIGHT.name,
        "mood" to Mood.HAPPY.name,
        "privateNote" to "Nota a simular",
        "loggedBy" to uid
      )).await()

      val migration = LegacySymptomsMigration(db)
      val dryResult = migration.migrateSpaceSymptoms(spaceId, dryRun = true)

      // Contadores en simulación
      assertEquals(0, dryResult.categoriesCreated)
      assertEquals(3, dryResult.categoriesSimulated)
      assertEquals(0, dryResult.categoriesSkippedExisting)
      assertEquals(1, dryResult.totalLegacyRead)

      // Verificar en BD que NINGUNA colección fue creada
      val physExists = db.collection("couple_spaces").document(spaceId)
        .collection("physical_symptoms").document(date).get().await().exists()
      val moodExists = db.collection("couple_spaces").document(spaceId)
        .collection("mood_records").document(date).get().await().exists()
      val noteExists = db.collection("couple_spaces").document(spaceId)
        .collection("private_notes").document(date).get().await().exists()

      assertFalse("Physical symptoms document must NOT exist after dryRun", physExists)
      assertFalse("Mood document must NOT exist after dryRun", moodExists)
      assertFalse("Private note document must NOT exist after dryRun", noteExists)
    }
  }

  // --- ESCENARIO F: Espacio no existente o denegado no genera falsos positivos ---
  @Test
  fun `Escenario F - espacio no existente retorna resultado vacio sin errores de conteo`() {
    runWithLooper {
      createAuthenticatedSpace() // sesión activa
      val nonExistentSpace = "space_inexistente_9999"
      val migration = LegacySymptomsMigration(db)
      val result = migration.migrateSpaceSymptoms(nonExistentSpace, dryRun = false)

      assertEquals(0, result.totalLegacyRead)
      assertEquals(0, result.categoriesCreated)
      assertEquals(0, result.categoriesSkippedExisting)
      assertEquals(1, result.errorsCount)
    }
  }

  // --- ESCENARIO G: Conflicto OCC concurrente real con reintento, preservación e idempotencia ---
  @Test
  fun `Escenario G - conflicto OCC entre lectura y commit provoca reintento sin sobreescritura ni duplicacion de contadores`() {
    runWithLooper {
      val (spaceId, uid) = createAuthenticatedSpace()
      val date = "2026-05-15"

      // Legacy doc contiene físico, estado de ánimo y nota privada
      val legacyDoc = db.collection("couple_spaces").document(spaceId)
        .collection("symptoms").document(date)
      legacyDoc.set(mapOf(
        "spaceId" to spaceId,
        "date" to date,
        "flowLevel" to FlowLevel.LIGHT.name,
        "painLevel" to 1,
        "mood" to Mood.CALM.name,
        "energyLevel" to 3,
        "privateNote" to "Nota privada legacy",
        "loggedBy" to uid
      )).await()

      var hookInvocations = 0
      val migration = LegacySymptomsMigration(db)
      migration.onAfterReadInTransactionHook = { hookDate, attempt ->
        hookInvocations++
        if (attempt == 1) {
          // Inyectamos una escritura concurrente externa sobre physical_symptoms
          // justo después de que la transacción leyó (physSnap no existía en attempt 1)
          // Esto causa un conflicto OCC garantizado al intentar el commit de attempt 1.
          runBlocking(Dispatchers.IO) {
            val physDoc = db.collection("couple_spaces").document(spaceId)
              .collection("physical_symptoms").document(hookDate)
            physDoc.set(mapOf(
              "spaceId" to spaceId,
              "date" to hookDate,
              "flowLevel" to FlowLevel.HEAVY.name,
              "painLevel" to 4,
              "isPrivate" to true,
              "loggedBy" to uid,
              "updatedAt" to FieldValue.serverTimestamp()
            )).await()
          }
        }
      }

      val result = migration.migrateSpaceSymptoms(spaceId, dryRun = false)

      // Verificamos que la transacción se reintentó (hook invocado al menos 2 veces)
      assertTrue("Transaction must have reattempted due to OCC conflict", hookInvocations >= 2)

      // Verificamos que el documento concurrente NO fue sobreescrito
      val physSnap = db.collection("couple_spaces").document(spaceId)
        .collection("physical_symptoms").document(date).get().await()
      assertEquals("Concurrent external write must NOT be overwritten", FlowLevel.HEAVY.name, physSnap.getString("flowLevel"))
      assertEquals(4L, physSnap.getLong("painLevel"))

      // Verificamos que las categorías restantes (mood y private_note) SÍ se migraron
      val moodSnap = db.collection("couple_spaces").document(spaceId)
        .collection("mood_records").document(date).get().await()
      assertTrue("Mood record must be migrated", moodSnap.exists())
      assertEquals(Mood.CALM.name, moodSnap.getString("mood"))

      val noteSnap = db.collection("couple_spaces").document(spaceId)
        .collection("private_notes").document(date).get().await()
      assertTrue("Private note must be migrated", noteSnap.exists())
      assertEquals("Nota privada legacy", noteSnap.getString("privateNote"))

      // Verificamos que los contadores NO se duplicaron por los reintentos:
      // physicalSkipped = 1, physicalMigrated = 0, moodMigrated = 1, notesMigrated = 1, categoriesCreated = 2
      assertEquals(0, result.physicalMigrated)
      assertEquals(1, result.physicalSkipped)
      assertEquals(1, result.moodMigrated)
      assertEquals(1, result.notesMigrated)
      assertEquals(2, result.categoriesCreated)
      assertEquals(1, result.categoriesSkippedExisting)
      assertEquals(0, result.errorsCount)

      // Verificamos idempotencia en una segunda pasada
      val idempotentResult = migration.migrateSpaceSymptoms(spaceId, dryRun = false)
      assertEquals(0, idempotentResult.categoriesCreated)
      assertEquals(3, idempotentResult.categoriesSkippedExisting)
      assertEquals(0, idempotentResult.errorsCount)
    }
  }

  // --- ESCENARIO H: Regla de autorización sobre notas privadas ---
  @Test
  fun `Escenario H - notas privadas migradas estan blindadas contra acceso no autorizado`() {
    runWithLooper {
      val (spaceId, ownerUid) = createAuthenticatedSpace()
      val date = "2026-05-16"

      // Crear nota privada
      val noteDoc = db.collection("couple_spaces").document(spaceId)
        .collection("private_notes").document(date)
      noteDoc.set(mapOf(
        "spaceId" to spaceId,
        "date" to date,
        "privateNote" to "Confidencial de Ella",
        "loggedBy" to ownerUid,
        "updatedAt" to FieldValue.serverTimestamp()
      )).await()

      // Iniciar sesión con un usuario extraño ajeno al espacio
      val stranger = auth.createUserWithEmailAndPassword("stranger_${UUID.randomUUID()}@test.com", "password123").await()
      assertNotNull(stranger.user)

      // Intentar leer la nota privada con el usuario ajeno
      var accessDenied = false
      try {
        db.collection("couple_spaces").document(spaceId)
          .collection("private_notes").document(date).get().await()
      } catch (e: Exception) {
        accessDenied = true
      }
      assertTrue("Stranger must be denied access to private notes by firestore.rules", accessDenied)
    }
  }

  // --- ESCENARIO I: Rechazo de commit por reglas de seguridad y rollback atómico completo ---
  @Test
  fun `Escenario I - rechazo de commit por reglas provoca rollback atomico completo sin escrituras parciales`() {
    runWithLooper {
      val (spaceId, uid) = createAuthenticatedSpace()
      val date = "2026-05-17"

      // Insertar legacy con datos inválidos según firestore.rules (painLevel = 99 excede el máximo permitido [0..5])
      val legacyDoc = db.collection("couple_spaces").document(spaceId)
        .collection("symptoms").document(date)
      legacyDoc.set(mapOf(
        "spaceId" to spaceId,
        "date" to date,
        "flowLevel" to FlowLevel.LIGHT.name,
        "painLevel" to 99, // Inválido por firestore.rules
        "mood" to Mood.HAPPY.name,
        "energyLevel" to 4,
        "privateNote" to "Nota que no debe persistirse si falla el commit",
        "loggedBy" to uid
      )).await()

      val migration = LegacySymptomsMigration(db)
      val result = migration.migrateSpaceSymptoms(spaceId, dryRun = false)

      // Verificación de contadores de error: 0 creados, 1 error registrado
      assertEquals(0, result.categoriesCreated)
      assertEquals(1, result.errorsCount)
      assertEquals(1, result.errorDetails.size)

      // Verificación directa en base de datos: NINGÚN documento parcial fue creado (rollback atómico total)
      val physExists = db.collection("couple_spaces").document(spaceId)
        .collection("physical_symptoms").document(date).get().await().exists()
      val moodExists = db.collection("couple_spaces").document(spaceId)
        .collection("mood_records").document(date).get().await().exists()
      val noteExists = db.collection("couple_spaces").document(spaceId)
        .collection("private_notes").document(date).get().await().exists()

      assertFalse("Physical symptoms document must NOT exist after rollback", physExists)
      assertFalse("Mood record document must NOT exist after rollback", moodExists)
      assertFalse("Private note document must NOT exist after rollback", noteExists)
    }
  }
}
