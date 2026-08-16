package pe.kipu.core.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.EnvelopeBudgetState
import pe.kipu.core.domain.model.EnvelopeBudgetStatus
import pe.kipu.core.domain.model.FinancialPlan
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.WeeklyEnvelopeBalanceStatus
import pe.kipu.core.domain.usecase.CalculateWeeklyEnvelopeBalanceUseCase
import java.math.BigDecimal

class CalculateWeeklyEnvelopeBalanceUseCaseTest {

    private val useCase = CalculateWeeklyEnvelopeBalanceUseCase()

    @Test
    fun noPlan_returnsNoPlanStatus() {
        val budgets = listOf(envelopeBudget(weeklyLimit = "100"))

        val summary = useCase(plan = null, budgets = budgets)

        assertEquals(WeeklyEnvelopeBalanceStatus.NO_PLAN, summary.status)
        assertEquals(null, summary.weeklyIncome)
    }

    @Test
    fun balancedPlan_returnsBalancedStatus() {
        val plan = planWithMonthlyIncome("800")
        val budgets = listOf(
            envelopeBudget(weeklyLimit = "100"),
            envelopeBudget(weeklyLimit = "100"),
        )

        val summary = useCase(plan = plan, budgets = budgets)

        assertEquals(WeeklyEnvelopeBalanceStatus.BALANCED, summary.status)
        assertEquals(money("200"), summary.allocated)
        assertEquals(money("0"), summary.unallocated)
    }

    @Test
    fun unallocatedIncome_returnsUnallocatedStatus() {
        val plan = planWithMonthlyIncome("800")
        val budgets = listOf(envelopeBudget(weeklyLimit = "100"))

        val summary = useCase(plan = plan, budgets = budgets)

        assertEquals(WeeklyEnvelopeBalanceStatus.UNALLOCATED, summary.status)
        assertEquals(money("100"), summary.unallocated)
    }

    @Test
    fun overAllocated_returnsOverAllocatedStatus() {
        val plan = planWithMonthlyIncome("400")
        val budgets = listOf(
            envelopeBudget(weeklyLimit = "100"),
            envelopeBudget(weeklyLimit = "100"),
        )

        val summary = useCase(plan = plan, budgets = budgets)

        assertEquals(WeeklyEnvelopeBalanceStatus.OVER_ALLOCATED, summary.status)
        assertEquals(money("100"), summary.unallocated)
    }

    @Test
    fun monthlyPlan_comparesLimitsAgainstFullMonthlyIncome() {
        // Ciclo MENSUAL: los límites ya son mensuales, así que se comparan contra el ingreso
        // mensual COMPLETO (no ingreso/4). Con semanal fijo esto marcaría OVER_ALLOCATED falso.
        val plan = FinancialPlan(
            id = "plan-1",
            estimatedMonthlyIncome = money("800"),
            fixedExpenses = Money.ZERO,
            budgetCycle = pe.kipu.core.domain.model.BudgetCycle.MONTHLY,
        )
        val budgets = listOf(
            envelopeBudget(weeklyLimit = "500"),
            envelopeBudget(weeklyLimit = "300"),
        )

        val summary = useCase(plan = plan, budgets = budgets)

        assertEquals(WeeklyEnvelopeBalanceStatus.BALANCED, summary.status)
        assertEquals(money("800"), summary.weeklyIncome)
        assertEquals(money("800"), summary.allocated)
    }

    private fun planWithMonthlyIncome(amount: String): FinancialPlan =
        FinancialPlan(
            id = "plan-1",
            estimatedMonthlyIncome = money(amount),
            fixedExpenses = Money.ZERO,
        )

    private fun envelopeBudget(weeklyLimit: String): EnvelopeBudgetState {
        val limit = money(weeklyLimit)
        return EnvelopeBudgetState(
            envelopeId = "env-${weeklyLimit}",
            name = "Test",
            categoryId = "cat-1",
            weeklyLimit = limit,
            spentAmount = Money.ZERO,
            remainingAmount = limit,
            percentUsed = 0,
            status = EnvelopeBudgetStatus.OK,
        )
    }

    private fun money(value: String): Money =
        (Money.of(BigDecimal(value)) as DomainResult.Ok).value
}
