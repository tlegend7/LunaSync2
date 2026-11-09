package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.example.ui.dialogs.IntimacyLogDialog
import com.example.ui.dialogs.SymptomLogDialog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Pruebas de EJECUCIÓN REAL de interfaz de usuario Compose para SymptomLogDialog e IntimacyLogDialog.
 * Ejecuta los componentes de producción completos en un entorno Robolectric JVM con ComposeTestRule:
 * - Introducción de datos en los campos de entrada reales.
 * - Pulsación sobre botones de guardado reales.
 * - Verificación de spinner/bloqueo de controles durante la operación asíncrona.
 * - Comprobación de que ante error de red o de servidor, los inputs introducidos se conservan intactos
 *   y el diálogo permanece abierto con el banner de error visible.
 * - Comprobación de que tras reintento y confirmación de éxito, el diálogo se cierra mediante onDismiss.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DialogFormComposeTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  // ==========================================
  // PRUEBAS DE SymptomLogDialog
  // ==========================================

  @Test
  fun `SymptomLogDialog - flujo completo de inputs, estado de guardado, preservacion de datos en error y cierre en exito`() {
    var onDismissCalled = false
    var onSaveCallbackHolder: ((Boolean, String?) -> Unit)? = null
    var capturedPublicNote: String? = null
    var capturedPrivateNote: String? = null

    composeTestRule.setContent {
      SymptomLogDialog(
        initialSymptom = null,
        onDismiss = { onDismissCalled = true },
        onSave = { _, _, _, _, _, _, _, _, _, _, _, _, publicNote, privateNote, _, onComplete ->
          capturedPublicNote = publicNote
          capturedPrivateNote = privateNote
          onSaveCallbackHolder = onComplete
        }
      )
    }

    // 1. Verificar que el diálogo se renderiza
    composeTestRule.onNodeWithText("Registro Holístico Diario").assertIsDisplayed()

    // 2. Introducir texto en la nota pública
    val inputPublic = "Dolor lumbar persistente y fatiga moderada"
    composeTestRule.onNodeWithTag("symptom_public_note_input")
      .performScrollTo()
      .performTextInput(inputPublic)

    // 3. Introducir texto en la nota privada
    val inputPrivate = "Nota confidencial para mi seguimiento íntimo"
    composeTestRule.onNodeWithTag("symptom_private_note_input")
      .performScrollTo()
      .performTextInput(inputPrivate)

    // 4. Pulsar el botón de guardado
    composeTestRule.onNodeWithTag("save_symptom_button")
      .assertIsEnabled()
      .performClick()

    // 5. Verificar que el botón se deshabilitó durante el guardado
    composeTestRule.onNodeWithTag("save_symptom_button").assertIsNotEnabled()
    assertFalse("Dialog must not be dismissed while saving", onDismissCalled)
    assertEquals(inputPublic, capturedPublicNote)
    assertEquals(inputPrivate, capturedPrivateNote)

    // 6. Simular fallo de servidor (onComplete(false, error))
    val serverErrorMsg = "Fallo de conexión al sincronizar con el servidor"
    composeTestRule.runOnUiThread {
      onSaveCallbackHolder?.invoke(false, serverErrorMsg)
    }
    composeTestRule.waitForIdle()

    // 7. Verificar que el diálogo NO se cerró y el error está visible
    assertFalse("Dialog must NOT be dismissed on error", onDismissCalled)
    composeTestRule.onNodeWithText(serverErrorMsg).performScrollTo().assertIsDisplayed()

    // 8. CRÍTICO: Verificar que los inputs introducidos por la usuaria se conservan intactos
    composeTestRule.onNodeWithTag("symptom_public_note_input").performScrollTo().assertTextContains(inputPublic)
    composeTestRule.onNodeWithTag("symptom_private_note_input").performScrollTo().assertTextContains(inputPrivate)

    // 9. El botón de guardado vuelve a estar habilitado para permitir reintento
    composeTestRule.onNodeWithTag("save_symptom_button")
      .assertIsEnabled()
      .performClick()

    // 10. Simular reintento exitoso
    composeTestRule.runOnUiThread {
      onSaveCallbackHolder?.invoke(true, null)
    }
    composeTestRule.waitForIdle()

    // 11. Verificar que tras el éxito, onDismiss es invocado y el diálogo se cierra
    assertTrue("Dialog must be dismissed on server success", onDismissCalled)
  }

  // ==========================================
  // PRUEBAS DE IntimacyLogDialog
  // ==========================================

  @Test
  fun `IntimacyLogDialog - flujo completo de inputs, retencion de datos en fallo de red y dismiss en confirmacion`() {
    var onDismissCalled = false
    var onSaveCallbackHolder: ((Boolean, String?) -> Unit)? = null
    var capturedNotes: String? = null

    composeTestRule.setContent {
      IntimacyLogDialog(
        onDismiss = { onDismissCalled = true },
        onSave = { _, _, _, _, notes, _, onComplete ->
          capturedNotes = notes
          onSaveCallbackHolder = onComplete
        }
      )
    }

    // 1. Verificar renderizado
    composeTestRule.onNodeWithText("Registro Íntimo & Contacto").assertIsDisplayed()

    // 2. Introducir notas íntimas
    val inputNotes = "Encuentro cariñoso y relajado con uso de preservativo"
    composeTestRule.onNodeWithTag("intimacy_notes_input")
      .performScrollTo()
      .performTextInput(inputNotes)

    // 3. Pulsar botón de guardar encuentro
    composeTestRule.onNodeWithTag("save_intimacy_button")
      .assertIsEnabled()
      .performClick()

    // 4. Verificar estado de guardado
    composeTestRule.onNodeWithTag("save_intimacy_button").assertIsNotEnabled()
    assertFalse(onDismissCalled)
    assertEquals(inputNotes, capturedNotes)

    // 5. Simular error de red
    val networkError = "Error de red al registrar encuentro"
    composeTestRule.runOnUiThread {
      onSaveCallbackHolder?.invoke(false, networkError)
    }
    composeTestRule.waitForIdle()

    // 6. Verificar persistencia de campos y mensaje de error
    assertFalse("Dialog must remain open on error", onDismissCalled)
    composeTestRule.onNodeWithText(networkError).performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("intimacy_notes_input").performScrollTo().assertTextContains(inputNotes)

    // 7. Reintentar guardado
    composeTestRule.onNodeWithTag("save_intimacy_button")
      .assertIsEnabled()
      .performClick()

    // 8. Confirmar éxito del servidor
    composeTestRule.runOnUiThread {
      onSaveCallbackHolder?.invoke(true, null)
    }
    composeTestRule.waitForIdle()

    // 9. Verificar cierre del diálogo
    assertTrue("Dialog must be dismissed after success", onDismissCalled)
  }
}
