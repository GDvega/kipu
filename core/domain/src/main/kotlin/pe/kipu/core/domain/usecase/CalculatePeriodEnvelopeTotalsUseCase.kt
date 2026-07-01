package pe.kipu.core.domain.usecase

import javax.inject.Inject
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.EnvelopeBudgetState
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.PeriodEnvelopeTotals

class CalculatePeriodEnvelopeTotalsUseCase @Inject constructor() {

    operator fun invoke(budgets: List<EnvelopeBudgetState>): PeriodEnvelopeTotals {
        if (budgets.isEmpty()) {
            return PeriodEnvelopeTotals(
                totalLimit = Money.ZERO,
                totalSpent = Money.ZERO,
                totalRemaining = Money.ZERO,
                cycleDeficit = null,
            )
        }

        val totalLimit = budgets.fold(Money.ZERO) { acc, budget -> acc + budget.weeklyLimit }
        val totalSpent = budgets.fold(Money.ZERO) { acc, budget -> acc + budget.spentAmount }
        val cycleDeficit = if (totalSpent.amount > totalLimit.amount) {
            subtractMoney(totalSpent, totalLimit)
        } else {
            null
        }
        val totalRemaining = if (cycleDeficit != null) {
            Money.ZERO
        } else {
            subtractMoney(totalLimit, totalSpent)
        }

        return PeriodEnvelopeTotals(
            totalLimit = totalLimit,
            totalSpent = totalSpent,
            totalRemaining = totalRemaining,
            cycleDeficit = cycleDeficit,
        )
    }

    private fun subtractMoney(minuend: Money, subtrahend: Money): Money =
        when (val result = minuend.minus(subtrahend)) {
            is DomainResult.Ok -> result.value
            is DomainResult.Err -> Money.ZERO
        }
}
