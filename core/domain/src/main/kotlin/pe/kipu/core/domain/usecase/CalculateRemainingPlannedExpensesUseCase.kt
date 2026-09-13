package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import pe.kipu.core.domain.model.BudgetCycle
import pe.kipu.core.domain.model.EnvelopeBudgetState
import pe.kipu.core.domain.model.FinancialPlan
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.receipt.MonthlyServiceReceipt

/** Conservative projection: protects the whole current cycle, even when it crosses month end. */
class CalculateRemainingPlannedExpensesUseCase @Inject constructor() {
    operator fun invoke(
        plan: FinancialPlan?,
        budgets: List<EnvelopeBudgetState>,
        receipts: List<MonthlyServiceReceipt>,
        today: LocalDate,
    ): Money {
        val month = YearMonth.from(today)
        val paidReferences = receipts.filter { it.isPaid && it.monthKey == month.toString() }
            .sumOf { it.configuredAmount.amount }
        val fixed = ((plan?.fixedExpenses?.amount ?: BigDecimal.ZERO) - paidReferences).max(BigDecimal.ZERO)
        val cycle = plan?.budgetCycle ?: BudgetCycle.WEEKLY
        val nextCycle = when (cycle) {
            BudgetCycle.DAILY -> today.plusDays(1)
            BudgetCycle.WEEKLY -> today.plusDays((8 - today.dayOfWeek.value).toLong())
            BudgetCycle.MONTHLY -> month.plusMonths(1).atDay(1)
        }
        val futureDays = ChronoUnit.DAYS.between(nextCycle, month.plusMonths(1).atDay(1)).coerceAtLeast(0)
        val daysPerCycle = if (cycle == BudgetCycle.WEEKLY) 7 else 1
        val envelopes = budgets.sumOf { budget ->
            val currentRemaining = (budget.cycleLimit.amount - budget.spentAmount.amount).max(BigDecimal.ZERO)
            val future = budget.cycleLimit.amount.multiply(BigDecimal.valueOf(futureDays))
                .divide(BigDecimal(daysPerCycle), 2, RoundingMode.CEILING)
            currentRemaining + future
        }
        return Money.of(fixed + envelopes).getOrError()
    }
}
