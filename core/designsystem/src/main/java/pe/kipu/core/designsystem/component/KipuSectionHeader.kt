package pe.kipu.core.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Subsection title within a screen (e.g. "Gastos hormiga", "Ingresos por confirmar"). */
@Composable
fun KipuSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = KipuLayout.screenHorizontalPadding,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(
            horizontal = horizontalPadding,
            vertical = 8.dp,
        ),
    )
}
