package pe.kipu.core.domain.plan

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.model.Commitment
import pe.kipu.core.domain.model.CommitmentType
import pe.kipu.core.domain.model.FinancialPlan
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.plan.FinancialPlanIds
import pe.kipu.core.domain.plan.IncomeProfile
import pe.kipu.core.domain.plan.PayFrequency

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
    fun `loads goal from emergency commitment`() {
        val commitment = Commitment(
            id = "goal-1",
            type = CommitmentType.SAVINGS_GOAL,
            title = "Viaje a Cusco",
            targetAmount = Money.of(BigDecimal("800")).getOrError(),
            currentAmount = Money.of(BigDecimal("200")).getOrError(),
        )

        val defaults = PlanWizardStateLoader.goalDefaults(commitment)

        assertEquals("Viaje a Cusco", defaults.goalName)
        assertEquals("800", defaults.goalTargetText)
        assertEquals("200", defaults.goalCurrentText)
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
    fun `new user wizard defaults do not use seed amounts as user data`() {
        val income = PlanWizardStateLoader.incomeDefaults(null)
        val fixed = PlanWizardStateLoader.fixedExpenseDefaults(null)
        val goal = PlanWizardStateLoader.goalDefaults(null)

        assertEquals("", income.approximateIncomeText)
        assertEquals("", income.fixedBaseText)
        assertEquals("", income.initialBalanceText)
        assertFalse(fixed.skipFixedExpenses)
        assertEquals("", fixed.educationText)
        assertEquals("", fixed.rentText)
        assertEquals("", fixed.utilitiesText)
        assertEquals("", fixed.phoneText)
        assertEquals("", fixed.debtsText)
        assertEquals("", goal.goalTargetText)
        assertEquals("", goal.goalCurrentText)
    }
}
