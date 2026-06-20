package pe.kipu.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val KipuDarkColorScheme = darkColorScheme(
    primary = KipuPrimary,
    onPrimary = KipuOnPrimary,
    primaryContainer = KipuPrimaryDim,
    onPrimaryContainer = KipuPrimary,
    secondary = KipuAmber,
    onSecondary = KipuOnPrimary,
    secondaryContainer = KipuAmberDim,
    onSecondaryContainer = KipuAmber,
    tertiary = KipuPurple,
    onTertiary = KipuOnPrimary,
    background = KipuBg,
    onBackground = KipuTextPrimary,
    surface = KipuBgCard,
    onSurface = KipuTextPrimary,
    surfaceVariant = KipuBgElevated,
    onSurfaceVariant = KipuTextSecondary,
    outline = KipuBorder,
    outlineVariant = KipuTextDim,
    error = KipuRed,
    onError = Color.White,
    inverseSurface = KipuTextPrimary,
    inverseOnSurface = KipuBg,
)

private val KipuLightColorScheme = lightColorScheme(
    primary = KipuPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = KipuPrimaryContainerLight,
    onPrimaryContainer = KipuOnPrimaryContainerLight,
    secondary = KipuAmber,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFF3D6),
    onSecondaryContainer = Color(0xFF7C4A03),
    tertiary = KipuPurple,
    onTertiary = Color.White,
    background = KipuBackgroundLight,
    onBackground = KipuOnSurfaceLight,
    surface = KipuSurfaceLight,
    onSurface = KipuOnSurfaceLight,
    surfaceVariant = KipuSurfaceLightVariant,
    onSurfaceVariant = KipuOnSurfaceLightVariant,
    outline = KipuBorderLight,
    outlineVariant = Color(0xFFC8C4BC),
    error = KipuRed,
    onError = Color.White,
    inverseSurface = KipuOnSurfaceLight,
    inverseOnSurface = KipuBackgroundLight,
)

@Composable
fun KipuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) KipuDarkColorScheme else KipuLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = KipuTypography,
        shapes = KipuShapes,
        content = content,
    )
}
