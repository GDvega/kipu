package pe.kipu.core.domain.plan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.model.DomainResult

class FixedExpenseBreakdownCalculatorTest {

    @Test
    fun sumParts_addsRentUtilitiesAndDebts() {
        val result = FixedExpenseBreakdownCalculator.sumParts("", "900", "350", "", "550")

        assertTrue(result is DomainResult.Ok)
        assertEquals("1800", (result as DomainResult.Ok).value.amount.stripTrailingZeros().toPlainString())
    }

    @Test
    fun sumParts_addsAllFiveCategories() {
        val result = FixedExpenseBreakdownCalculator.sumParts("200", "900", "350", "50", "550")

        assertTrue(result is DomainResult.Ok)
        assertEquals("2050", (result as DomainResult.Ok).value.amount.stripTrailingZeros().toPlainString())
    }

    @Test
    fun sumParts_treatsEmptyFieldsAsZero() {
        val result = FixedExpenseBreakdownCalculator.sumParts("", "500", "", "100", "200")

        assertTrue(result is DomainResult.Ok)
        assertEquals("800", (result as DomainResult.Ok).value.amount.stripTrailingZeros().toPlainString())
    }

    @Test
    fun sumParts_rejectsInvalidAmount() {
        val result = FixedExpenseBreakdownCalculator.sumParts("", "abc", "100", "", "50")

        assertTrue(result is DomainResult.Err)
    }

    @Test
    fun sumAll_includesCustomLines() {
        val custom = listOf(
            PlanWizardLineItem(id = "1", label = "Netflix", amountText = "45"),
            PlanWizardLineItem(id = "2", label = "Spotify", amountText = "15"),
        )
        val result = FixedExpenseBreakdownCalculator.sumAll(
            presetParts = listOf("900", "350", "", "", ""),
            customLines = custom,
        )

        assertTrue(result is DomainResult.Ok)
        assertEquals("1310", (result as DomainResult.Ok).value.amount.stripTrailingZeros().toPlainString())
    }
}
