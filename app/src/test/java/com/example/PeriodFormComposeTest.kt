package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.model.CycleEntry
import com.example.ui.dialogs.PeriodLogDialog
import com.example.ui.theme.LunaSyncTheme
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PeriodFormComposeTest {
  @get:Rule val compose = createComposeRule()
  private val day = LocalDate.of(2026, 6, 15)
  private val entry = CycleEntry(id = "period-one-day", startDate = day.toString(), endDate = day.toString(), periodDuration = 1)

  @Test fun oneDayBleedingKeepsBothExplicitDatesDuringSaveAndRetry() {
    var completion: ((Boolean, String?) -> Unit)? = null
    var captured: Pair<LocalDate, LocalDate?>? = null
    var dismissed = false
    compose.setContent {
      LunaSyncTheme {
        PeriodLogDialog(day, entry, day, { dismissed = true }, { start, end, done ->
          captured = start to end; completion = done
        })
      }
    }
    compose.onNodeWithText("1 día registrado").performScrollTo().assertIsDisplayed()
    compose.onNodeWithTag("save_period_button").performClick()
    assertEquals(day to day, captured)
    assertFalse(dismissed)
    compose.onNodeWithTag("pick_start_date_button").assertIsNotEnabled()
    compose.onNodeWithTag("save_period_button").assertIsNotEnabled()
    compose.runOnIdle { completion!!(false, "Sin conexión. Reintenta.") }
    compose.onNodeWithText("Sin conexión. Reintenta.").assertIsDisplayed()
    compose.onNodeWithTag("save_period_button").performClick()
    assertEquals(day to day, captured)
    compose.runOnIdle { completion!!(true, null) }
    assertTrue(dismissed)
  }

  @Test fun deletingRequiresConfirmationAndWaitsForSuccessWithoutLosingItsRetry() {
    var calls = 0
    var completion: ((Boolean, String?) -> Unit)? = null
    var dismissed = false
    compose.setContent {
      LunaSyncTheme(darkTheme = true) {
        PeriodLogDialog(day, entry, day, { dismissed = true }, { _, _, _ -> }, onDelete = { done ->
          calls++; completion = done
        })
      }
    }
    compose.onNodeWithTag("delete_period_button").performScrollTo().performClick()
    assertEquals(0, calls)
    compose.onNodeWithTag("confirm_delete_period_button").performScrollTo().performClick()
    compose.onNodeWithTag("confirm_delete_period_button").assertIsNotEnabled()
    compose.onNodeWithTag("cancel_delete_period_button").assertIsNotEnabled()
    assertEquals(1, calls)
    assertFalse(dismissed)
    compose.runOnIdle { completion!!(false, "No se pudo eliminar.") }
    compose.onNodeWithText("No se pudo eliminar.").performScrollTo().assertIsDisplayed()
    compose.onNodeWithTag("confirm_delete_period_button").performScrollTo().performClick()
    assertEquals(2, calls)
    compose.runOnIdle { completion!!(true, null) }
    assertTrue(dismissed)
  }
}
