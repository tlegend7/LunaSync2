package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.model.DailySymptom
import com.example.ui.dialogs.SymptomLogDialog
import com.example.ui.theme.LunaSyncTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SymptomPrivacyComposeTest {
  @get:Rule val compose = createComposeRule()

  @Test fun physicalAndMoodSharingCanBeChosenSeparatelyAndSurviveAFailedSave() {
    var result: Pair<Boolean, Boolean>? = null
    var completion: ((Boolean, String?) -> Unit)? = null
    compose.setContent {
      LunaSyncTheme(darkTheme = true) {
        SymptomLogDialog(DailySymptom(isPrivate = false, isMoodPrivate = true), onDismiss = {},
          onSave = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, physicalPrivate, moodPrivate, done ->
            result = physicalPrivate to moodPrivate; completion = done
          })
      }
    }
    compose.onNodeWithTag("symptom_privacy_switch").performScrollTo().assertIsOn().performClick()
    compose.onNodeWithTag("mood_privacy_switch").performScrollTo().assertIsOff().performClick()
    compose.onNodeWithTag("save_symptom_button").performClick()
    assertEquals(true to false, result)
    compose.runOnIdle { completion!!(false, "Reintenta la conexión.") }
    compose.onNodeWithText("Reintenta la conexión.").assertIsDisplayed()
    compose.onNodeWithTag("symptom_privacy_switch").performScrollTo().assertIsOff()
    compose.onNodeWithTag("mood_privacy_switch").performScrollTo().assertIsOn()
    compose.onNodeWithTag("save_symptom_button").performClick()
    assertEquals(true to false, result)
  }
}
