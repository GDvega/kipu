package pe.kipu.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// Brand — teal Kipu
val KipuPrimary = Color(0xFF34D399) // Emerald 400 - Softer and more professional for dark theme
val KipuPrimaryDark = Color(0xFF10B981) // Emerald 500
val KipuPrimaryLight = Color(0xFF059669) // Emerald 600 - Good contrast for light theme
val KipuPrimaryDim = Color(0x3334D399)
val KipuOnPrimary = Color(0xFF022C22)

// Dark surfaces - Deep Indigo inspired
val KipuBg = Color(0xFF0A0A14) // Deep Indigo almost black
val KipuBgElevated = Color(0xFF141423) // Slightly raised indigo
val KipuBgCard = Color(0xFF1B1B2C) // Card background
val KipuBgHover = Color(0xFF26263A)
val KipuBgInput = Color(0xFF05050A)
val KipuBorder = Color(0xFF33334C)

// Light surfaces — crisp and modern
val KipuBackgroundLight = Color(0xFFF7F8FA) // Crisp off-white
val KipuBackgroundLightElevated = Color(0xFFF0F2F5)
val KipuSurfaceLight = Color(0xFFFFFFFF)
val KipuSurfaceLightVariant = Color(0xFFEDF0F3)
val KipuOnSurfaceLight = Color(0xFF11141C)
val KipuOnSurfaceLightVariant = Color(0xFF646B7C)
val KipuBorderLight = Color(0xFFD6DBE5)
val KipuPrimaryContainerLight = Color(0xFFD1FBF1)
val KipuOnPrimaryContainerLight = Color(0xFF004D3C)

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

// Semantic aliases (legacy names used across features)
val KipuSecondary = KipuAmber
val KipuBackground = KipuBg
val KipuSurface = KipuBgCard
val KipuOnSurface = KipuTextPrimary
val KipuError = KipuRed
val KipuIncome = KipuPrimary
val KipuExpense = KipuRed
