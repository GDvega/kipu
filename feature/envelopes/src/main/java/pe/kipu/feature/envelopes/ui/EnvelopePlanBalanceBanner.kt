package pe.kipu.feature.envelopes.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.kipu.core.designsystem.component.KipuCard
import pe.kipu.core.designsystem.component.formatPenAmountForDisplay
import pe.kipu.core.domain.model.WeeklyEnvelopeBalanceStatus
import pe.kipu.core.domain.model.WeeklyEnvelopeBalanceSummary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EnvelopePlanBalanceBanner(
    summary: WeeklyEnvelopeBalanceSummary,
    modifier: Modifier = Modifier,
) {
    if (summary.status == WeeklyEnvelopeBalanceStatus.NO_PLAN) return

    val (title, titleColor) = when (summary.status) {
        WeeklyEnvelopeBalanceStatus.BALANCED ->
            "Tu presupuesto semanal está balanceado" to MaterialTheme.colorScheme.primary
        WeeklyEnvelopeBalanceStatus.UNALLOCATED ->
            "Tienes plata sin asignar a sobres" to MaterialTheme.colorScheme.secondary
        WeeklyEnvelopeBalanceStatus.OVER_ALLOCATED ->
            "Sobreasignaste tus sobres semanales" to MaterialTheme.colorScheme.error
        WeeklyEnvelopeBalanceStatus.NO_PLAN -> return
    }

    KipuCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = titleColor,
            fontWeight = FontWeight.Bold,
        )
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            summary.weeklyIncome?.let { income ->
                BalanceCell(
                    label = "Ingresos/sem",
                    value = formatPenAmountForDisplay(income.amount),
                )
            }
            BalanceCell(
                label = "Asignado",
                value = formatPenAmountForDisplay(summary.allocated.amount),
            )
            summary.unallocated?.let { unallocated ->
                BalanceCell(
                    label = if (summary.status == WeeklyEnvelopeBalanceStatus.OVER_ALLOCATED) {
                        "Exceso"
                    } else {
                        "Sin asignar"
                    },
                    value = formatPenAmountForDisplay(unallocated.amount),
                    valueColor = titleColor,
                )
            }
        }
    }
}

@Composable
private fun BalanceCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = valueColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
