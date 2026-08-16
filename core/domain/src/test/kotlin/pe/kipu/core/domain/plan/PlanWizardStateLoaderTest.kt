package pe.kipu.core.domain.plan

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.model.Commitment
import pe.kipu.core.domain.model.CommitmentType
import pe.kipu.core.domain.model.EnvelopeBudgetState
import pe.kipu.core.domain.model.EnvelopeBudgetStatus
import pe.kipu.core.domain.model.FinancialPlan
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.plan.FinancialPlanIds
import pe.kipu.core.domain.plan.IncomeProfile
import pe.kipu.core.domain.plan.PayFrequency
import pe.kipu.core.domain.usecase.EstimateMonthlyIncomeUseCase

class PlanWizardStateLoaderTest {

    @Test
    fun `loads income from existing plan`() {
        val plan = FinancialPlan(
            id = FinancialPlanIds.PRIMARY,
            estimatedMonthlyIncome = Money.of(BigDecimal("4200.00")).getOrError(),
            fixedExpenses = Money.of(BigDecimal("1500.00")).getOrError(),
            initialBalance = Money.of(BigDecimal("800.00")).getOrError(),
        )

        val defaults = PlanWizardStateLoader.incomeDefaults(plan)

        assertEquals("4200", defaults.approximateIncomeText)
        assertEquals("4200", defaults.fixedBaseText)
        assertEquals("800", defaults.initialBalanceText)
        assertIncomeRoundTrip(plan, defaults)
    }

    @Test
    fun `rehydrates fixed weekly income without changing its monthly total`() {
        val plan = planWithIncome(
            amount = "4500.01",
            profile = IncomeProfile.FIXED,
            frequency = PayFrequency.WEEKLY,
        )

        val defaults = PlanWizardStateLoader.incomeDefaults(plan)

        assertEquals("1125", defaults.fixedBaseText)
        assertEquals("0.01", defaults.extraIncomeText)
        assertIncomeRoundTrip(plan, defaults)
    }

    @Test
    fun `rehydrates fixed biweekly income without changing its monthly total`() {
        val plan = planWithIncome(
            amount = "1550.01",
            profile = IncomeProfile.FIXED,
            frequency = PayFrequency.BIWEEKLY,
        )

        val defaults = PlanWizardStateLoader.incomeDefaults(plan)

        assertEquals("775.01", defaults.fixedBaseText)
        assertEquals("775", defaults.secondQuincenaText)
        assertIncomeRoundTrip(plan, defaults)
    }

    @Test
    fun `rehydrates variable income without changing its monthly total`() {
        val plan = planWithIncome(
            amount = "400.01",
            profile = IncomeProfile.VARIABLE,
            frequency = PayFrequency.WEEKLY,
        )

        val defaults = PlanWizardStateLoader.incomeDefaults(plan)

        assertTrue(defaults.lowWeekText.isNotBlank())
        assertTrue(defaults.normalWeekText.isNotBlank())
        assertTrue(defaults.goodWeekText.isNotBlank())
        assertIncomeRoundTrip(plan, defaults)
    }

    @Test
    fun `rehydrates approximate income without changing its monthly total`() {
        val plan = planWithIncome(
            amount = "2789.43",
            profile = IncomeProfile.APPROXIMATE,
            frequency = PayFrequency.MONTHLY,
        )

        val defaults = PlanWizardStateLoader.incomeDefaults(plan)

        assertIncomeRoundTrip(plan, defaults)
    }

    @Test
    fun `does not invent fixed expense breakdown when total matches old seed`() {
        val plan = FinancialPlan(
            id = FinancialPlanIds.PRIMARY,
            estimatedMonthlyIncome = Money.of(BigDecimal("3000")).getOrError(),
            fixedExpenses = Money.of(PeruPlanDefaults.SEED_FIXED_EXPENSES_MONTHLY).getOrError(),
        )

        val defaults = PlanWizardStateLoader.fixedExpenseDefaults(plan)

        assertFalse(defaults.skipFixedExpenses)
        assertEquals("", defaults.rentText)
        assertEquals("1800", defaults.debtsText)
    }

    @Test
    fun `loads custom fixed total into debts field`() {
        val plan = FinancialPlan(
            id = FinancialPlanIds.PRIMARY,
            estimatedMonthlyIncome = Money.of(BigDecimal("3000")).getOrError(),
            fixedExpenses = Money.of(BigDecimal("2200.00")).getOrError(),
        )

        val defaults = PlanWizardStateLoader.fixedExpenseDefaults(plan)

        assertEquals("2200", defaults.debtsText)
        assertEquals("", defaults.rentText)
    }

    @Test
    fun `rehydrates goal currency and horizon without changing them`() {
        val commitment = Commitment(
            id = "goal-1",
            type = CommitmentType.SAVINGS_GOAL,
            title = "Ahorro para viaje",
            targetAmount = Money.of(BigDecimal("800")).getOrError(),
            currentAmount = Money.of(BigDecimal("200")).getOrError(),
            currencyCode = GoalCurrency.USD.code,
            savingsHorizonMonths = 11,
        )

        val defaults = PlanWizardStateLoader.goalDefaults(commitment)

        assertEquals("Ahorro para viaje", defaults.goalName)
        assertEquals("800", defaults.goalTargetText)
        assertEquals("200", defaults.goalCurrentText)
        assertEquals(GoalType.DOLLARS, defaults.goalType)
        assertEquals("11", defaults.goalMonthsText)
        assertEquals(commitment.currencyCode, defaults.goalType.currency().code)
    }

