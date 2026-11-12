package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LunaLightColorScheme = lightColorScheme(
  primary = LunaNavyPrimary,
  onPrimary = Color.White,
  primaryContainer = LunaIceBlueSubtle,
  onPrimaryContainer = LunaNavyPrimary,
  secondary = LunaNavyPrimary,
  onSecondary = Color.White,
  secondaryContainer = LunaIceBlueSurface,
  onSecondaryContainer = LunaNavyDeep,
  tertiary = LunaSageText,
  onTertiary = Color.White,
  background = LunaBackgroundLight,
  onBackground = LunaTextPrimary,
  surface = LunaSurfaceLight,
  onSurface = LunaTextPrimary,
  surfaceVariant = LunaIceBlueSurface,
  onSurfaceVariant = LunaTextSecondary,
  outline = LunaBorderLight,
  outlineVariant = LunaBorderLight,
  error = LunaCoralText,
  onError = Color.White,
  errorContainer = LunaDangerSoft,
  onErrorContainer = LunaCoralText
)

private val LunaDarkColorScheme = darkColorScheme(
  primary = LunaIceBlueAccent,
  onPrimary = LunaNavyDeep,
  primaryContainer = LunaIceDarkSoft,
  onPrimaryContainer = LunaIceBlueAccent,
  secondary = LunaIceBlueText,
  onSecondary = LunaNavyDeep,
  secondaryContainer = LunaDarkSurfaceCard,
  onSecondaryContainer = LunaDarkTextPrimary,
  tertiary = LunaSageDarkText,
  onTertiary = LunaSageDarkSoft,
  background = LunaDarkBackground,
  onBackground = LunaDarkTextPrimary,
  surface = LunaDarkSurface,
  onSurface = LunaDarkTextPrimary,
  surfaceVariant = LunaDarkSurfaceCard,
  onSurfaceVariant = LunaDarkTextSecondary,
  outline = LunaNavyBorderDark,
  outlineVariant = LunaNavyBorderDark,
  error = LunaDangerDarkText,
  onError = LunaDangerDarkSoft,
  errorContainer = LunaDangerDarkSoft,
  onErrorContainer = LunaDangerDarkText
)

@Composable
fun LunaSyncTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) LunaDarkColorScheme else LunaLightColorScheme
  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  LunaSyncTheme(darkTheme = darkTheme, content = content)
}
