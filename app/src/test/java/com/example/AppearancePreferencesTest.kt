package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.preferences.AppAppearanceMode
import com.example.data.preferences.AppearancePreferences
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppearancePreferencesTest {
  private val context = ApplicationProvider.getApplicationContext<Context>()

  @Before fun reset() {
    context.getSharedPreferences("appearance", Context.MODE_PRIVATE).edit().clear().commit()
  }

  @Test fun choosingAnAppearanceSurvivesRecreatingItsOwner() {
    val first = AppearancePreferences(context)
    assertEquals(AppAppearanceMode.SYSTEM, first.mode.value)
    first.select(AppAppearanceMode.DARK)
    assertEquals(AppAppearanceMode.DARK, first.mode.value)
    val afterRecreation = AppearancePreferences(context)
    assertEquals(AppAppearanceMode.DARK, afterRecreation.mode.value)
    assertTrue(afterRecreation.mode.value.isDark(systemDark = false))
    afterRecreation.select(AppAppearanceMode.LIGHT)
    assertFalse(AppearancePreferences(context).mode.value.isDark(systemDark = true))
    afterRecreation.select(AppAppearanceMode.SYSTEM)
    assertTrue(afterRecreation.mode.value.isDark(systemDark = true))
    assertFalse(afterRecreation.mode.value.isDark(systemDark = false))
  }

  @Test fun unknownStoredValueFallsBackToSystem() {
    context.getSharedPreferences("appearance", Context.MODE_PRIVATE).edit().putString("mode", "REMOVED").commit()
    assertEquals(AppAppearanceMode.SYSTEM, AppearancePreferences(context).mode.value)
  }
}
