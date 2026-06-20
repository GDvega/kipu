package pe.kipu.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Horizontal filter row — HTML `.mov-filter` / `.mov-filter-btn`. */
@Composable
fun KipuFilterChipRow(
    labels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp),
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        labels.forEachIndexed { index, label ->
            KipuFilterChip(
                text = label,
                selected = index == selectedIndex,
                onClick = { onSelected(index) },
            )
        }
    }
}

@Composable
fun KipuFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.extraLarge
    val colors = MaterialTheme.colorScheme

    Text(
        text = text,
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(shape)
            .then(
                if (selected) {
                    Modifier.background(colors.primary, shape)
                } else {
                    Modifier
                        .background(colors.surface, shape)
                        .border(1.5.dp, colors.outline, shape)
                },
            )
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = if (selected) "$text, seleccionado" else text
            },
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = if (selected) colors.onPrimary else colors.onSurfaceVariant,
    )
}
