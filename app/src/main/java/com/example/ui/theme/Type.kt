package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// LunaSync Typography - Inter scale (Figma: LunaSync / Sistema visual)
val LunaFontFamily = FontFamily.Default

val Typography = Typography(
  // Hero headline ("Día 18", "LunaSync")
  displayLarge = TextStyle(
    fontFamily = LunaFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 44.sp,
    lineHeight = 48.sp,
    letterSpacing = (-0.5).sp
  ),
  displaySmall = TextStyle(
    fontFamily = LunaFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 32.sp,
    lineHeight = 38.sp,
    letterSpacing = (-0.4).sp
  ),
  // Heading ("Hoy, a tu ritmo", "Tu calendario", "Estar, sin adivinar")
  headlineMedium = TextStyle(
    fontFamily = LunaFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 32.sp,
    lineHeight = 38.sp,
    letterSpacing = (-0.3).sp
  ),
  // Section Titles ("Tus registros", "¿Cómo usarás LunaSync?", "Lo que sabes hoy")
  titleLarge = TextStyle(
    fontFamily = LunaFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 22.sp,
    lineHeight = 28.sp,
    letterSpacing = (-0.2).sp
  ),
  // Card Titles & Row Headers ("Menstruación", "Intimidad", "Privacidad y permisos")
  titleMedium = TextStyle(
    fontFamily = LunaFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp,
    lineHeight = 23.sp,
    letterSpacing = (-0.1).sp
  ),
  // Small Headers
  titleSmall = TextStyle(
    fontFamily = LunaFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 15.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.sp
  ),
  // Body Regular
  bodyLarge = TextStyle(
    fontFamily = LunaFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 23.sp,
    letterSpacing = 0.sp
  ),
  // Body Regular Subtitle ("Las fechas pueden variar", "Acompañar no es adivinar")
  bodyMedium = TextStyle(
    fontFamily = LunaFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.sp
  ),
  // LunaSync/Small (13sp / 19sp)
  bodySmall = TextStyle(
    fontFamily = LunaFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 19.sp,
    letterSpacing = 0.sp
  ),
  // Button Labels (16sp SemiBold)
  labelLarge = TextStyle(
    fontFamily = LunaFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.sp
  ),
  // LunaSync/Caption (11sp Medium / 15sp)
  labelSmall = TextStyle(
    fontFamily = LunaFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    lineHeight = 15.sp,
    letterSpacing = 0.2.sp
  )
)
