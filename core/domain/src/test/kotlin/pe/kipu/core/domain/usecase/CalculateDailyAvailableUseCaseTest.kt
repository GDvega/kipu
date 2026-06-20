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
import pe.kipu.core.domain.time.WeekRange
import pe.kipu.core.domain.time.FixedTimeProvider
import pe.kipu.core.domain.time.WeekRangeCalculator

class CalculateDailyAvailableUseCaseTest {

    private val useCase = CalculateDailyAvailableUseCase(CalculateWeeklyEnvelopeTotalsUseCase())
    private val peruZone: ZoneId = WeekRangeCalculator.PERU_ZONE

    @Test
    fun `calculates weekly remaining and divides by days remaining`() {
        val wednesday = ZonedDateTime.of(2026, 6, 17, 10, 0, 0, 0, peruZone).toInstant()
        val weekRange = weekRangeForWednesday(wednesday)
        val budgets = listOf(
            budget(
                limit = "100.00",
                spent = "50.00",
                remaining = "50.00",
            ),
        )

        val result = useCase(budgets, wednesday, weekRange)

        assertEquals(Money.of(BigDecimal("50.00")).getOrError(), result.weeklyRemaining)
        assertEquals(5, result.daysRemainingInWeek)
        assertEquals(Money.of(BigDecimal("10.00")).getOrError(), result.dailyAvailable)
        assertTrue(!result.isOverBudget)
        assertNull(result.weeklyDeficit)
    }

    @Test
    fun `handles over budget without crashing`() {
        val wednesday = ZonedDateTime.of(2026, 6, 17, 10, 0, 0, 0, peruZone).toInstant()
        val weekRange = weekRangeForWednesday(wednesday)
        val budgets = listOf(
            budget(
                limit = "100.00",
                spent = "120.00",
                remaining = "0.00",
            ),
        )

        val result = useCase(budgets, wednesday, weekRange)

        assertTrue(result.isOverBudget)
        assertEquals(Money.ZERO, result.weeklyRemaining)
        assertEquals(Money.of(BigDecimal("20.00")).getOrError(), result.weeklyDeficit)
        assertNull(result.dailyAvailable)
    }

    @Test
    fun `returns null daily available when no envelopes configured`() {
        val wednesday = ZonedDateTime.of(2026, 6, 17, 10, 0, 0, 0, peruZone).toInstant()
        val weekRange = weekRangeForWednesday(wednesday)

        val result = useCase(emptyList(), wednesday, weekRange)

        assertNull(result.dailyAvailable)
        assertEquals(5, result.daysRemainingInWeek)
    }

    @Test
    fun `rounds daily available to two decimal places`() {
        val wednesday = ZonedDateTime.of(2026, 6, 17, 10, 0, 0, 0, peruZone).toInstant()
        val weekRange = weekRangeForWednesday(wednesday)
        val budgets = listOf(
            budget(
                limit = "100.00",
                spent = "10.00",
                remaining = "90.00",
            ),
        )

        val result = useCase(budgets, wednesday, weekRange)

        assertEquals(Money.of(BigDecimal("18.00")).getOrError(), result.dailyAvailable)
    }

    @Test
    fun `sunday still has one day remaining`() {
        val sunday = ZonedDateTime.of(2026, 6, 21, 18, 0, 0, 0, peruZone).toInstant()
        val weekRange = weekRangeForWednesday(
            ZonedDateTime.of(2026, 6, 17, 10, 0, 0, 0, peruZone).toInstant(),
        )
        val budgets = listOf(
            budget(
                limit = "70.00",
                spent = "35.00",
                remaining = "35.00",
            ),
        )

        val result = useCase(budgets, sunday, weekRange)

        assertEquals(1, result.daysRemainingInWeek)
        assertEquals(Money.of(BigDecimal("35.00")).getOrError(), result.dailyAvailable)
    }

    @Test
    fun `uses global remaining when one envelope exceeded but total is not`() {
        val wednesday = ZonedDateTime.of(2026, 6, 17, 10, 0, 0, 0, peruZone).toInstant()
        val weekRange = weekRangeForWednesday(wednesday)
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

        val result = useCase(budgets, wednesday, weekRange)

        assertEquals(Money.of(BigDecimal("60.00")).getOrError(), result.weeklyRemaining)
        assertEquals(Money.of(BigDecimal("12.00")).getOrError(), result.dailyAvailable)
    }

    @Test
    fun `returns null daily available when days remaining is zero`() {
        val nextMonday = ZonedDateTime.of(2026, 6, 22, 0, 0, 0, 0, peruZone).toInstant()
        val weekRange = weekRangeForWednesday(
            ZonedDateTime.of(2026, 6, 17, 10, 0, 0, 0, peruZone).toInstant(),
        )
        val budgets = listOf(
            budget(
                limit = "100.00",
                spent = "10.00",
                remaining = "90.00",
            ),
        )

        val result = useCase(budgets, nextMonday, weekRange)

        assertEquals(0, result.daysRemainingInWeek)
        assertNull(result.dailyAvailable)
    }

    private fun weekRangeForWednesday(wednesday: Instant): WeekRange {
        val calculator = WeekRangeCalculator(FixedTimeProvider(wednesday))
        return calculator.currentWeekRange(wednesday)
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
