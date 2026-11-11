package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// LunaSync Official Design System (Figma: LunaSync / Sistema visual)
// Base: Azul marino (#173E68, #14365A, #102943)
// Interacción y superficies: Azul hielo (#A9CEFA, #DCE4EE, #E6EFF9, #F5F7FA)
// Acentos discretos: Salvia (#376C59), Arena (#795D36), Coral (#AD4C5D, #A33345)
// ============================================================================

// --- Brand & Marine Hero Tones ---
val LunaNavyPrimary = Color(0xFF173E68)       // Color principal interactivo / botones primarios
val LunaNavyDarkCard = Color(0xFF14365A)      // Fondo de tarjeta hero "TU CICLO" / "Su ciclo"
val LunaNavyDeep = Color(0xFF102943)          // Azul marino profundo para contrastes altos
val LunaNavySurfaceDark = Color(0xFF172231)   // Fondo de navegación y tarjetas en modo oscuro
val LunaNavyBorderDark = Color(0xFF304156)    // Bordes y divisores en modo oscuro

// --- Ice Blue & Interaction Surfaces ---
val LunaBackgroundLight = Color(0xFFF5F7FA)   // Fondo base en modo claro
val LunaSurfaceLight = Color(0xFFFFFFFF)      // Superficie de tarjetas en modo claro
val LunaBorderLight = Color(0xFFDCE4EE)       // Borde tenue / separadores en modo claro
val LunaIceBlueSubtle = Color(0xFFE6EFF9)     // Superficie tenue azul hielo (chips seleccionados, badges)
val LunaIceBlueSurface = Color(0xFFF2F6FC)    // Superficie suave secundaria
val LunaIceBlueAccent = Color(0xFFA9CEFA)     // Acento azul hielo interactivo / tabs activos oscuro
val LunaIceBlueText = Color(0xFFC2D6EC)       // Texto secundario sobre fondos marinos oscuros

// --- Harmonic Subtle Accents ---
// Salvia (Cuidado, éxito, registros confirmados)
val LunaSageSoft = Color(0xFFEAF2EC)          // Fondo insignia registrado / mensaje apoyo
val LunaSageText = Color(0xFF376C59)          // Texto insignia registrado / indicador

// Arena (Estimaciones, advertencias sutiles)
val LunaSandSoft = Color(0xFFF4EEE4)          // Fondo insignia estimado
val LunaSandText = Color(0xFF795D36)          // Texto insignia estimado

// Coral (Menstruación, sangrado, peligro/desvincular)
val LunaCoralSoft = Color(0xFFFAE9ED)         // Fondo botón peligro / periodo registrado
val LunaCoralText = Color(0xFFA33345)         // Texto botón peligro / acción destructiva
val LunaPeriodRed = Color(0xFFAD4C5D)         // Indicador y borde de menstruación

// --- Typography & Neutral Slate ---
// Modo Claro
val LunaTextPrimary = Color(0xFF162638)       // Títulos y textos principales
val LunaTextSecondary = Color(0xFF5F7185)     // Subtítulos y textos de soporte
val LunaTextTertiary = Color(0xFF8E9CAE)      // Indicadores deshabilitados / placeholders

// Modo Oscuro
val LunaDarkBackground = Color(0xFF101B26)    // Fondo base modo oscuro
val LunaDarkSurface = Color(0xFF172231)       // Tarjetas modo oscuro
val LunaDarkSurfaceCard = Color(0xFF1E2D3E)   // Superficie elevada modo oscuro
val LunaDarkTextPrimary = Color(0xFFF5F7FA)   // Texto principal modo oscuro
val LunaDarkTextSecondary = Color(0xFFABBACD) // Texto secundario modo oscuro
val LunaDarkBorder = Color(0xFF2A3D52)        // Bordes modo oscuro

// --- Acentos Modo Oscuro (Tokens oficiales Figma #03) ---
val LunaSageDarkSoft = Color(0xFF22382D)      // Fondo salvia oscuro
val LunaSageDarkText = Color(0xFFA8D4BC)      // Texto salvia oscuro
val LunaSandDarkSoft = Color(0xFF3B3329)      // Fondo arena oscuro
val LunaSandDarkText = Color(0xFFE4C595)      // Texto arena oscuro
val LunaIceDarkSoft = Color(0xFF213C59)       // Fondo azul hielo oscuro
val LunaIceDarkText = Color(0xFFA9CEFA)       // Texto azul hielo oscuro
val LunaCoralDarkSoft = Color(0xFF412632)     // Fondo coral oscuro
val LunaCoralDarkText = Color(0xFFF1A4AF)     // Texto coral oscuro

// --- Backward Compatibility Aliases ---
val BrandTerracottaRose = LunaNavyPrimary
val BrandTerracottaSoft = LunaIceBlueSubtle
val BrandWarmRose = LunaNavyPrimary
val PhaseMenstrualRose = LunaPeriodRed
val PhaseMenstrualSubtle = LunaCoralSoft
val PhaseFertileSage = LunaSageText
val PhaseFertileSubtle = LunaSageSoft
val PhaseFollicularPeach = LunaSandText
val PhaseFollicularSubtle = LunaSandSoft
val PhaseLutealLavender = LunaNavyPrimary
val PhaseLutealSubtle = LunaIceBlueSubtle
val AppleBackground = LunaBackgroundLight
val AppleSurface = LunaSurfaceLight
val AppleHairlineBorder = LunaBorderLight
val AppleTextPrimary = LunaTextPrimary
val AppleTextSecondary = LunaTextSecondary
val AppleDarkBackground = LunaDarkBackground
val AppleDarkSurface = LunaDarkSurface
val AppleDarkHairlineBorder = LunaDarkBorder
val AppleDarkTextPrimary = LunaDarkTextPrimary
val AppleDarkTextSecondary = LunaDarkTextSecondary
val AppleSystemBlue = LunaNavyPrimary
val AppleSystemGray = LunaTextSecondary
val AppleSystemGray5 = LunaBorderLight
val AppleSystemGray6 = LunaBackgroundLight
val PhaseMenstrual = LunaPeriodRed
val PhaseFollicular = LunaSandText
val PhaseOvulatory = LunaSageText
val PhaseLuteal = LunaNavyPrimary
val IceBlue = LunaIceBlueAccent
val IceBlueSubtle = LunaIceBlueSubtle
val DeepOceanBlue = LunaNavyPrimary
val SlateNavy = LunaTextPrimary
val DarkNavyMidnight = LunaNavyDarkCard
val StatusSuccess = LunaSageText
val StatusSuccessSubtle = LunaSageSoft
val StatusError = LunaCoralText
val StatusErrorSubtle = LunaCoralSoft
val AppleHealthFollicularSubtle = LunaSandSoft
val AppleSystemIndigo = LunaNavyPrimary
val AppleHealthFollicular = LunaSandText
val AppleHealthFertile = LunaSageText
val AppleHealthLuteal = LunaNavyPrimary
val AppleHealthMenstrual = LunaPeriodRed