    @Test
    fun `does not invent a horizon for an existing legacy goal`() {
        val commitment = Commitment(
            id = "goal-legacy",
            type = CommitmentType.SAVINGS_GOAL,
            title = "Meta anterior",
            targetAmount = money("500"),
        )

        val defaults = PlanWizardStateLoader.goalDefaults(commitment)

        assertEquals("", defaults.goalMonthsText)
    }

    @Test
    fun `loads income profile from existing plan`() {
        val plan = FinancialPlan(
            id = FinancialPlanIds.PRIMARY,
            estimatedMonthlyIncome = Money.of(BigDecimal("3000")).getOrError(),
            fixedExpenses = Money.ZERO,
            incomeProfile = IncomeProfile.VARIABLE,
            payFrequency = PayFrequency.BIWEEKLY,
        )

        val defaults = PlanWizardStateLoader.incomeProfileDefaults(plan)

        assertEquals(IncomeProfile.VARIABLE, defaults.incomeProfile)
        assertEquals(PayFrequency.BIWEEKLY, defaults.payFrequency)
    }

    @Test
    fun `income profile defaults to fixed monthly when plan is null`() {
        val defaults = PlanWizardStateLoader.incomeProfileDefaults(null)

        assertEquals(IncomeProfile.FIXED, defaults.incomeProfile)
        assertEquals(PayFrequency.MONTHLY, defaults.payFrequency)
    }

    @Test
    fun `detects existing plan`() {
        assertTrue(
            PlanWizardStateLoader.hasExistingPlan(
                FinancialPlan(
                    id = FinancialPlanIds.PRIMARY,
                    estimatedMonthlyIncome = Money.of(BigDecimal("1000")).getOrError(),
                    fixedExpenses = Money.ZERO,
                ),
            ),
        )
        assertFalse(PlanWizardStateLoader.hasExistingPlan(null))
    }

    @Test
    fun `loads only wizard managed custom envelopes referenced by the plan`() {
        val plan = FinancialPlan(
            id = FinancialPlanIds.PRIMARY,
            estimatedMonthlyIncome = money("3000"),
            fixedExpenses = money("1000"),
            envelopeIds = listOf("envelope-plan-pets", "envelope-manual", "envelope-plan-orphan"),
        )
        val budgets = listOf(
            budget("envelope-plan-pets", "Mascota", "category-pets", "35"),
            budget("envelope-manual", "Manual", "category-manual", "50"),
            budget("envelope-plan-not-linked", "No enlazado", "category-other", "25"),
        )

        val lines = PlanWizardStateLoader.customEnvelopeDefaults(plan, budgets)

        assertEquals(
            listOf(PlanWizardLineItem("envelope-plan-pets", "Mascota", "35", "category-pets")),
            lines,
        )
    }

    @Test
    fun `new user wizard defaults do not use seed amounts as user data`() {
        val income = PlanWizardStateLoader.incomeDefaults(null)
        val fixed = PlanWizardStateLoader.fixedExpenseDefaults(null)
        val goal = PlanWizardStateLoader.goalDefaults(null)

        assertEquals("", income.approximateIncomeText)
        assertEquals("", income.fixedBaseText)
        assertEquals("", income.secondQuincenaText)
        assertEquals("", income.extraIncomeText)
        assertEquals("", income.lowWeekText)
        assertEquals("", income.normalWeekText)
        assertEquals("", income.goodWeekText)
        assertEquals("", income.initialBalanceText)
        assertFalse(fixed.skipFixedExpenses)
        assertEquals("", fixed.electricityText)
        assertEquals("", fixed.waterText)
        assertEquals("", fixed.internetText)
        assertEquals("", fixed.rentText)
        assertEquals("", fixed.phoneText)
        assertEquals("", fixed.debtsText)
        assertEquals("", fixed.educationText)

        assertEquals("", goal.goalTargetText)
        assertEquals("", goal.goalCurrentText)
        assertEquals(GoalType.EMERGENCY, goal.goalType)
        assertEquals("5", goal.goalMonthsText)
    }

    private fun budget(id: String, name: String, categoryId: String, limit: String) =
        EnvelopeBudgetState(
            envelopeId = id,
            name = name,
            categoryId = categoryId,
            weeklyLimit = money(limit),
            spentAmount = Money.ZERO,
            remainingAmount = money(limit),
            percentUsed = 0,
            status = EnvelopeBudgetStatus.OK,
        )

    private fun money(value: String): Money = Money.of(BigDecimal(value)).getOrError()

    private fun planWithIncome(
        amount: String,
        profile: IncomeProfile,
        frequency: PayFrequency,
    ) = FinancialPlan(
        id = FinancialPlanIds.PRIMARY,
        estimatedMonthlyIncome = money(amount),
        fixedExpenses = Money.ZERO,
        incomeProfile = profile,
        payFrequency = frequency,
    )

    private fun assertIncomeRoundTrip(
        plan: FinancialPlan,
        defaults: PlanWizardStateLoader.IncomeDefaults,
    ) {
        val restored = EstimateMonthlyIncomeUseCase().estimate(
            profile = plan.incomeProfile,
            fixedBaseText = defaults.fixedBaseText,
            frequency = plan.payFrequency,
            secondQuincenaText = defaults.secondQuincenaText,
            extraIncomeText = defaults.extraIncomeText,
            lowWeekText = defaults.lowWeekText,
            normalWeekText = defaults.normalWeekText,
            goodWeekText = defaults.goodWeekText,
            approximateText = defaults.approximateIncomeText,
        ).getOrError()

        assertEquals(plan.estimatedMonthlyIncome, restored)
    }
}
