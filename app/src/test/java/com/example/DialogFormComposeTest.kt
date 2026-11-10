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
import com.example.data.model.CheckInPromptType
import com.example.data.model.PerceivedMood
import com.example.ui.dialogs.IntimacyLogDialog
import com.example.ui.dialogs.PartnerObservationDialog
import com.example.ui.dialogs.PeriodLogDialog
import com.example.ui.dialogs.SendCheckInDialog
import com.example.ui.dialogs.SymptomLogDialog
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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

  // ==========================================
  // PRUEBAS DE PeriodLogDialog
  // ==========================================

  @Test
  fun `PeriodLogDialog - flujo completo de guardado, persistencia en error y cierre en reintento exitoso`() {
    var onDismissCalled = false
    var onSaveCallbackHolder: ((Boolean, String?) -> Unit)? = null
    var capturedStartDate: LocalDate? = null
    var capturedEndDate: LocalDate? = null

    val testStartDate = LocalDate.of(2026, 5, 10)
    val testToday = LocalDate.of(2026, 5, 20)

    composeTestRule.setContent {
      PeriodLogDialog(
        initialDate = testStartDate,
        existingCycle = null,
        today = testToday,
        onDismiss = { onDismissCalled = true },
        onSave = { start, end, onComplete ->
          capturedStartDate = start
          capturedEndDate = end
          onSaveCallbackHolder = onComplete
        }
      )
    }

    // 1. Verificar renderizado del diálogo de periodo
    composeTestRule.onNodeWithText("Registrar Menstruación").assertIsDisplayed()

    // 2. Activar switch de sangrado finalizado
    composeTestRule.onNodeWithTag("period_finished_switch")
      .performScrollTo()
      .performClick()

    // 3. Pulsar botón de guardar periodo
    composeTestRule.onNodeWithTag("save_period_button")
      .assertIsEnabled()
      .performClick()

    // 4. Verificar que se capturó la fecha y que no se cerró prematuramente
    assertFalse("Dialog must not be dismissed while saving", onDismissCalled)
    assertEquals(testStartDate, capturedStartDate)
    assertNotNull("End date must be populated when finished bleeding is true", capturedEndDate)

    // 5. Simular error del backend
    val serverError = "Error al persistir el ciclo en la base de datos"
    composeTestRule.runOnUiThread {
      onSaveCallbackHolder?.invoke(false, serverError)
    }
    composeTestRule.waitForIdle()

    // 6. Verificar que el diálogo permanece abierto y el error es visible
    assertFalse("Dialog must remain open on error", onDismissCalled)
    composeTestRule.onNodeWithText(serverError).performScrollTo().assertIsDisplayed()

    // 7. Reintentar guardado
    composeTestRule.onNodeWithTag("save_period_button")
      .assertIsEnabled()
      .performClick()

    // 8. Confirmar éxito del servidor
    composeTestRule.runOnUiThread {
      onSaveCallbackHolder?.invoke(true, null)
    }
    composeTestRule.waitForIdle()

    // 9. Verificar que se cerró el diálogo mediante onDismiss
    assertTrue("Dialog must be dismissed after successful retry", onDismissCalled)
  }

  // ==========================================
  // PRUEBAS DE PartnerObservationDialog
  // ==========================================

  @Test
  fun `PartnerObservationDialog - flujo completo de guardado asincrono, preservacion en error y cierre en exito`() {
    var onDismissCalled = false
    var onSaveCallbackHolder: ((Boolean, String?) -> Unit)? = null
    var saveCallCount = 0
    var capturedMood: PerceivedMood? = null
    var capturedPain: Int? = null
    var capturedSpotting: Boolean? = null
    var capturedAction: String? = null
    var capturedMessage: String? = null

    composeTestRule.setContent {
      PartnerObservationDialog(
        onDismiss = { onDismissCalled = true },
        onSave = { mood, pain, spotting, action, msg, onComplete ->
          saveCallCount++
          capturedMood = mood
          capturedPain = pain
          capturedSpotting = spotting
          capturedAction = action
          capturedMessage = msg
          onSaveCallbackHolder = onComplete
        }
      )
    }

    // 1. Verificar renderizado
    composeTestRule.onNodeWithText("Registrar Observación y Apoyo").assertIsDisplayed()

    // 2. Elegir un estado de ánimo distinto del predeterminado (por defecto es TIRED)
    composeTestRule.onNodeWithTag("mood_chip_${PerceivedMood.CALM.name}").performClick()

    // 3. Modificar manchado mediante el switch
    composeTestRule.onNodeWithTag("spotting_switch").performClick()

    // 4. Escribir acción de cuidado
    val expectedAction = "Le preparé infusión de jengibre y ventilé la habitación"
    composeTestRule.onNodeWithTag("observation_care_action_input")
      .performScrollTo()
      .performTextInput(expectedAction)

    // 5. Escribir mensaje cariñoso de apoyo
    val expectedMessage = "Aquí estoy para lo que necesites, descansa hoy"
    composeTestRule.onNodeWithTag("observation_sweet_message_input")
      .performScrollTo()
      .performTextInput(expectedMessage)

    // 6. Pulsar botón de guardado y verificar que pulsaciones consecutivas no duplican envíos
    composeTestRule.onNodeWithTag("save_observation_button")
      .performScrollTo()
      .assertIsEnabled()
      .performClick()

    composeTestRule.onNodeWithTag("save_observation_button")
      .performClick()

    assertEquals("Pulsar repetidamente no debe duplicar llamadas de guardado", 1, saveCallCount)
    assertFalse("Dialog must not dismiss while operation is in flight", onDismissCalled)
    composeTestRule.onNodeWithTag("save_observation_button").assertIsNotEnabled()

    // 7. Simular error controlado de servidor
    val errorMsg = "Problema de conexión con el servidor. Verifica tu internet e intenta nuevamente."
    composeTestRule.runOnUiThread {
      onSaveCallbackHolder?.invoke(false, errorMsg)
    }
    composeTestRule.waitForIdle()

    // 8. Comprobar que todos los valores permanecen intactos tras el error
    assertFalse("Dialog must remain open on failure", onDismissCalled)
    composeTestRule.onNodeWithText(errorMsg).assertIsDisplayed()
    composeTestRule.onNodeWithTag("observation_care_action_input")
      .performScrollTo()
      .assertTextContains(expectedAction)
    composeTestRule.onNodeWithTag("observation_sweet_message_input")
      .performScrollTo()
      .assertTextContains(expectedMessage)
    composeTestRule.onNodeWithTag("save_observation_button")
      .performScrollTo()
      .assertIsEnabled()

    // 9. Reintentar guardado
    composeTestRule.onNodeWithTag("save_observation_button").performClick()
    assertEquals("El reintento debe ejecutar una segunda llamada", 2, saveCallCount)
    assertEquals(PerceivedMood.CALM, capturedMood)
    assertTrue("Manchado debe seguir siendo verdadero en el reintento", capturedSpotting == true)
    assertEquals(expectedAction, capturedAction)
    assertEquals(expectedMessage, capturedMessage)

    // 10. Simular éxito en el segundo intento
    composeTestRule.runOnUiThread {
      onSaveCallbackHolder?.invoke(true, null)
    }
    composeTestRule.waitForIdle()

    // 11. Verificar cierre exitoso
    assertTrue("Dialog must dismiss after successful completion", onDismissCalled)
  }

  // ==========================================
  // PRUEBAS DE SendCheckInDialog
  // ==========================================

  @Test
  fun `SendCheckInDialog - flujo completo de envio, persistencia en error y cierre en exito`() {
    var onDismissCalled = false
    var onSendCallbackHolder: ((Boolean, String?) -> Unit)? = null
    var sendCallCount = 0
    var capturedPrompt: CheckInPromptType? = null
    var capturedNote: String? = null

    composeTestRule.setContent {
      SendCheckInDialog(
        onDismiss = { onDismissCalled = true },
        onSend = { prompt, note, onComplete ->
          sendCallCount++
          capturedPrompt = prompt
          capturedNote = note
          onSendCallbackHolder = onComplete
        }
      )
    }

    // 1. Verificar renderizado
    composeTestRule.onNodeWithText("Enviar Check-in Afectivo").assertIsDisplayed()

    // 2. Seleccionar una pregunta cariñosa distinta de la predeterminada
    composeTestRule.onNodeWithTag("prompt_${CheckInPromptType.CAN_I_BRING_YOU_SOMETHING.name}")
      .performClick()

    // 3. Escribir nota personalizada
    val expectedNote = "¿Quieres una infusión tibia o algo rico para picar?"
    composeTestRule.onNodeWithTag("checkin_custom_note_input")
      .performScrollTo()
      .performTextInput(expectedNote)

    // 4. Pulsar enviar pregunta y verificar que pulsar repetidamente no duplica
    composeTestRule.onNodeWithTag("send_check_in_button")
      .assertIsEnabled()
      .performClick()

    composeTestRule.onNodeWithTag("send_check_in_button").performClick()
    assertEquals("Pulsar repetidamente no debe duplicar llamadas de envío", 1, sendCallCount)
    assertFalse("Dialog must not dismiss immediately", onDismissCalled)
    composeTestRule.onNodeWithTag("send_check_in_button").assertIsNotEnabled()

    // 5. Simular error controlado
    val checkInError = "Problema de conexión con el servidor. Verifica tu internet e intenta nuevamente."
    composeTestRule.runOnUiThread {
      onSendCallbackHolder?.invoke(false, checkInError)
    }
    composeTestRule.waitForIdle()

    // 6. Verificar que el diálogo permanece abierto y la nota se conserva intacta
    assertFalse("Dialog must remain open on error", onDismissCalled)
    composeTestRule.onNodeWithText(checkInError).assertIsDisplayed()
    composeTestRule.onNodeWithTag("checkin_custom_note_input")
      .performScrollTo()
      .assertTextContains(expectedNote)
    composeTestRule.onNodeWithTag("send_check_in_button").assertIsEnabled()

    // 7. Reintentar con éxito
    composeTestRule.onNodeWithTag("send_check_in_button").performClick()
    assertEquals("El reintento debe ejecutar una segunda llamada", 2, sendCallCount)
    assertEquals(CheckInPromptType.CAN_I_BRING_YOU_SOMETHING, capturedPrompt)
    assertEquals(expectedNote, capturedNote)

    composeTestRule.runOnUiThread {
      onSendCallbackHolder?.invoke(true, null)
    }
    composeTestRule.waitForIdle()

    // 8. Verificar cierre tras éxito
    assertTrue("Dialog must dismiss on success", onDismissCalled)
  }
}
