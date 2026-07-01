package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.EnvelopeBudgetState
import pe.kipu.core.domain.model.EnvelopeBudgetStatus
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.getOrError

class CalculatePeriodEnvelopeTotalsUseCaseTest {

    private val useCase = CalculatePeriodEnvelopeTotalsUseCase()

    @Test
    fun `uses global remaining instead of summing per envelope remainings`() {
        val budgets = listOf(
            budget(
                categoryId = CategoryIds.FOOD,
                limit = "100.00",
                spent = "120.00",
                remaining = "0.00",
            ),
            budget(
                categoryId = CategoryIds.TRANSPORT,
                limit = "80.00",
                spent = "0.00",
                remaining = "80.00",
            ),
        )

        val totals = useCase(budgets)

        assertEquals(Money.of(BigDecimal("180.00")).getOrError(), totals.totalLimit)
        assertEquals(Money.of(BigDecimal("120.00")).getOrError(), totals.totalSpent)
        assertEquals(Money.of(BigDecimal("60.00")).getOrError(), totals.totalRemaining)
        assertNull(totals.cycleDeficit)
    }

    @Test
    fun `returns weekly deficit when global spending exceeds total limit`() {
        val budgets = listOf(
            budget(
                categoryId = CategoryIds.FOOD,
                limit = "100.00",
                spent = "150.00",
                remaining = "0.00",
            ),
        )

        val totals = useCase(budgets)

        assertEquals(Money.ZERO, totals.totalRemaining)
        assertEquals(Money.of(BigDecimal("50.00")).getOrError(), totals.cycleDeficit)
    }

    private fun budget(
        categoryId: String,
        limit: String,
        spent: String,
        remaining: String,
    ): EnvelopeBudgetState = EnvelopeBudgetState(
        envelopeId = "envelope-$categoryId",
        name = categoryId,
        categoryId = categoryId,
        weeklyLimit = Money.of(BigDecimal(limit)).getOrError(),
        spentAmount = Money.of(BigDecimal(spent)).getOrError(),
        remainingAmount = Money.of(BigDecimal(remaining)).getOrError(),
        percentUsed = 0,
        status = EnvelopeBudgetStatus.OK,
    )
}
