package com.example.data.preferences

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppAppearanceMode {
  SYSTEM, LIGHT, DARK;

  fun isDark(systemDark: Boolean): Boolean = when (this) {
    SYSTEM -> systemDark
    LIGHT -> false
    DARK -> true
  }
}

/** Device preference, retained when changing accounts or leaving the demonstration. */
class AppearancePreferences(context: Context) {
  private val preferences = context.applicationContext.getSharedPreferences("appearance", Context.MODE_PRIVATE)
  private val storedMode = preferences.getString("mode", null)
  private val _mode = MutableStateFlow(AppAppearanceMode.entries.firstOrNull { it.name == storedMode }
    ?: AppAppearanceMode.SYSTEM)
  val mode = _mode.asStateFlow()

  fun select(mode: AppAppearanceMode) {
    preferences.edit().putString("mode", mode.name).apply()
    _mode.value = mode
  }
}
