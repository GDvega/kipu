package pe.kipu.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import pe.kipu.core.designsystem.theme.KipuAmber
import pe.kipu.core.designsystem.theme.KipuAmberDim
import pe.kipu.core.designsystem.theme.KipuRed
import pe.kipu.core.designsystem.theme.KipuRedDim

enum class KipuAlertTone {
    Warning,
    Critical,
    Info,
}

/**
 * Alert / radar card — HTML `.radar-card` / `.alert-card`.
 */
@Composable
fun KipuAlertCard(
    tone: KipuAlertTone,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val (startColor, borderColor) = when (tone) {
        KipuAlertTone.Warning -> KipuAmberDim to KipuAmber.copy(alpha = 0.25f)
        KipuAlertTone.Critical -> KipuRedDim to KipuRed.copy(alpha = 0.25f)
        KipuAlertTone.Info -> KipuAmberDim to KipuAmber.copy(alpha = 0.18f)
    }
    val surface = MaterialTheme.colorScheme.surface

    val shape = MaterialTheme.shapes.extraLarge
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(startColor, surface),
                ),
            )
            .border(width = 1.dp, color = borderColor, shape = shape)
            .padding(24.dp),
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            Column(content = content)
        }
    }
}
