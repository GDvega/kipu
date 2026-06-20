package pe.kipu.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.math.BigDecimal

@Composable
fun KipuAmountPresetRow(
    presets: List<BigDecimal>,
    selectedAmount: BigDecimal?,
    onPresetSelected: (BigDecimal) -> Unit,
    modifier: Modifier = Modifier,
    onCustomize: (() -> Unit)? = null,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            presets.forEach { amount ->
                val label = "S/ ${amount.stripTrailingZeros().toPlainString()}"
                KipuFilterChip(
                    text = label,
                    selected = selectedAmount?.compareTo(amount) == 0,
                    onClick = { onPresetSelected(amount) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (onCustomize != null) {
            KipuFilterChip(
                text = "Personalizar",
                selected = selectedAmount != null && presets.none { it.compareTo(selectedAmount) == 0 },
                onClick = onCustomize,
            )
        }
    }
}
