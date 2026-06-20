package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.util.MoneyInputParser

/**
 * Suggests a weekly savings amount to reach a goal within [months].
 */
class CalculateGoalWeeklyContributionUseCase @Inject constructor() {

    fun invoke(
        targetText: String,
        currentText: String,
        months: Int,
    ): DomainResult<Money> {
        if (months <= 0) {
            return DomainResult.Err(
                pe.kipu.core.domain.model.DomainError.InvalidField("Months must be positive"),
            )
        }

        val target = parseAmount(targetText) ?: return invalidAmount()
        val current = parseAmount(currentText) ?: return invalidAmount()

        if (target.isZero()) return invalidAmount()

        val remaining = when (val diff = target.minus(current)) {
            is DomainResult.Ok -> diff.value
            is DomainResult.Err -> return diff
        }

        if (remaining.amount <= BigDecimal.ZERO) {
            return DomainResult.Ok(Money.ZERO)
        }

        val weeks = BigDecimal.valueOf(months.toLong()).multiply(WEEKS_PER_MONTH)
        val weekly = remaining.amount
            .divide(weeks, 2, RoundingMode.HALF_UP)

        return Money.of(weekly)
    }

    private fun parseAmount(text: String): Money? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return Money.ZERO
        return when (val result = MoneyInputParser.parsePen(trimmed)) {
            is DomainResult.Ok -> result.value
            is DomainResult.Err -> null
        }
    }

    private fun invalidAmount(): DomainResult.Err =
        DomainResult.Err(pe.kipu.core.domain.model.DomainError.InvalidAmount("Invalid goal amount"))

    private companion object {
        val WEEKS_PER_MONTH: BigDecimal = BigDecimal("4")
    }
}
