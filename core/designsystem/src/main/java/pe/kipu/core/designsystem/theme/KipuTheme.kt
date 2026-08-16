package pe.kipu.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val KipuDarkColorScheme = darkColorScheme(
    primary = KipuPrimary,
    onPrimary = KipuOnPrimary,
    primaryContainer = KipuPrimaryDark,
    onPrimaryContainer = KipuOnPrimaryContainerDark,
    secondary = KipuSecondaryDark,
    onSecondary = KipuOnSecondaryDark,
    secondaryContainer = KipuSecondaryContainerDark,
    onSecondaryContainer = KipuOnSecondaryContainerDark,
    tertiary = KipuTertiaryDark,
    onTertiary = KipuOnTertiaryDark,
    tertiaryContainer = KipuTertiaryContainerDark,
    onTertiaryContainer = KipuOnTertiaryContainerDark,
    background = KipuBg,
    onBackground = KipuTextPrimary,
    surface = KipuBgCard,
    onSurface = KipuTextPrimary,
    surfaceVariant = KipuBgElevated,
    onSurfaceVariant = KipuTextSecondary,
    outline = KipuBorder,
    outlineVariant = KipuTextMuted,
    error = KipuErrorDark,
    onError = KipuOnErrorDark,
    errorContainer = KipuErrorContainerDark,
    onErrorContainer = KipuOnErrorContainerDark,
    inverseSurface = KipuTextPrimary,
    inverseOnSurface = KipuBg,
)

private val KipuLightColorScheme = lightColorScheme(
    primary = KipuPrimaryLight,
    onPrimary = KipuOnPrimaryLight,
    primaryContainer = KipuPrimaryContainerLight,
    onPrimaryContainer = KipuOnPrimaryContainerLight,
    secondary = KipuSecondaryLight,
    onSecondary = KipuOnSecondaryLight,
    secondaryContainer = KipuSecondaryContainerLight,
    onSecondaryContainer = KipuOnSecondaryContainerLight,
    tertiary = KipuTertiaryLight,
    onTertiary = KipuOnTertiaryLight,
    tertiaryContainer = KipuTertiaryContainerLight,
    onTertiaryContainer = KipuOnTertiaryContainerLight,
    background = KipuBackgroundLight,
    onBackground = KipuOnSurfaceLight,
    surface = KipuSurfaceLight,
    onSurface = KipuOnSurfaceLight,
    surfaceVariant = KipuSurfaceLightVariant,
    onSurfaceVariant = KipuOnSurfaceLightVariant,
    outline = KipuBorderLight,
    outlineVariant = KipuOutlineVariantLight,
    error = KipuErrorLight,
    onError = KipuOnErrorLight,
    errorContainer = KipuErrorContainerLight,
    onErrorContainer = KipuOnErrorContainerLight,
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
