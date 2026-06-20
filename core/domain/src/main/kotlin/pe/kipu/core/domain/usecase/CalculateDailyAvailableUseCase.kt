package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import pe.kipu.core.domain.model.DailyAvailableBudget
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.EnvelopeBudgetState
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.time.WeekRange
import pe.kipu.core.domain.time.WeekRangeCalculator

class CalculateDailyAvailableUseCase @Inject constructor(
    private val calculateWeeklyEnvelopeTotals: CalculateWeeklyEnvelopeTotalsUseCase,
) {

    operator fun invoke(
        budgets: List<EnvelopeBudgetState>,
        referenceInstant: Instant,
        weekRange: WeekRange,
    ): DailyAvailableBudget {
        val totals = calculateWeeklyEnvelopeTotals(budgets)
        val daysRemaining = calculateDaysRemainingInWeek(referenceInstant, weekRange)
        val isOverBudget = totals.weeklyDeficit != null
        val weeklyRemaining = if (isOverBudget) {
            Money.ZERO
        } else {
            totals.totalRemaining
        }

        val dailyAvailable = when {
            daysRemaining <= 0 -> null
            isOverBudget -> null
            budgets.isEmpty() -> null
            else -> divideMoney(weeklyRemaining, daysRemaining)
        }

        return DailyAvailableBudget(
            weeklyRemaining = weeklyRemaining,
            weeklyDeficit = totals.weeklyDeficit,
            daysRemainingInWeek = daysRemaining,
            dailyAvailable = dailyAvailable,
            isOverBudget = isOverBudget,
        )
    }

    private fun calculateDaysRemainingInWeek(referenceInstant: Instant, weekRange: WeekRange): Int {
        val referenceDate = referenceInstant.atZone(WeekRangeCalculator.PERU_ZONE).toLocalDate()
        val weekEndDate = weekRange.end
            .atZone(WeekRangeCalculator.PERU_ZONE)
            .toLocalDate()
            .minusDays(1)
        val daysBetween = ChronoUnit.DAYS.between(referenceDate, weekEndDate)
        return (daysBetween + 1).toInt().coerceAtLeast(0)
    }

    private fun divideMoney(amount: Money, divisor: Int): Money {
        val quotient = amount.amount
            .divide(BigDecimal(divisor), MoneyScale.SCALE, RoundingMode.HALF_UP)
        return when (val result = Money.of(quotient)) {
            is DomainResult.Ok -> result.value
            is DomainResult.Err -> Money.ZERO
        }
    }

    private object MoneyScale {
        const val SCALE = 2
    }
}
