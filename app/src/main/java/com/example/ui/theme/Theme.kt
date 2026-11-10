package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppleLightColorScheme = lightColorScheme(
  primary = BrandTerracottaRose,
  onPrimary = Color.White,
  primaryContainer = BrandTerracottaRose.copy(alpha = 0.12f),
  onPrimaryContainer = BrandTerracottaRose,
  secondary = BrandWarmRose,
  onSecondary = Color.White,
  secondaryContainer = AppleSystemGray5,
  onSecondaryContainer = AppleTextPrimary,
  tertiary = PhaseFertileSage,
  onTertiary = Color.White,
  background = AppleBackground,
  onBackground = AppleTextPrimary,
  surface = AppleSurface,
  onSurface = AppleTextPrimary,
  surfaceVariant = AppleSystemGray6,
  onSurfaceVariant = AppleTextSecondary,
  outline = AppleHairlineBorder,
  outlineVariant = AppleHairlineBorder
)

private val AppleDarkColorScheme = darkColorScheme(
  primary = BrandTerracottaRose,
  onPrimary = Color.White,
  primaryContainer = BrandTerracottaRose.copy(alpha = 0.25f),
  onPrimaryContainer = Color.White,
  secondary = BrandWarmRose,
  onSecondary = Color.White,
  secondaryContainer = AppleDarkSurfaceElevated,
  onSecondaryContainer = AppleDarkTextPrimary,
  tertiary = PhaseFertileSage,
  onTertiary = Color.White,
  background = AppleDarkBackground,
  onBackground = AppleDarkTextPrimary,
  surface = AppleDarkSurface,
  onSurface = AppleDarkTextPrimary,
  surfaceVariant = AppleDarkSurfaceElevated,
  onSurfaceVariant = AppleDarkTextSecondary,
  outline = AppleDarkHairlineBorder,
  outlineVariant = AppleDarkHairlineBorder
)

@Composable
fun LunaSyncTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) AppleDarkColorScheme else AppleLightColorScheme
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
