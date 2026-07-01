package pe.kipu.core.designsystem.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import pe.kipu.core.designsystem.component.KipuFilterChipRow
import pe.kipu.core.designsystem.theme.KipuTheme
import java.math.BigDecimal

/**
 * Showcase of Kipu design system components for Android Studio previews.
 */
@Preview(showBackground = true, name = "Design System Showcase - Light")
@Composable
fun DesignSystemPreviewLight() {
    DesignSystemShowcase(darkTheme = false)
}

@Preview(showBackground = true, name = "Design System Showcase - Dark")
@Composable
fun DesignSystemPreviewDark() {
    DesignSystemShowcase(darkTheme = true)
}

@Composable
fun DesignSystemShowcase(darkTheme: Boolean) {
    KipuTheme(darkTheme = darkTheme) {
        Column(modifier = Modifier.fillMaxSize()) {
            KipuTopBar(title = "Kipu Design System")
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                SectionTitle("Selection Chips")
                KipuFilterChipRow(
                    labels = listOf("Claro", "Oscuro", "Sistema"),
                    selectedIndex = 0,
                    onSelected = {},
                    contentPadding = PaddingValues(0.dp)
                )

                SectionTitle("Buttons")
                KipuPrimaryButton(text = "Primary Button", onClick = {})
                KipuSecondaryButton(text = "Secondary Button", onClick = {}, fillWidth = true)

                SectionTitle("Card")
                KipuCard(
                    title = "Movimiento ejemplo",
                    subtitle = "Plin · Ayer",
                )

                SectionTitle("Amounts")
                KipuAmountText(amount = BigDecimal("1500.75"), type = AmountType.INCOME)
                KipuAmountText(amount = BigDecimal("32.40"), type = AmountType.EXPENSE)
                KipuAmountText(text = "S/ 500.00", type = AmountType.NEUTRAL)
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
