package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

/** Exercises the actual Activity and ViewModel; demo writes are deliberately memory-only. */
class NativeDemoFlowTest {
  @get:Rule val compose = createAndroidComposeRule<MainActivity>()

  @Test fun recordPrivateDataSwitchRolesAndAnswerACheckIn() {
    compose.onNodeWithTag("role_ella_chip").performScrollTo().performClick()
    compose.onNodeWithTag("guest_demo_mode_button").performScrollTo().performClick()
    compose.onNodeWithTag("log_symptoms_cta").performScrollTo().performClick()
    compose.onNodeWithTag("symptom_private_note_input").performScrollTo().performTextReplacement("Solo para mí: prueba nativa")
    compose.onNodeWithTag("save_symptom_button").performClick()
    compose.onNodeWithTag("symptom_log_dialog").assertDoesNotExist()

    compose.onNodeWithTag("nav_tab_calendar").performClick()
    compose.onNodeWithTag("selected_private_note").performScrollTo().assertTextContains("Solo para mí: prueba nativa")
    compose.onNodeWithTag("nav_tab_dashboard").performClick()
    compose.onNodeWithTag("log_intimacy_cta").performScrollTo().performClick()
    compose.onNodeWithTag("intimacy_notes_input").performScrollTo().performTextInput("Encuentro privado: prueba nativa")
    compose.onNodeWithTag("save_intimacy_button").performClick()
    compose.onNodeWithTag("intimacy_log_dialog").assertDoesNotExist()

    compose.onNodeWithTag("switch_to_partner").performClick()
    compose.onNodeWithText("Solo para mí: prueba nativa", substring = true).assertDoesNotExist()
    compose.onNodeWithText("Encuentro privado: prueba nativa", substring = true).assertDoesNotExist()
    compose.onNodeWithTag("send_check_in_cta").performScrollTo().performClick()
    compose.onNodeWithTag("send_checkin_dialog").assertIsDisplayed()
    compose.onNodeWithTag("prompt_NEED_SPACE_OR_CUDDLES").performScrollTo().performClick()
    compose.onNodeWithTag("checkin_custom_note_input").performScrollTo().performTextInput("¿Te apetece compañía?")
    compose.onNodeWithTag("send_check_in_button").performClick()
    compose.onNodeWithTag("send_checkin_dialog").assertDoesNotExist()

    compose.onNodeWithTag("switch_to_ella").performClick()
    compose.onNodeWithTag("open_reply_button").performScrollTo().performClick()
    compose.onNodeWithTag("reply_option_NEED_SILENCE").performScrollTo().performClick()
    compose.onNodeWithTag("reply_note_input").performScrollTo().performTextInput("Un rato tranquila, gracias.")
    compose.onNodeWithTag("submit_reply_button").performClick()
    compose.onNodeWithTag("checkin_response_dialog").assertDoesNotExist()
    compose.onNodeWithTag("switch_to_partner").performClick()
    compose.onNodeWithText("Un rato tranquila, gracias.").performScrollTo().assertIsDisplayed()
    compose.onNodeWithTag("log_observation_cta").performScrollTo().performClick()
    compose.onNodeWithTag("observation_care_action_input").performScrollTo().performTextInput("Preparé una infusión.")
    compose.onNodeWithTag("save_observation_button").performClick()
    compose.onNodeWithTag("partner_observation_dialog").assertDoesNotExist()
  }
}
