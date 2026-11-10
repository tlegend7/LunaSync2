package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Warm Organic Harmonic Palette (Inspired by Flo, Natural Cycles & Apple Health)
// Natural, warm, calming and feminine tones — not cold AI monochrome

// Brand & Accent Colors
val BrandTerracottaRose = Color(0xFFC75D6D) // Warm harmonic hero rose
val BrandTerracottaSoft = Color(0xFFFBECEF)
val BrandRoseGold = Color(0xFFD48B80)

// Phase Specific Organic Tones
val PhaseMenstrualRose = Color(0xFFE26D77) // Soft rosewood / terracotta blush
val PhaseMenstrualSubtle = Color(0xFFFDF0F1)

val PhaseFollicularPeach = Color(0xFFE58C65) // Warm radiant apricot
val PhaseFollicularSubtle = Color(0xFFFDF3ED)

val PhaseFertileSage = Color(0xFF4A9B6F) // Botanical sage / eucalyptus green
val PhaseFertileSubtle = Color(0xFFEFF8F3)
val PhaseOvulationGold = Color(0xFFECC265) // Soft golden star

val PhaseLutealLavender = Color(0xFF8B6FA8) // Serene mauve lavender
val PhaseLutealSubtle = Color(0xFFF6F2FA)

// Neutral Grouped Backgrounds (Warm Alabaster & Milk Silk)
val AppleBackground = Color(0xFFFAF7F2) // Warm alabaster / non-sterile
val AppleSurface = Color(0xFFFFFFFF) // Pure warm card surface
val AppleSurfaceElevated = Color(0xFFFFFFFF)
val AppleHairlineBorder = Color(0xFFEDE6DE) // Warm whisper border
val AppleTextPrimary = Color(0xFF2C2523) // Warm deep espresso/slate
val AppleTextSecondary = Color(0xFF756863) // Cocoa slate
val AppleTextTertiary = Color(0xFFA39690)

// Dark Surfaces (Warm OLED Noir)
val AppleDarkBackground = Color(0xFF141211)
val AppleDarkSurface = Color(0xFF1E1A19)
val AppleDarkSurfaceElevated = Color(0xFF2A2524)
val AppleDarkHairlineBorder = Color(0xFF383230)
val AppleDarkTextPrimary = Color(0xFFFDFBF7)
val AppleDarkTextSecondary = Color(0xFFA89F9B)
val AppleDarkTextTertiary = Color(0xFF6B6360)

// Status & Semantic Colors
val StatusSuccess = Color(0xFF2E7D32)
val StatusSuccessSubtle = Color(0xFFE8F5E9)
val StatusWarning = Color(0xFFD97706)
val StatusWarningSubtle = Color(0xFFFEF3C7)
val StatusError = Color(0xFFC62828)
val StatusErrorSubtle = Color(0xFFFFEBEE)

// System & Component Compatibilities
val AppleSystemBlue = BrandTerracottaRose
val AppleSystemIndigo = PhaseLutealLavender
val AppleSystemTeal = PhaseFertileSage
val AppleSystemGray = Color(0xFF9E928D)
val AppleSystemGray2 = Color(0xFFBEB4AF)
val AppleSystemGray5 = Color(0xFFEDE7E1)
val AppleSystemGray6 = Color(0xFFFAF7F2)

// Phase mappings for Cycle Dial and cards
val AppleHealthMenstrual = PhaseMenstrualRose
val AppleHealthMenstrualSubtle = PhaseMenstrualSubtle
val AppleHealthFollicular = PhaseFollicularPeach
val AppleHealthFollicularSubtle = PhaseFollicularSubtle
val AppleHealthFertile = PhaseFertileSage
val AppleHealthFertileSubtle = PhaseFertileSubtle
val AppleHealthLuteal = PhaseLutealLavender
val AppleHealthLutealSubtle = PhaseLutealSubtle

// Aliases
val PhaseMenstrual = PhaseMenstrualRose
val PhaseFollicular = PhaseFollicularPeach
val PhaseOvulatory = PhaseFertileSage
val PhaseLuteal = PhaseLutealLavender
val IceBlue = PhaseFollicularSubtle
val IceBlueSubtle = AppleBackground
val SoftCerulean = BrandTerracottaRose.copy(alpha = 0.5f)
val CeruleanMid = BrandTerracottaRose
val DeepOceanBlue = BrandTerracottaRose
val SlateNavy = AppleTextPrimary
val DarkNavyMidnight = AppleDarkBackground
