package pe.kipu.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import pe.kipu.core.designsystem.theme.KipuPrimary

/**
 * Hero card with gradient surface and subtle primary glow — HTML `.daily-card`.
 */
@Composable
fun KipuHeroCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.extraLarge
    val surface = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val border = MaterialTheme.colorScheme.outline

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(surface, surfaceVariant),
                    start = Offset.Zero,
                    end = Offset(800f, 800f),
                ),
            )
            .border(width = 1.dp, color = border, shape = shape),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            KipuPrimary.copy(alpha = 0.10f),
                            Color.Transparent,
                        ),
                        center = Offset(600f, 0f),
                        radius = 400f,
                    ),
                ),
        )
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
            content = content,
        )
    }
}
