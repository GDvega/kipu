package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test
import pe.kipu.core.domain.model.*
import pe.kipu.core.domain.receipt.MonthlyServiceReceipt
import pe.kipu.core.domain.receipt.ServiceReceiptKey

class CalculateRemainingPlannedExpensesUseCaseTest {
    private val useCase = CalculateRemainingPlannedExpensesUseCase()

    @Test
    fun `monthly obligations protect fixed references and unspent envelopes`() {
        val plan = plan(BudgetCycle.MONTHLY, "200")
        assertEquals(money("800"), useCase(plan, listOf(budget("700", "100")), emptyList(), date))
    }

    @Test
    fun `paid variable receipt removes reference not actual payment from remaining obligations`() {
        val paid = MonthlyServiceReceipt(
            key = ServiceReceiptKey.LIGHT, title = "Luz", configuredAmount = money("45"),
            monthKey = "2026-09", isPaid = true, paidAmount = money("55"), paidMovementId = "payment",
        )
        assertEquals(money("100"), useCase(plan(BudgetCycle.MONTHLY, "145"), emptyList(), listOf(paid), date))
        assertEquals(money("145"), useCase(plan(BudgetCycle.MONTHLY, "145"), emptyList(),
            listOf(paid.copy(monthKey = "2026-08")), date))
    }

    @Test
    fun `daily projection includes all remaining calendar days rather than one day only`() {
        // September 29: 6 left today and 10 for September 30.
        assertEquals(money("16"), useCase(plan(BudgetCycle.DAILY), listOf(budget("10", "4")),
            emptyList(), LocalDate.of(2026, 9, 29)))
    }

    @Test
    fun `weekly projection protects full current cycle and prorates future days conservatively`() {
        // September 20, Sunday: 20 left this week, plus 10 future days at 10/day.
        assertEquals(money("120"), useCase(plan(BudgetCycle.WEEKLY), listOf(budget("70", "50")),
            emptyList(), LocalDate.of(2026, 9, 20)))
        // A week crossing the month boundary still protects its full remaining envelope.
        assertEquals(money("20"), useCase(plan(BudgetCycle.WEEKLY), listOf(budget("70", "50")),
            emptyList(), LocalDate.of(2026, 9, 30)))
    }

    private fun plan(cycle: BudgetCycle, fixed: String = "0") = FinancialPlan(
        id = "plan", estimatedMonthlyIncome = money("2000"), fixedExpenses = money(fixed), budgetCycle = cycle,
    )
    private fun budget(limit: String, spent: String) = EnvelopeBudgetState(
        envelopeId = "food", name = "Comida", categoryId = "food", weeklyLimit = money(limit),
        spentAmount = money(spent), remainingAmount = (money(limit) - money(spent)).getOrError(),
        percentUsed = 0, status = EnvelopeBudgetStatus.OK,
    )
    private val date = LocalDate.of(2026, 9, 10)
    private fun money(value: String) = Money.of(BigDecimal(value)).getOrError()
}
