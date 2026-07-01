package pe.kipu.core.domain.time

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

import pe.kipu.core.domain.time.FixedTimeProvider

class CycleRangeCalculatorTest {

    private val peruZone: ZoneId = CycleRangeCalculator.PERU_ZONE
    private val reference = ZonedDateTime.of(2026, 6, 17, 12, 0, 0, 0, peruZone).toInstant()
    private val calculator = CycleRangeCalculator(FixedTimeProvider(reference))

    @Test
    fun `current week starts monday at midnight lima`() {
        val wednesday = ZonedDateTime.of(2026, 6, 17, 15, 30, 0, 0, peruZone).toInstant()

        val range = calculator.currentCycleRange(pe.kipu.core.domain.model.BudgetCycle.WEEKLY, wednesday)

        val expectedStart = ZonedDateTime.of(2026, 6, 15, 0, 0, 0, 0, peruZone).toInstant()
        val expectedEnd = ZonedDateTime.of(2026, 6, 22, 0, 0, 0, 0, peruZone).toInstant()
        assertEquals(expectedStart, range.start)
        assertEquals(expectedEnd, range.end)
    }

    @Test
    fun `monday midnight is inclusive in range`() {
        val monday = ZonedDateTime.of(2026, 6, 15, 0, 0, 0, 0, peruZone).toInstant()
        val range = calculator.currentCycleRange(pe.kipu.core.domain.model.BudgetCycle.WEEKLY, monday)

        assertTrue(range.contains(monday))
    }

    @Test
    fun `next monday midnight is exclusive`() {
        val nextMonday = ZonedDateTime.of(2026, 6, 22, 0, 0, 0, 0, peruZone).toInstant()
        val wednesday = ZonedDateTime.of(2026, 6, 17, 12, 0, 0, 0, peruZone).toInstant()
        val range = calculator.currentCycleRange(pe.kipu.core.domain.model.BudgetCycle.WEEKLY, wednesday)

        assertFalse(range.contains(nextMonday))
    }

    @Test
    fun `sunday late night is still inside week`() {
        val sunday = ZonedDateTime.of(2026, 6, 21, 23, 59, 0, 0, peruZone).toInstant()
        val wednesday = ZonedDateTime.of(2026, 6, 17, 12, 0, 0, 0, peruZone).toInstant()
        val range = calculator.currentCycleRange(pe.kipu.core.domain.model.BudgetCycle.WEEKLY, wednesday)

        assertTrue(range.contains(sunday))
    }
}
