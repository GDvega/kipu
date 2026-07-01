package pe.kipu.core.domain.plan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PlanWizardDoesNotUseSeedAsUserDataTest {

    @Test
    fun newUserFinancialFieldsStartBlank() {
        val income = PlanWizardStateLoader.incomeDefaults(plan = null)
        val fixed = PlanWizardStateLoader.fixedExpenseDefaults(plan = null)
        val goal = PlanWizardStateLoader.goalDefaults(emergencyGoal = null)

        assertEquals("", income.fixedBaseText)
        assertEquals("", income.approximateIncomeText)
        assertEquals("", income.initialBalanceText)
        assertEquals("", fixed.rentText)
        assertEquals("", fixed.debtsText)
        assertEquals("", goal.goalTargetText)
        assertEquals("", goal.goalCurrentText)
        assertFalse(goal.goalSkipped)
    }
}
