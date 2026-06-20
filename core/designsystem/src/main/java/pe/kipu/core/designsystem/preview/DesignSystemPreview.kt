package pe.kipu.core.designsystem.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pe.kipu.core.designsystem.component.AmountType
import pe.kipu.core.designsystem.component.KipuAmountText
import pe.kipu.core.designsystem.component.KipuCard
import pe.kipu.core.designsystem.component.KipuEmptyState
import pe.kipu.core.designsystem.component.KipuErrorState
import pe.kipu.core.designsystem.component.KipuPrimaryButton
import pe.kipu.core.designsystem.component.KipuSecondaryButton
import pe.kipu.core.designsystem.component.KipuTopBar
import pe.kipu.core.designsystem.theme.KipuTheme
import java.math.BigDecimal

/**
 * Showcase of Kipu design system components for Android Studio previews.
 */
@Preview(showBackground = true, name = "Design System Showcase")
@Composable
fun DesignSystemPreview() {
    KipuTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            KipuTopBar(title = "Kipu Design System")
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                SectionTitle("Buttons")
                KipuPrimaryButton(text = "Primary", onClick = {})
                KipuSecondaryButton(text = "Secondary", onClick = {})

                SectionTitle("Card")
                KipuCard(
                    title = "Movimiento ejemplo",
                    subtitle = "Plin · Ayer",
                )

                SectionTitle("Amounts")
                KipuAmountText(amount = BigDecimal("1500.75"), type = AmountType.INCOME)
                KipuAmountText(amount = BigDecimal("32.40"), type = AmountType.EXPENSE)
                KipuAmountText(text = "S/ 500.00", type = AmountType.NEUTRAL)

                SectionTitle("Empty state")
                KipuEmptyState(
                    title = "Aún no hay movimientos",
                    message = "Registra tu primer gasto o ingreso.",
                    actionLabel = "Agregar",
                    onAction = {},
                )

                SectionTitle("Error state")
                KipuErrorState(
                    title = "No pudimos cargar",
                    message = "Inténtalo de nuevo.",
                    retryLabel = "Reintentar",
                    onRetry = {},
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}
