package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.EnvelopeBudgetState
import pe.kipu.core.domain.model.EnvelopeBudgetStatus
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.time.CycleRange
import pe.kipu.core.domain.time.FixedTimeProvider
import pe.kipu.core.domain.time.CycleRangeCalculator

class CalculateCycleAvailableUseCaseTest {

    private val useCase = CalculateCycleAvailableUseCase(CalculatePeriodEnvelopeTotalsUseCase())
    private val peruZone: ZoneId = CycleRangeCalculator.PERU_ZONE

    @Test
    fun `calculates weekly remaining and divides by days remaining`() {
        val wednesday = ZonedDateTime.of(2026, 6, 17, 10, 0, 0, 0, peruZone).toInstant()
        val cycleRange = cycleRangeForWednesday(wednesday)
        val budgets = listOf(
            budget(
                limit = "100.00",
                spent = "50.00",
                remaining = "50.00",
            ),
        )

        val result = useCase(budgets, wednesday, cycleRange, pe.kipu.core.domain.model.BudgetCycle.WEEKLY)

        assertEquals(Money.of(BigDecimal("50.00")).getOrError(), result.cycleRemaining)
        assertEquals(5, result.daysRemainingInCycle)
        assertEquals(Money.of(BigDecimal("10.00")).getOrError(), result.cycleAvailable)
        assertTrue(!result.isOverBudget)
        assertNull(result.cycleDeficit)
    }

    @Test
    fun `handles over budget without crashing`() {
        val wednesday = ZonedDateTime.of(2026, 6, 17, 10, 0, 0, 0, peruZone).toInstant()
        val cycleRange = cycleRangeForWednesday(wednesday)
        val budgets = listOf(
            budget(
                limit = "100.00",
                spent = "120.00",
                remaining = "0.00",
            ),
        )

        val result = useCase(budgets, wednesday, cycleRange, pe.kipu.core.domain.model.BudgetCycle.WEEKLY)

        assertTrue(result.isOverBudget)
        assertEquals(Money.ZERO, result.cycleRemaining)
        assertEquals(Money.of(BigDecimal("20.00")).getOrError(), result.cycleDeficit)
        assertNull(result.cycleAvailable)
    }

    @Test
    fun `returns null daily available when no envelopes configured`() {
        val wednesday = ZonedDateTime.of(2026, 6, 17, 10, 0, 0, 0, peruZone).toInstant()
        val cycleRange = cycleRangeForWednesday(wednesday)

        val result = useCase(emptyList(), wednesday, cycleRange, pe.kipu.core.domain.model.BudgetCycle.WEEKLY)

        assertNull(result.cycleAvailable)
        assertEquals(5, result.daysRemainingInCycle)
    }

    @Test
    fun `rounds daily available to two decimal places`() {
        val wednesday = ZonedDateTime.of(2026, 6, 17, 10, 0, 0, 0, peruZone).toInstant()
        val cycleRange = cycleRangeForWednesday(wednesday)
        val budgets = listOf(
            budget(
                limit = "100.00",
                spent = "10.00",
                remaining = "90.00",
            ),
        )

        val result = useCase(budgets, wednesday, cycleRange, pe.kipu.core.domain.model.BudgetCycle.WEEKLY)

        assertEquals(Money.of(BigDecimal("18.00")).getOrError(), result.cycleAvailable)
    }

    @Test
    fun `sunday still has one day remaining`() {
        val sunday = ZonedDateTime.of(2026, 6, 21, 18, 0, 0, 0, peruZone).toInstant()
        val cycleRange = cycleRangeForWednesday(
            ZonedDateTime.of(2026, 6, 17, 10, 0, 0, 0, peruZone).toInstant(),
        )
        val budgets = listOf(
            budget(
                limit = "70.00",
                spent = "35.00",
                remaining = "35.00",
            ),
        )

        val result = useCase(budgets, sunday, cycleRange, pe.kipu.core.domain.model.BudgetCycle.WEEKLY)

        assertEquals(1, result.daysRemainingInCycle)
        assertEquals(Money.of(BigDecimal("35.00")).getOrError(), result.cycleAvailable)
    }

    @Test
    fun `uses global remaining when one envelope exceeded but total is not`() {
        val wednesday = ZonedDateTime.of(2026, 6, 17, 10, 0, 0, 0, peruZone).toInstant()
        val cycleRange = cycleRangeForWednesday(wednesday)
        val budgets = listOf(
            budget(
                limit = "100.00",
                spent = "120.00",
                remaining = "0.00",
            ),
            budget(
                limit = "80.00",
                spent = "0.00",
                remaining = "80.00",
            ),
        )

        val result = useCase(budgets, wednesday, cycleRange, pe.kipu.core.domain.model.BudgetCycle.WEEKLY)

        assertEquals(Money.of(BigDecimal("60.00")).getOrError(), result.cycleRemaining)
        assertEquals(Money.of(BigDecimal("12.00")).getOrError(), result.cycleAvailable)
    }

    @Test
    fun `returns null daily available when days remaining is zero`() {
        val nextMonday = ZonedDateTime.of(2026, 6, 22, 0, 0, 0, 0, peruZone).toInstant()
        val cycleRange = cycleRangeForWednesday(
            ZonedDateTime.of(2026, 6, 17, 10, 0, 0, 0, peruZone).toInstant(),
        )
        val budgets = listOf(
            budget(
                limit = "100.00",
                spent = "10.00",
                remaining = "90.00",
            ),
        )

        val result = useCase(budgets, nextMonday, cycleRange, pe.kipu.core.domain.model.BudgetCycle.WEEKLY)

        assertEquals(0, result.daysRemainingInCycle)
        assertNull(result.cycleAvailable)
    }

    private fun cycleRangeForWednesday(wednesday: Instant): CycleRange {
        val calculator = CycleRangeCalculator(FixedTimeProvider(wednesday))
        return calculator.currentCycleRange(pe.kipu.core.domain.model.BudgetCycle.WEEKLY, wednesday)
    }

    private fun budget(
        limit: String,
        spent: String,
        remaining: String,
    ): EnvelopeBudgetState = EnvelopeBudgetState(
        envelopeId = "envelope-food",
        name = "Comida",
        categoryId = CategoryIds.FOOD,
        weeklyLimit = Money.of(BigDecimal(limit)).getOrError(),
        spentAmount = Money.of(BigDecimal(spent)).getOrError(),
        remainingAmount = Money.of(BigDecimal(remaining)).getOrError(),
        percentUsed = 0,
        status = EnvelopeBudgetStatus.OK,
    )
}
