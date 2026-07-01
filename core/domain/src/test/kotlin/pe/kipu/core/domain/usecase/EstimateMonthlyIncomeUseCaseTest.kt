package pe.kipu.core.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.plan.PayFrequency
import pe.kipu.core.domain.plan.PlanWizardLineItem

class EstimateMonthlyIncomeUseCaseTest {

    private val useCase = EstimateMonthlyIncomeUseCase()

    @Test
    fun fromFixed_monthly_includesExtras() {
        val result = useCase.fromFixed("1500", PayFrequency.MONTHLY, extraIncomeText = "300")

        assertTrue(result is DomainResult.Ok)
        assertEquals("1800", (result as DomainResult.Ok).value.amount.stripTrailingZeros().toPlainString())
    }

    @Test
    fun fromFixed_biweekly_sumsBothQuincenas() {
        val result = useCase.fromFixed("750", PayFrequency.BIWEEKLY, secondQuincenaText = "800")

        assertTrue(result is DomainResult.Ok)
        assertEquals("1550", (result as DomainResult.Ok).value.amount.stripTrailingZeros().toPlainString())
    }

    @Test
    fun fromFixed_biweekly_allowsSingleQuincena() {
        val result = useCase.fromFixed("750", PayFrequency.BIWEEKLY, secondQuincenaText = "")

        assertTrue(result is DomainResult.Ok)
        assertEquals("750", (result as DomainResult.Ok).value.amount.stripTrailingZeros().toPlainString())
    }

    @Test
    fun fromFixed_weekly_quadruplesBase() {
        val result = useCase.fromFixed("400", PayFrequency.WEEKLY)

        assertTrue(result is DomainResult.Ok)
        assertEquals("1600", (result as DomainResult.Ok).value.amount.stripTrailingZeros().toPlainString())
    }

    @Test
    fun fromFixed_additionalLines_sumToMonthly() {
        val lines = listOf(
            PlanWizardLineItem(id = "1", label = "Freelance", amountText = "200"),
            PlanWizardLineItem(id = "2", label = "Yapeos", amountText = "100"),
        )
        val result = useCase.fromFixed("1500", PayFrequency.MONTHLY, additionalLines = lines)

        assertTrue(result is DomainResult.Ok)
        assertEquals("1800", (result as DomainResult.Ok).value.amount.stripTrailingZeros().toPlainString())
    }

    @Test
    fun fromVariable_averagesThreeWeeksTimesFour() {
        val result = useCase.fromVariable("250", "400", "650")

        assertTrue(result is DomainResult.Ok)
        assertEquals("1733.33", (result as DomainResult.Ok).value.amount.toPlainString())
    }

    @Test
    fun fromApproximate_returnsAmount() {
        val result = useCase.fromApproximate("1500")

        assertTrue(result is DomainResult.Ok)
        assertEquals("1500", (result as DomainResult.Ok).value.amount.stripTrailingZeros().toPlainString())
    }
}
