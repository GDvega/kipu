package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import javax.inject.Inject
import pe.kipu.core.domain.model.BudgetCycle
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

        // `weeklyIncome` es en realidad el ingreso del ciclo del plan (deuda de naming):
        // los límites del sobre se expresan por ciclo, así que el ingreso se prorratea al mismo ciclo.
        val cycleIncome = prorateMonthlyIncome(plan.estimatedMonthlyIncome, plan.budgetCycle)
        val delta = cycleIncome.amount.subtract(allocated.amount)
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
            weeklyIncome = cycleIncome,
            allocated = allocated,
            unallocated = unallocated,
            status = status,
        )
    }

    private fun prorateMonthlyIncome(monthly: Money, cycle: BudgetCycle): Money {
        val divisor = when (cycle) {
            BudgetCycle.DAILY -> DAYS_PER_MONTH
            BudgetCycle.WEEKLY -> WEEKS_PER_MONTH
            BudgetCycle.MONTHLY -> 1L
        }
        if (divisor == 1L) return monthly
        val prorated = monthly.amount.divide(
            BigDecimal.valueOf(divisor),
            MONEY_SCALE,
            java.math.RoundingMode.HALF_UP,
        )
        return when (val result = Money.of(prorated)) {
            is DomainResult.Ok -> result.value
            is DomainResult.Err -> Money.ZERO
        }
    }

    private companion object {
        const val WEEKS_PER_MONTH: Long = 4L
        const val DAYS_PER_MONTH: Long = 30L
        const val MONEY_SCALE: Int = 2
    }
}
