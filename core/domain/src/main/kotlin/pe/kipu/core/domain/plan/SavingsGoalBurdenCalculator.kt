package pe.kipu.core.domain.plan

import java.math.BigDecimal
import java.math.RoundingMode
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.Money

/**
 * Converts a savings goal into a monthly plan burden using the wizard's weekly-quota model.
 *
 * Monthly burden = weekly contribution × 4, where weekly = remaining ÷ (horizonMonths × 4).
 * Equivalent to remaining ÷ horizonMonths when using four weeks per month.
 */
object SavingsGoalBurdenCalculator {

    const val DEFAULT_HORIZON_MONTHS: Int = 5
    private const val MONEY_SCALE: Int = 2
    private val WEEKS_PER_MONTH: BigDecimal = BigDecimal.valueOf(4)

    fun monthlyBurden(
        target: Money,
        current: Money,
        horizonMonths: Int?,
        currencyCode: String,
    ): Money {
        val months = horizonMonths?.takeIf { it > 0 } ?: DEFAULT_HORIZON_MONTHS
        val remaining = remainingInPen(target, current, currencyCode) ?: return Money.ZERO
        if (remaining.isZero()) return Money.ZERO
        return monthlyBurdenFromRemaining(remaining, months)
    }

    fun weeklyContribution(remaining: Money, horizonMonths: Int): DomainResult<Money> {
        if (horizonMonths <= 0) {
            return DomainResult.Err(
                pe.kipu.core.domain.model.DomainError.InvalidField("Months must be positive"),
            )
        }
        if (remaining.isZero()) return DomainResult.Ok(Money.ZERO)

        val weeks = BigDecimal.valueOf(horizonMonths.toLong()).multiply(WEEKS_PER_MONTH)
        val weekly = remaining.amount.divide(weeks, MONEY_SCALE, RoundingMode.HALF_UP)
        return Money.of(weekly)
    }

    private fun monthlyBurdenFromRemaining(remaining: Money, horizonMonths: Int): Money {
        val monthly = remaining.amount.divide(
            BigDecimal.valueOf(horizonMonths.toLong()),
            MONEY_SCALE,
            RoundingMode.HALF_UP,
        )
        return when (val result = Money.of(monthly)) {
            is DomainResult.Ok -> result.value
            is DomainResult.Err -> Money.ZERO
        }
    }

    private fun remainingInPen(target: Money, current: Money, currencyCode: String): Money? {
        val remaining = when (val diff = target.minus(current)) {
            is DomainResult.Ok -> diff.value
            is DomainResult.Err -> return Money.ZERO
        }
        return CurrencyConverter.toPen(remaining, currencyCode)
    }
}
