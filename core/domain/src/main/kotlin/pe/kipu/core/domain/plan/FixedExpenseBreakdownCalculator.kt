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
    ): DomainResult<Money> = sumAll(
        presetParts = listOf(educationText, rentText, utilitiesText, phoneText, debtsText),
        customLines = emptyList(),
    )

    fun sumAll(
        presetParts: List<String>,
        customLines: List<PlanWizardLineItem>,
    ): DomainResult<Money> {
        var total = BigDecimal.ZERO

        for (part in presetParts) {
            when (val parsed = parsePart(part)) {
                is DomainResult.Err -> return parsed
                is DomainResult.Ok -> total = total.add(parsed.value.amount)
            }
        }

        for (line in customLines) {
            when (val parsed = parsePart(line.amountText)) {
                is DomainResult.Err -> return parsed
                is DomainResult.Ok -> total = total.add(parsed.value.amount)
            }
        }

        return Money.of(total)
    }

    fun formatTotal(
        educationText: String,
        rentText: String,
        utilitiesText: String,
        phoneText: String,
        debtsText: String,
        customLines: List<PlanWizardLineItem> = emptyList(),
    ): String = when (
        val result = sumAll(
            presetParts = listOf(educationText, rentText, utilitiesText, phoneText, debtsText),
            customLines = customLines,
        )
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
