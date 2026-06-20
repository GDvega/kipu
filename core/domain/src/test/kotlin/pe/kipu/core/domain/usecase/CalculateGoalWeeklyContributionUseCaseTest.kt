package pe.kipu.core.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.model.DomainResult

class CalculateGoalWeeklyContributionUseCaseTest {

    private val useCase = CalculateGoalWeeklyContributionUseCase()

    @Test
    fun invoke_dividesRemainingByWeeks() {
        val result = useCase.invoke("1000", "150", months = 5)

        assertTrue(result is DomainResult.Ok)
        assertEquals("42.50", (result as DomainResult.Ok).value.amount.toPlainString())
    }

    @Test
    fun invoke_returnsZeroWhenGoalReached() {
        val result = useCase.invoke("500", "500", months = 3)

        assertTrue(result is DomainResult.Ok)
        assertEquals("0", (result as DomainResult.Ok).value.amount.stripTrailingZeros().toPlainString())
    }
}
