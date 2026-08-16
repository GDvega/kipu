package pe.kipu.core.domain.util

import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import pe.kipu.core.domain.time.CycleRangeCalculator

object RelativeDateFormatter {

    private val dayHeaderFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEEE d MMM", Locale.forLanguageTag("es-PE"))

    fun formatDayHeader(
        instant: Instant,
        referenceDate: LocalDate = LocalDate.now(CycleRangeCalculator.PERU_ZONE),
    ): String {
        val date = instant.atZone(CycleRangeCalculator.PERU_ZONE).toLocalDate()
        return when (date) {
            referenceDate -> "Hoy"
            referenceDate.minusDays(1) -> "Ayer"
            else -> dayHeaderFormatter.format(date).replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.forLanguageTag("es-PE")) else char.toString()
            }
        }
    }

    fun dayKey(instant: Instant): LocalDate =
        instant.atZone(CycleRangeCalculator.PERU_ZONE).toLocalDate()
}
