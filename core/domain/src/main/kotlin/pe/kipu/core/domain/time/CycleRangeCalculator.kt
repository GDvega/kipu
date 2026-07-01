package pe.kipu.core.domain.time

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import pe.kipu.core.domain.model.BudgetCycle

/**
 * Calculates calendar ranges in [PERU_ZONE] for daily, weekly, or monthly cycles.
 */
class CycleRangeCalculator @Inject constructor(
    private val timeProvider: TimeProvider,
) {

    fun currentCycleRange(cycle: BudgetCycle, reference: Instant = timeProvider.now()): CycleRange {
        val zoned = reference.atZone(PERU_ZONE)
        
        return when (cycle) {
            BudgetCycle.DAILY -> {
                val dayStart = zoned.toLocalDate().atStartOfDay(PERU_ZONE)
                val dayEnd = dayStart.plusDays(1)
                CycleRange(start = dayStart.toInstant(), end = dayEnd.toInstant())
            }
            BudgetCycle.WEEKLY -> {
                val daysFromMonday = zoned.dayOfWeek.value - DayOfWeek.MONDAY.value
                val weekStart = zoned.toLocalDate()
                    .minusDays(daysFromMonday.toLong())
                    .atStartOfDay(PERU_ZONE)
                val weekEnd = weekStart.plusWeeks(1)
                CycleRange(start = weekStart.toInstant(), end = weekEnd.toInstant())
            }
            BudgetCycle.MONTHLY -> {
                val monthStart = zoned.toLocalDate().with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay(PERU_ZONE)
                val monthEnd = monthStart.plusMonths(1)
                CycleRange(start = monthStart.toInstant(), end = monthEnd.toInstant())
            }
        }
    }

    companion object {
        val PERU_ZONE: ZoneId = ZoneId.of("America/Lima")
    }
}
