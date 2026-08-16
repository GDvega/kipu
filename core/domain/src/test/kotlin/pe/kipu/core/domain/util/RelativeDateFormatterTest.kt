package pe.kipu.core.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone
import pe.kipu.core.domain.time.CycleRangeCalculator

class RelativeDateFormatterTest {

    private val zone = ZoneId.of("America/Lima")

    @Test
    fun formatDayHeader_today() {
        val reference = LocalDate.of(2026, 6, 21)
        val instant = reference.atStartOfDay(zone).toInstant()

        assertEquals("Hoy", RelativeDateFormatter.formatDayHeader(instant, reference))
    }

    @Test
    fun formatDayHeader_yesterday() {
        val reference = LocalDate.of(2026, 6, 21)
        val instant = reference.minusDays(1).atStartOfDay(zone).toInstant()

        assertEquals("Ayer", RelativeDateFormatter.formatDayHeader(instant, reference))
    }

    @Test
    fun formatDayHeader_olderDate_includesWeekday() {
        val reference = LocalDate.of(2026, 6, 21)
        val instant = LocalDate.of(2026, 6, 15).atStartOfDay(zone).toInstant()

        val header = RelativeDateFormatter.formatDayHeader(instant, reference)

        assertEquals(true, header.contains("15"))
    }

    @Test
    fun dayKey_usesLocalDate() {
        val instant = Instant.parse("2026-06-21T15:00:00Z")

        val key = RelativeDateFormatter.dayKey(instant)

        assertEquals(LocalDate.of(2026, 6, 21), key)
    }

    @Test
    fun formatters_keepThePeruCalendarWhenTheDeviceZoneIsAhead() {
        val originalTimeZone = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Kiritimati"))
            val peruToday = LocalDate.now(CycleRangeCalculator.PERU_ZONE)
            val instant = peruToday.atTime(23, 30).atZone(CycleRangeCalculator.PERU_ZONE).toInstant()

            assertEquals("Hoy", RelativeDateFormatter.formatDayHeader(instant, peruToday))
            assertEquals(peruToday, RelativeDateFormatter.dayKey(instant))
        } finally {
            TimeZone.setDefault(originalTimeZone)
        }
    }
}
