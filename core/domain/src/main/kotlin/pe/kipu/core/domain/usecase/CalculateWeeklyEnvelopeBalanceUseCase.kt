package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import javax.inject.Inject
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.EnvelopeBudgetState
import pe.kipu.core.domain.model.FinancialPlan
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.WeeklyEnvelopeBalanceStatus
import pe.kipu.core.domain.model.WeeklyEnvelopeBalanceSummary

class CalculateWeeklyEnvelopeBalanceUseCase @Inject constructor() {

    operator fun invoke(
        plan: FinancialPlan?,
        budgets: List<EnvelopeBudgetState>,
    ): WeeklyEnvelopeBalanceSummary {
        val allocated = budgets.fold(Money.ZERO) { acc, budget ->
            acc + budget.weeklyLimit
        }

        if (plan == null || plan.estimatedMonthlyIncome.isZero()) {
            return WeeklyEnvelopeBalanceSummary(
                weeklyIncome = null,
                allocated = allocated,
                unallocated = null,
                status = WeeklyEnvelopeBalanceStatus.NO_PLAN,
            )
        }

        val weeklyIncome = divideMonthlyToWeekly(plan.estimatedMonthlyIncome)
        val delta = weeklyIncome.amount.subtract(allocated.amount)
        val unallocated = when (val result = Money.of(delta.abs())) {
            is DomainResult.Ok -> result.value
            is DomainResult.Err -> Money.ZERO
        }

        val status = when {
            delta.signum() < 0 -> WeeklyEnvelopeBalanceStatus.OVER_ALLOCATED
            delta.signum() > 0 -> WeeklyEnvelopeBalanceStatus.UNALLOCATED
            else -> WeeklyEnvelopeBalanceStatus.BALANCED
        }

        return WeeklyEnvelopeBalanceSummary(
            weeklyIncome = weeklyIncome,
            allocated = allocated,
            unallocated = unallocated,
            status = status,
        )
    }

    private fun divideMonthlyToWeekly(monthly: Money): Money {
        val weekly = monthly.amount.divide(
            BigDecimal.valueOf(WEEKS_PER_MONTH),
            MONEY_SCALE,
            java.math.RoundingMode.HALF_UP,
        )
        return when (val result = Money.of(weekly)) {
            is DomainResult.Ok -> result.value
            is DomainResult.Err -> Money.ZERO
        }
    }

    private companion object {
        const val WEEKS_PER_MONTH: Long = 4L
        const val MONEY_SCALE: Int = 2
    }
}
