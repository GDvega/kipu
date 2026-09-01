package pe.kipu.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// Brand — teal Kipu
val KipuPrimary = Color(0xFF34D399) // Emerald 400 - Softer and more professional for dark theme
val KipuPrimaryDark = Color(0xFF00513F)
val KipuPrimaryLight = Color(0xFF006B55)
val KipuOnPrimaryLight = Color.White
val KipuPrimaryDim = Color(0x3334D399)
val KipuOnPrimary = Color(0xFF022C22)
val KipuOnPrimaryContainerDark = Color(0xFF7CF8C3)

// Dark surfaces - Deep Indigo inspired
val KipuBg = Color(0xFF0A0A14) // Deep Indigo almost black
val KipuBgElevated = Color(0xFF141423) // Slightly raised indigo
val KipuBgCard = Color(0xFF1B1B2C) // Card background
val KipuBgHover = Color(0xFF26263A)
val KipuBgInput = Color(0xFF05050A)
val KipuBorder = Color(0xFF8491A3)

// Light surfaces — crisp and modern
val KipuBackgroundLight = Color(0xFFF7F8FA) // Crisp off-white
val KipuBackgroundLightElevated = Color(0xFFF0F2F5)
val KipuSurfaceLight = Color(0xFFFFFFFF)
val KipuSurfaceLightVariant = Color(0xFFEDF0F3)
val KipuOnSurfaceLight = Color(0xFF11141C)
val KipuOnSurfaceLightVariant = Color(0xFF646B7C)
val KipuBorderLight = Color(0xFF737A8C)
val KipuOutlineVariantLight = Color(0xFF858C9C)
val KipuPrimaryContainerLight = Color(0xFF9EF2D5)
val KipuOnPrimaryContainerLight = Color(0xFF002117)

// Text — dark theme defaults
val KipuTextPrimary = Color(0xFFF8FAFC) // Slate 50 for max contrast
val KipuTextSecondary = Color(0xFF94A3B8) // Slate 400
val KipuTextMuted = Color(0xFF64748B) // Slate 500
val KipuTextDim = Color(0xFF475569) // Slate 600

// Accents — Vibrant and Premium
val KipuAmber = Color(0xFFF59E0B) // Amber
val KipuAmberDim = Color(0x26F59E0B)
val KipuRed = Color(0xFFFF5467) // Coral Red
val KipuRedDim = Color(0x26FF5467)
val KipuBlue = Color(0xFF3B82F6) // Bright Blue
val KipuBlueDim = Color(0x263B82F6)
val KipuPurple = Color(0xFFA855F7) // Vibrant Purple
val KipuPurpleDim = Color(0x26A855F7)

// Accessible semantic pairs for Material light/dark schemes.
val KipuSecondaryLight = Color(0xFF7A4E00)
val KipuOnSecondaryLight = Color.White
val KipuSecondaryContainerLight = Color(0xFFFFDEA8)
val KipuOnSecondaryContainerLight = Color(0xFF281800)
val KipuSecondaryDark = Color(0xFFFFB951)
val KipuOnSecondaryDark = Color(0xFF432C00)
val KipuSecondaryContainerDark = Color(0xFF5C3B00)
val KipuOnSecondaryContainerDark = Color(0xFFFFDEA8)

val KipuTertiaryLight = Color(0xFF7B2CBF)
val KipuOnTertiaryLight = Color.White
val KipuTertiaryContainerLight = Color(0xFFEEDBFF)
val KipuOnTertiaryContainerLight = Color(0xFF2B0052)
val KipuTertiaryDark = Color(0xFFD8B4FE)
val KipuOnTertiaryDark = Color(0xFF3B0764)
val KipuTertiaryContainerDark = Color(0xFF5B218A)
val KipuOnTertiaryContainerDark = Color(0xFFF2DAFF)

val KipuErrorLight = Color(0xFFBA1A1A)
val KipuOnErrorLight = Color.White
val KipuErrorContainerLight = Color(0xFFFFDAD6)
val KipuOnErrorContainerLight = Color(0xFF410002)
val KipuErrorDark = Color(0xFFFFB4AB)
val KipuOnErrorDark = Color(0xFF690005)
val KipuErrorContainerDark = Color(0xFF93000A)
val KipuOnErrorContainerDark = Color(0xFFFFDAD6)

// Semantic aliases (legacy names used across features)
val KipuSecondary = KipuAmber
val KipuBackground = KipuBg
val KipuSurface = KipuBgCard
val KipuOnSurface = KipuTextPrimary
val KipuError = KipuRed
val KipuIncome = KipuPrimary
val KipuExpense = KipuRed

// Multi-category chart color palette
val KipuChartPalette = listOf(
    KipuPrimary,             // Emerald #34D399
    KipuPurple,              // Purple #A855F7
    KipuAmber,               // Amber #F59E0B
    KipuBlue,                // Bright Blue #3B82F6
    KipuRed,                 // Coral Red #FF5467
    Color(0xFF06B6D4),       // Cyan #06B6D4
    Color(0xFFEC4899),       // Pink #EC4899
    Color(0xFF84CC16),       // Lime #84CC16
)

fun getChartColor(index: Int): Color = KipuChartPalette[index % KipuChartPalette.size]
