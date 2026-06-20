package pe.kipu.core.domain.usecase

import javax.inject.Inject
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.EnvelopeBudgetState
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.WeeklyEnvelopeTotals

class CalculateWeeklyEnvelopeTotalsUseCase @Inject constructor() {

    operator fun invoke(budgets: List<EnvelopeBudgetState>): WeeklyEnvelopeTotals {
        if (budgets.isEmpty()) {
            return WeeklyEnvelopeTotals(
                totalLimit = Money.ZERO,
                totalSpent = Money.ZERO,
                totalRemaining = Money.ZERO,
                weeklyDeficit = null,
            )
        }

        val totalLimit = budgets.fold(Money.ZERO) { acc, budget -> acc + budget.weeklyLimit }
        val totalSpent = budgets.fold(Money.ZERO) { acc, budget -> acc + budget.spentAmount }
        val weeklyDeficit = if (totalSpent.amount > totalLimit.amount) {
            subtractMoney(totalSpent, totalLimit)
        } else {
            null
        }
        val totalRemaining = if (weeklyDeficit != null) {
            Money.ZERO
        } else {
            subtractMoney(totalLimit, totalSpent)
        }

        return WeeklyEnvelopeTotals(
            totalLimit = totalLimit,
            totalSpent = totalSpent,
            totalRemaining = totalRemaining,
            weeklyDeficit = weeklyDeficit,
        )
    }

    private fun subtractMoney(minuend: Money, subtrahend: Money): Money =
        when (val result = minuend.minus(subtrahend)) {
            is DomainResult.Ok -> result.value
            is DomainResult.Err -> Money.ZERO
        }
}
