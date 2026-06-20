package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.EnvelopeBudgetState
import pe.kipu.core.domain.model.EnvelopeBudgetStatus
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.plan.DefaultPlanEnvelopeIds

class DetectAntSpendingWeeklyLimitUseCaseTest {

    private val useCase = DetectAntSpendingWeeklyLimitUseCase()

    @Test
    fun `returns threshold reached when spent meets alert percent`() {
        val budget = antBudget(spent = "28.00", limit = "35.00")

        val result = useCase.invoke(
            antEnvelopeBudget = budget,
            alertEnabled = true,
            alertPercent = 80,
            configuredWeeklyLimit = null,
        )

        assertTrue(result is AntSpendingWeeklyLimitStatus.ThresholdReached)
    }

    @Test
    fun `returns below threshold when spent is under alert percent`() {
        val budget = antBudget(spent = "10.00", limit = "35.00")

        val result = useCase.invoke(
            antEnvelopeBudget = budget,
            alertEnabled = true,
            alertPercent = 80,
            configuredWeeklyLimit = null,
        )

        assertTrue(result is AntSpendingWeeklyLimitStatus.BelowThreshold)
    }

    @Test
    fun `returns not configured when alert disabled`() {
        val budget = antBudget(spent = "30.00", limit = "35.00")

        val result = useCase.invoke(
            antEnvelopeBudget = budget,
            alertEnabled = false,
            alertPercent = 80,
            configuredWeeklyLimit = null,
        )

        assertEquals(AntSpendingWeeklyLimitStatus.NotConfigured, result)
    }

    private fun antBudget(spent: String, limit: String): EnvelopeBudgetState = EnvelopeBudgetState(
        envelopeId = DefaultPlanEnvelopeIds.ANT_SPENDING,
        name = "Gastos hormiga",
        categoryId = "category-other",
        weeklyLimit = Money.of(BigDecimal(limit)).getOrError(),
        spentAmount = Money.of(BigDecimal(spent)).getOrError(),
        remainingAmount = Money.of(BigDecimal("5.00")).getOrError(),
        percentUsed = 80,
        status = EnvelopeBudgetStatus.ADJUSTED,
    )
}
