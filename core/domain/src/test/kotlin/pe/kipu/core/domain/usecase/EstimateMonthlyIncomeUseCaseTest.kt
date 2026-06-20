package pe.kipu.core.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.plan.PayFrequency

class EstimateMonthlyIncomeUseCaseTest {

    private val useCase = EstimateMonthlyIncomeUseCase()

    @Test
    fun fromFixed_monthly_includesExtras() {
        val result = useCase.fromFixed("1500", PayFrequency.MONTHLY, "300")

        assertTrue(result is DomainResult.Ok)
        assertEquals("1800", (result as DomainResult.Ok).value.amount.stripTrailingZeros().toPlainString())
    }

    @Test
    fun fromFixed_biweekly_doublesBase() {
        val result = useCase.fromFixed("750", PayFrequency.BIWEEKLY, "")

        assertTrue(result is DomainResult.Ok)
        assertEquals("1500", (result as DomainResult.Ok).value.amount.stripTrailingZeros().toPlainString())
    }

    @Test
    fun fromFixed_weekly_quadruplesBase() {
        val result = useCase.fromFixed("400", PayFrequency.WEEKLY, "")

        assertTrue(result is DomainResult.Ok)
        assertEquals("1600", (result as DomainResult.Ok).value.amount.stripTrailingZeros().toPlainString())
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
