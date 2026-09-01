package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.model.EnvelopeBudgetState
import pe.kipu.core.domain.model.EnvelopeBudgetStatus
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.UnexpectedExpenseCoverage
import pe.kipu.core.domain.model.UnexpectedExpensePreview
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.plan.DefaultPlanEnvelopeIds

class BuildUnexpectedExpenseRecoveryPlanUseCaseTest {
    private val useCase = BuildUnexpectedExpenseRecoveryPlanUseCase()

    @Test
    fun `proposal reduces discretionary envelopes and protects food and transport`() {
        val result = useCase(
            uncovered = money("200.00"),
            budgets = listOf(
                budget(DefaultPlanEnvelopeIds.FOOD, "Comida", "200.00", "50.00"),
                budget(DefaultPlanEnvelopeIds.TRANSPORT, "Transporte", "100.00", "20.00"),
                budget(DefaultPlanEnvelopeIds.ANT_SPENDING, "Gastos hormiga", "30.00", "10.00"),
                budget(DefaultPlanEnvelopeIds.LEISURE, "Ocio", "100.00", "20.00"),
                budget(DefaultPlanEnvelopeIds.FAMILY, "Familia", "100.00", "50.00"),
            ),
        )

        assertEquals(
            listOf(
                DefaultPlanEnvelopeIds.ANT_SPENDING,
                DefaultPlanEnvelopeIds.LEISURE,
                DefaultPlanEnvelopeIds.FAMILY,
            ),
            result.adjustments.map { it.envelopeId },
        )
        assertEquals(BigDecimal("150.00"), result.adjustments.fold(BigDecimal.ZERO) { total, item ->
            total + item.reduction.amount
        })
        assertEquals(BigDecimal("50.00"), result.remainingGap.amount)
        assertFalse(result.isFullyRecoverable)
    }

    @Test
    fun `proposal never lowers an envelope below what was already spent`() {
        val result = useCase(
            uncovered = money("50.00"),
            budgets = listOf(
                budget(DefaultPlanEnvelopeIds.LEISURE, "Ocio", "100.00", "20.00"),
            ),
        )

        assertEquals(BigDecimal("50.00"), result.adjustments.single().proposedLimit.amount)
        assertTrue(result.adjustments.single().proposedLimit.amount >= BigDecimal("20.00"))
        assertEquals(BigDecimal("0.00"), result.remainingGap.amount)
        assertTrue(result.isFullyRecoverable)
    }

    @Test
    fun `proposal keeps the minimum valid limit when nothing was spent`() {
        val result = useCase(
            uncovered = money("100.00"),
            budgets = listOf(
                budget(DefaultPlanEnvelopeIds.LEISURE, "Ocio", "100.00", "0.00"),
            ),
        )

        assertEquals(BigDecimal("0.01"), result.adjustments.single().proposedLimit.amount)
        assertEquals(BigDecimal("0.01"), result.remainingGap.amount)
        assertFalse(result.isFullyRecoverable)
    }

    @Test
    fun `user selection recomputes the remaining gap without changing original proposal`() {
        val original = useCase(
            uncovered = money("100.00"),
            budgets = listOf(
                budget(DefaultPlanEnvelopeIds.LEISURE, "Ocio", "80.00", "20.00"),
                budget(DefaultPlanEnvelopeIds.FAMILY, "Familia", "80.00", "20.00"),
            ),
        )
        val preview = UnexpectedExpensePreview(
            coverage = UnexpectedExpenseCoverage(
                fromReserve = money("0.00"),
                fromAvailableBalance = money("0.00"),
                uncovered = money("100.00"),
                isFullyCovered = false,
            ),
            recoveryPlan = original,
        )

        val selected = preview.recoveryPlanFor(setOf(DefaultPlanEnvelopeIds.LEISURE))

        assertEquals(listOf(DefaultPlanEnvelopeIds.LEISURE), selected.adjustments.map { it.envelopeId })
        assertEquals(BigDecimal("40.00"), selected.remainingGap.amount)
        assertFalse(selected.isFullyRecoverable)
        assertEquals(2, original.adjustments.size)
    }

    private fun budget(
        id: String,
        name: String,
        limit: String,
        spent: String,
    ): EnvelopeBudgetState {
        val limitMoney = money(limit)
        val spentMoney = money(spent)
        return EnvelopeBudgetState(
            envelopeId = id,
            name = name,
            categoryId = "category-other",
            weeklyLimit = limitMoney,
            spentAmount = spentMoney,
            remainingAmount = Money.of(limitMoney.amount - spentMoney.amount).getOrError(),
            percentUsed = 0,
            status = EnvelopeBudgetStatus.OK,
        )
    }

    private fun money(value: String) = Money.of(BigDecimal(value)).getOrError()
}
