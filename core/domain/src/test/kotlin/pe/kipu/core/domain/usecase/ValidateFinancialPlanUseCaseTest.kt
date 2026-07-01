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
            Money.of(BigDecimal("116.00")).getOrError(),
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
    fun `analyze monthly surplus matches validation and includes commitments`() {
        val plan = plan(income = "3300.00", fixed = "1800.00")
        val envelopes = listOf(
            envelope("80.00"),
            envelope("30.00"),
            envelope("40.00"),
            envelope("100.00"),
            envelope("35.00"),
        )
        val commitments = listOf(
            savingsGoal(target = "500.00", current = "120.00"),
        )

        val breakdown = useCase.analyze(plan, envelopes, commitments)

        assertEquals(FinancialPlanValidationResult.Valid, breakdown.validation)
        assertEquals(
            BigDecimal("284.00"),
            breakdown.monthlySurplus.setScale(2),
        )
    }

    @Test
    fun `savings goal uses monthly quota instead of full remaining target`() {
        val plan = plan(income = "3500.00", fixed = "1800.00")
        val envelopes = listOf(
            envelope("80.00"),
            envelope("30.00"),
            envelope("40.00"),
            envelope("100.00"),
            envelope("35.00"),
        )
        val fullTargetBurden = useCase.analyze(
            plan = plan,
            envelopes = envelopes,
            commitments = listOf(
                savingsGoal(target = "1000.00", current = "150.00", horizonMonths = 1),
            ),
        )
        val quotaBurden = useCase.analyze(
            plan = plan,
            envelopes = envelopes,
            commitments = listOf(
                savingsGoal(target = "1000.00", current = "150.00", horizonMonths = 5),
            ),
        )

        assertTrue(fullTargetBurden.validation is FinancialPlanValidationResult.Invalid)
        assertEquals(FinancialPlanValidationResult.Valid, quotaBurden.validation)
    }

    @Test
    fun `linked income on savings goal reduces monthly burden`() {
        val plan = plan(income = "3000.00", fixed = "1800.00")
        val envelopes = listOf(
            envelope("150.00"),
            envelope("80.00"),
            envelope("60.00"),
        )
        val goal = savingsGoal(target = "1000.00", current = "150.00", horizonMonths = 5)
        val withoutLinked = useCase.analyze(plan, envelopes, listOf(goal))
        val withLinked = useCase.analyze(
            plan = plan,
            envelopes = envelopes,
            commitments = listOf(goal),
            linkedIncomeByCommitmentId = mapOf(goal.id to Money.of(BigDecimal("200.00")).getOrError()),
        )

        assertTrue(withoutLinked.commitmentsBurden.amount > withLinked.commitmentsBurden.amount)
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

    private fun savingsGoal(
        target: String,
        current: String,
        horizonMonths: Int? = null,
    ): Commitment = Commitment(
        id = "goal-1",
        type = CommitmentType.SAVINGS_GOAL,
        title = "Meta",
        targetAmount = Money.of(BigDecimal(target)).getOrError(),
        currentAmount = Money.of(BigDecimal(current)).getOrError(),
        savingsHorizonMonths = horizonMonths,
    )

    private fun socialDebt(amount: String): Commitment = Commitment(
        id = "debt-1",
        type = CommitmentType.SOCIAL_DEBT,
        title = "Deuda",
        currentAmount = Money.of(BigDecimal(amount)).getOrError(),
        counterpartyName = "Juan",
    )
}
