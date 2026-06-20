package pe.kipu.core.domain.plan

import java.math.BigDecimal
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.util.MoneyInputParser

object FixedExpenseBreakdownCalculator {

    fun sumParts(
        educationText: String,
        rentText: String,
        utilitiesText: String,
        phoneText: String,
        debtsText: String,
    ): DomainResult<Money> {
        val parts = listOf(educationText, rentText, utilitiesText, phoneText, debtsText)
        var total = BigDecimal.ZERO

        for (part in parts) {
            when (val parsed = parsePart(part)) {
                is DomainResult.Err -> return parsed
                is DomainResult.Ok -> total = total.add(parsed.value.amount)
            }
        }

        return Money.of(total)
    }

    /** @deprecated Use [sumParts] with five categories. Kept for gradual migration. */
    fun sumParts(
        rentText: String,
        utilitiesText: String,
        debtsText: String,
    ): DomainResult<Money> = sumParts(
        educationText = "",
        rentText = rentText,
        utilitiesText = utilitiesText,
        phoneText = "",
        debtsText = debtsText,
    )

    fun formatTotal(
        educationText: String,
        rentText: String,
        utilitiesText: String,
        phoneText: String,
        debtsText: String,
    ): String = when (
        val result = sumParts(educationText, rentText, utilitiesText, phoneText, debtsText)
    ) {
        is DomainResult.Ok -> result.value.amount.stripTrailingZeros().toPlainString()
        is DomainResult.Err -> ""
    }

    private fun parsePart(text: String): DomainResult<Money> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return DomainResult.Ok(Money.ZERO)
        }
        return MoneyInputParser.parsePen(trimmed)
    }
}
