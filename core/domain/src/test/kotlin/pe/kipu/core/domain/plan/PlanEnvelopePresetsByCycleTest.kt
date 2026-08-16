package pe.kipu.core.domain.plan

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.model.BudgetCycle

class PlanEnvelopePresetsByCycleTest {

    @Test
    fun foodPresetsVaryByBudgetCycle() {
        val foodTemplate = PlanEnvelopeTemplates.WIZARD_ENVELOPES.first { it.envelopeId == DefaultPlanEnvelopeIds.FOOD }
        val daily = PlanEnvelopeTemplates.presetsForCycle(foodTemplate, BudgetCycle.DAILY)
        val weekly = PlanEnvelopeTemplates.presetsForCycle(foodTemplate, BudgetCycle.WEEKLY)
        val monthly = PlanEnvelopeTemplates.presetsForCycle(foodTemplate, BudgetCycle.MONTHLY)

        assertEquals(listOf(BigDecimal("15"), BigDecimal("25"), BigDecimal("35")), daily)
        assertEquals(listOf(BigDecimal("80"), BigDecimal("120"), BigDecimal("180")), weekly)
        assertEquals(listOf(BigDecimal("350"), BigDecimal("500"), BigDecimal("750")), monthly)
    }

    @Test
    fun antSpendingPresetsVaryByBudgetCycle() {
        val daily = PlanEnvelopeTemplates.antSpendingPresetsForCycle(BudgetCycle.DAILY)
        val weekly = PlanEnvelopeTemplates.antSpendingPresetsForCycle(BudgetCycle.WEEKLY)
        val monthly = PlanEnvelopeTemplates.antSpendingPresetsForCycle(BudgetCycle.MONTHLY)

        assertTrue(daily.all { it < BigDecimal("20") })
        assertTrue(weekly.all { it >= BigDecimal("20") && it <= BigDecimal("60") })
        assertTrue(monthly.all { it >= BigDecimal("100") })
    }
}
