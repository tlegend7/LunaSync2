package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.model.PerceivedMood
import com.example.ui.dialogs.PartnerObservationDialog
import com.example.ui.theme.LunaSyncTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ObservationFormComposeTest {
  @get:Rule val compose = createComposeRule()

  @Test fun recordingCareDoesNotInventMoodOrPainAndKeepsItsDraftOnFailure() {
    var completion: ((Boolean, String?) -> Unit)? = null
    var capturedMood: PerceivedMood? = PerceivedMood.TIRED
    var capturedPain: Int? = 1
    var capturedAction: String? = null
    var dismissed = false
    compose.setContent {
      LunaSyncTheme(darkTheme = true) {
        PartnerObservationDialog({ dismissed = true }, { mood, pain, _, action, _, done ->
          capturedMood = mood; capturedPain = pain; capturedAction = action; completion = done
        })
      }
    }
    compose.onNodeWithTag("save_observation_button").assertIsNotEnabled()
    compose.onNodeWithTag("observation_care_action_input").performScrollTo().performTextInput("Preparé la cena")
    compose.onNodeWithTag("save_observation_button").assertIsDisplayed().performClick()
    assertNull(capturedMood)
    assertNull(capturedPain)
    assertEquals("Preparé la cena", capturedAction)
    assertFalse(dismissed)
    compose.onNodeWithTag("observation_care_action_input").assertIsNotEnabled()
    compose.runOnIdle { completion!!(false, "No se pudo guardar. Reintenta.") }
    compose.onNodeWithText("No se pudo guardar. Reintenta.").assertIsDisplayed()
    compose.onNodeWithTag("observation_care_action_input").performScrollTo().assertTextContains("Preparé la cena")
    compose.onNodeWithTag("save_observation_button").performClick()
    compose.runOnIdle { completion!!(true, null) }
    assertTrue(dismissed)
  }
}
