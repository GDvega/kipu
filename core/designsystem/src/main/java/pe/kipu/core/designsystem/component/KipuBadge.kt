package pe.kipu.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class KipuBadgeTone {
    Primary,
    Warning,
    Critical,
    Info,
    Purple,
}

/** Pill status chip — HTML `.daily-status` / `.mov-status`. */
@Composable
fun KipuBadge(
    text: String,
    tone: KipuBadgeTone,
    modifier: Modifier = Modifier,
) {
    val (background, foreground) = tone.colors()

    Text(
        text = text,
        modifier = modifier
            .background(color = background, shape = MaterialTheme.shapes.extraLarge)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = foreground,
    )
}

/** Small tag — HTML `.mov-source` / `.source-tag`. */
@Composable
fun KipuCompactBadge(
    text: String,
    tone: KipuBadgeTone,
    modifier: Modifier = Modifier,
) {
    val (background, foreground) = tone.colors()

    Text(
        text = text.uppercase(),
        modifier = modifier
            .background(color = background, shape = MaterialTheme.shapes.extraSmall)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = foreground,
    )
}

@Composable
private fun KipuBadgeTone.colors() = when (this) {
    KipuBadgeTone.Primary -> MaterialTheme.colorScheme.primaryContainer to
        MaterialTheme.colorScheme.onPrimaryContainer
    KipuBadgeTone.Warning -> MaterialTheme.colorScheme.secondaryContainer to
        MaterialTheme.colorScheme.onSecondaryContainer
    KipuBadgeTone.Critical -> MaterialTheme.colorScheme.errorContainer to
        MaterialTheme.colorScheme.onErrorContainer
    KipuBadgeTone.Info -> MaterialTheme.colorScheme.surfaceVariant to
        MaterialTheme.colorScheme.onSurfaceVariant
    KipuBadgeTone.Purple -> MaterialTheme.colorScheme.tertiaryContainer to
        MaterialTheme.colorScheme.onTertiaryContainer
}
