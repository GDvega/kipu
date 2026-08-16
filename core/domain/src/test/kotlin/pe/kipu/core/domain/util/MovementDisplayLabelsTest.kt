package pe.kipu.core.domain.util

import java.time.LocalDate
import java.util.TimeZone
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.time.CycleRangeCalculator

class MovementDisplayLabelsTest {

    @Test
    fun formatDateTime_keepsTodayInPeruWhenTheDeviceZoneIsAhead() {
        val originalTimeZone = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Kiritimati"))
            val peruToday = LocalDate.now(CycleRangeCalculator.PERU_ZONE)
            val instant = peruToday.atTime(23, 30).atZone(CycleRangeCalculator.PERU_ZONE).toInstant()

            assertTrue(MovementDisplayLabels.formatDateTime(instant, peruToday).startsWith("Hoy,"))
        } finally {
            TimeZone.setDefault(originalTimeZone)
        }
    }
}
