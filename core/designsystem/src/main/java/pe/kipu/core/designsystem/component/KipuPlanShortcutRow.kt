package pe.kipu.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class KipuPlanShortcut(
    val label: String,
    val onClick: () -> Unit,
)

/**
 * Navigation shortcuts to plan wizard steps — visually distinct from filter chips.
 */
@Composable
fun KipuPlanShortcutRow(
    shortcuts: List<KipuPlanShortcut>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        shortcuts.chunked(2).forEach { rowShortcuts ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowShortcuts.forEach { shortcut ->
                    KipuSecondaryButton(
                        text = shortcut.label,
                        onClick = shortcut.onClick,
                        modifier = Modifier.weight(1f),
                        fillWidth = true,
                    )
                }
                repeat(2 - rowShortcuts.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
