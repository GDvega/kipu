package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import pe.kipu.core.domain.model.CycleAvailableBudget
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.EnvelopeBudgetState
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.time.CycleRange
import pe.kipu.core.domain.time.CycleRangeCalculator

class CalculateCycleAvailableUseCase @Inject constructor(
    private val calculatePeriodEnvelopeTotals: CalculatePeriodEnvelopeTotalsUseCase,
) {

    operator fun invoke(
        budgets: List<EnvelopeBudgetState>,
        referenceInstant: Instant,
        cycleRange: CycleRange,
        cycle: pe.kipu.core.domain.model.BudgetCycle,
    ): CycleAvailableBudget {
        val totals = calculatePeriodEnvelopeTotals(budgets)
        val daysRemaining = calculateDaysRemainingInCycle(referenceInstant, cycleRange)
        val isOverBudget = totals.cycleDeficit != null // We'll fix totals struct next
        val cycleRemaining = if (isOverBudget) {
            Money.ZERO
        } else {
            totals.totalRemaining
        }

        val cycleAvailable = when (cycle) {
            pe.kipu.core.domain.model.BudgetCycle.DAILY -> cycleRemaining
            else -> {
                if (daysRemaining <= 0 || isOverBudget || budgets.isEmpty()) null
                else divideMoney(cycleRemaining, daysRemaining)
            }
        }
        
        val label = when (cycle) {
            pe.kipu.core.domain.model.BudgetCycle.DAILY -> "Disponible hoy"
            pe.kipu.core.domain.model.BudgetCycle.WEEKLY -> "Disponible esta semana"
            pe.kipu.core.domain.model.BudgetCycle.MONTHLY -> "Disponible este mes"
        }

        return CycleAvailableBudget(
            cycle = cycle,
            periodLabel = label,
            cycleRemaining = cycleRemaining,
            cycleDeficit = totals.cycleDeficit,
            daysRemainingInCycle = daysRemaining,
            cycleAvailable = cycleAvailable,
            isOverBudget = isOverBudget,
        )
    }

    private fun calculateDaysRemainingInCycle(referenceInstant: Instant, cycleRange: CycleRange): Int {
        val referenceDate = referenceInstant.atZone(CycleRangeCalculator.PERU_ZONE).toLocalDate()
        val cycleEndDate = cycleRange.end
            .atZone(CycleRangeCalculator.PERU_ZONE)
            .toLocalDate()
            .minusDays(1)
        val daysBetween = ChronoUnit.DAYS.between(referenceDate, cycleEndDate)
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
