package pe.kipu.app.widget

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyAvailableWidgetTimestampFormatterTest {

    @Test
    fun showsTheSnapshotDateAndTime() {
        val label = formatDailyAvailableWidgetUpdatedAt(
            updatedAtMillis = Instant.parse("2026-08-13T15:30:00Z").toEpochMilli(),
            zoneId = ZoneId.of("America/Lima"),
        )

        assertEquals("Actualizado 13/08/2026, 10:30", label)
    }

    @Test
    fun marksLegacySnapshotsWithoutATimestampAsNotUpdated() {
        assertEquals("Sin actualizar", formatDailyAvailableWidgetUpdatedAt(null))
    }
}
