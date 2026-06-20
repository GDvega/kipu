package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.Commitment
import pe.kipu.core.domain.model.CommitmentType
import pe.kipu.core.domain.model.Envelope
import pe.kipu.core.domain.model.FinancialPlan
import pe.kipu.core.domain.model.FinancialPlanValidationResult
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.getOrError

class ValidateFinancialPlanUseCaseTest {

    private val useCase = ValidateFinancialPlanUseCase()

    @Test
    fun `returns valid when income covers outflows`() {
        val plan = plan(income = "5000.00", fixed = "1000.00")
        val envelopes = listOf(envelope("150.00"))
        val commitments = listOf(
            savingsGoal(target = "500.00", current = "100.00"),
            socialDebt("50.00"),
        )

        val result = useCase(plan, envelopes, commitments)

        assertEquals(FinancialPlanValidationResult.Valid, result)
    }

    @Test
    fun `returns invalid with calculated deficit`() {
        val plan = plan(income = "3000.00", fixed = "1800.00")
        val envelopes = listOf(
            envelope("150.00"),
            envelope("80.00"),
            envelope("60.00"),
        )
        val commitments = listOf(
            savingsGoal(target = "500.00", current = "120.00"),
            socialDebt("80.00"),
        )

        val result = useCase(plan, envelopes, commitments)

        assertTrue(result is FinancialPlanValidationResult.Invalid)
        assertEquals(
            Money.of(BigDecimal("420.00")).getOrError(),
            (result as FinancialPlanValidationResult.Invalid).deficit,
        )
    }

    @Test
    fun `returns invalid with full outflows when income is zero`() {
        val plan = plan(income = "0.00", fixed = "1000.00")
        val envelopes = listOf(envelope("100.00"))
        val commitments = emptyList<Commitment>()

        val result = useCase(plan, envelopes, commitments)

        assertTrue(result is FinancialPlanValidationResult.Invalid)
        assertEquals(
            Money.of(BigDecimal("1400.00")).getOrError(),
            (result as FinancialPlanValidationResult.Invalid).deficit,
        )
    }

    @Test
    fun `uses only fixed expenses and envelopes when commitments are empty`() {
        val plan = plan(income = "2000.00", fixed = "500.00")
        val envelopes = listOf(envelope("100.00"))

        val validResult = useCase(plan, envelopes, emptyList())
        assertEquals(FinancialPlanValidationResult.Valid, validResult)

        val tightPlan = plan(income = "800.00", fixed = "500.00")
        val invalidResult = useCase(tightPlan, envelopes, emptyList())
        assertTrue(invalidResult is FinancialPlanValidationResult.Invalid)
        assertEquals(
            Money.of(BigDecimal("100.00")).getOrError(),
            (invalidResult as FinancialPlanValidationResult.Invalid).deficit,
        )
    }

    @Test
    fun `returns structured result without throwing for business cases`() {
        val result = useCase(
            plan = plan(income = "100.00", fixed = "500.00"),
            envelopes = emptyList(),
            commitments = emptyList(),
        )

        assertTrue(
            result is FinancialPlanValidationResult.Invalid ||
                result is FinancialPlanValidationResult.Valid,
        )
    }

    @Test
    fun `converts usd savings goal burden to pen using reference rate`() {
        val plan = plan(income = "5000.00", fixed = "1000.00")
        val envelopes = listOf(envelope("100.00"))
        val usdGoal = savingsGoal(target = "100.00", current = "0.00").copy(currencyCode = "USD")

        val result = useCase(plan, envelopes, listOf(usdGoal))

        assertEquals(FinancialPlanValidationResult.Valid, result)
    }

    private fun plan(income: String, fixed: String): FinancialPlan = FinancialPlan(
        id = "plan-1",
        estimatedMonthlyIncome = Money.of(BigDecimal(income)).getOrError(),
        fixedExpenses = Money.of(BigDecimal(fixed)).getOrError(),
    )

    private fun envelope(weeklyLimit: String): Envelope = Envelope(
        id = "envelope-${weeklyLimit}",
        name = "Sobre",
        weeklyLimit = Money.of(BigDecimal(weeklyLimit)).getOrError(),
        categoryId = CategoryIds.FOOD,
    )

    private fun savingsGoal(target: String, current: String): Commitment = Commitment(
        id = "goal-1",
        type = CommitmentType.SAVINGS_GOAL,
        title = "Meta",
        targetAmount = Money.of(BigDecimal(target)).getOrError(),
        currentAmount = Money.of(BigDecimal(current)).getOrError(),
    )

    private fun socialDebt(amount: String): Commitment = Commitment(
        id = "debt-1",
        type = CommitmentType.SOCIAL_DEBT,
        title = "Deuda",
        currentAmount = Money.of(BigDecimal(amount)).getOrError(),
        counterpartyName = "Juan",
    )
}
