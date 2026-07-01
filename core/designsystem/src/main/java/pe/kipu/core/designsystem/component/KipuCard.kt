package pe.kipu.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pe.kipu.core.designsystem.theme.KipuTheme

enum class KipuCardStyle {
    /** Standard card — bg-card + border (default list items). */
    Default,
    /** Larger radius for detail sections. */
    Large,
}

/**
 * Surface card matching HTML `.plan-card` / `.mov-card` — bordered, no elevation shadow.
 */
@Composable
fun KipuCard(
    modifier: Modifier = Modifier,
    style: KipuCardStyle = KipuCardStyle.Default,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = when (style) {
        KipuCardStyle.Default -> MaterialTheme.shapes.large
        KipuCardStyle.Large -> MaterialTheme.shapes.extraLarge
    }

    Surface(
        modifier = modifier
            .fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        tonalElevation = 2.dp,
        shadowElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content,
        )
    }
}

@Composable
fun KipuCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    style: KipuCardStyle = KipuCardStyle.Default,
) {
    KipuCard(modifier = modifier, style = style) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun KipuCardPreview() {
    KipuTheme(darkTheme = true) {
        Box(Modifier.padding(16.dp)) {
            KipuCard {
                Text("Yape a María", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Hoy · Comida",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
