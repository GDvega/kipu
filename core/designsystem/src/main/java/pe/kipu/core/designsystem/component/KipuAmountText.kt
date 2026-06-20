package pe.kipu.core.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import pe.kipu.core.designsystem.theme.KipuExpense
import pe.kipu.core.designsystem.theme.KipuIncome
import pe.kipu.core.designsystem.theme.KipuPrimary
import pe.kipu.core.designsystem.theme.KipuTheme
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Visual category for amount coloring in the UI.
 */
enum class AmountType {
    INCOME,
    EXPENSE,
    NEUTRAL,
}

/**
 * Displays a Peruvian sol (PEN) amount with Kipu styling.
 * Performs display formatting only; no financial calculations.
 */
@Composable
fun KipuAmountText(
    amount: BigDecimal,
    type: AmountType,
    modifier: Modifier = Modifier,
    showSign: Boolean = false,
) {
    KipuAmountText(
        text = formatPenAmountForDisplay(amount, showSign),
        type = type,
        modifier = modifier,
    )
}

/**
 * Displays a pre-formatted PEN amount string.
 */
@Composable
fun KipuAmountText(
    text: String,
    type: AmountType,
    modifier: Modifier = Modifier,
) {
    val color = when (type) {
        AmountType.INCOME -> KipuIncome
        AmountType.EXPENSE -> KipuExpense
        AmountType.NEUTRAL -> MaterialTheme.colorScheme.onSurface
    }

    val typography = when (type) {
        AmountType.INCOME -> MaterialTheme.typography.displayMedium
        AmountType.NEUTRAL -> MaterialTheme.typography.titleMedium
        AmountType.EXPENSE -> MaterialTheme.typography.titleMedium
    }

    Text(
        text = text,
        modifier = modifier,
        style = typography,
        fontWeight = if (type == AmountType.INCOME) FontWeight.W800 else FontWeight.SemiBold,
        color = if (type == AmountType.INCOME) KipuPrimary else color,
    )
}

/**
 * Formats a [BigDecimal] for PEN display: `S/ 1,234.50`.
 */
fun formatPenAmountForDisplay(amount: BigDecimal, showSign: Boolean = false): String {
    val symbols = DecimalFormatSymbols(Locale.US).apply {
        groupingSeparator = ','
        decimalSeparator = '.'
    }
    val formatter = DecimalFormat("#,##0.00", symbols)
    val normalized = amount.setScale(2, RoundingMode.HALF_UP)
    val prefix = when {
        showSign && normalized.signum() > 0 -> "+"
        showSign && normalized.signum() < 0 -> "-"
        else -> ""
    }
    val absolute = normalized.abs()
    return "$prefix S/ ${formatter.format(absolute)}".trim()
}

@Preview(showBackground = true)
@Composable
private fun KipuAmountTextIncomePreview() {
    KipuTheme {
        KipuAmountText(amount = BigDecimal("1234.5"), type = AmountType.INCOME)
    }
}

@Preview(showBackground = true)
@Composable
private fun KipuAmountTextExpensePreview() {
    KipuTheme {
        KipuAmountText(amount = BigDecimal("25.50"), type = AmountType.EXPENSE)
    }
}

@Preview(showBackground = true)
@Composable
private fun KipuAmountTextNeutralPreview() {
    KipuTheme {
        KipuAmountText(text = "S/ 100.00", type = AmountType.NEUTRAL)
    }
}
