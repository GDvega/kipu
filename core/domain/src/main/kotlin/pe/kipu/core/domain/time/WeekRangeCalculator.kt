package pe.kipu.core.domain.time

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

/**
 * Calculates calendar week ranges in [PERU_ZONE] with weeks starting Monday 00:00:00.
 */
class WeekRangeCalculator @Inject constructor(
    private val timeProvider: TimeProvider,
) {

    fun currentWeekRange(reference: Instant = timeProvider.now()): WeekRange {
        val zoned = reference.atZone(PERU_ZONE)
        val daysFromMonday = zoned.dayOfWeek.value - DayOfWeek.MONDAY.value
        val weekStart = zoned.toLocalDate()
            .minusDays(daysFromMonday.toLong())
            .atStartOfDay(PERU_ZONE)
        val weekEnd = weekStart.plusWeeks(1)
        return WeekRange(
            start = weekStart.toInstant(),
            end = weekEnd.toInstant(),
        )
    }

    companion object {
        val PERU_ZONE: ZoneId = ZoneId.of("America/Lima")
    }
}
