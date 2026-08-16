package pe.kipu.core.domain.util

import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import pe.kipu.core.domain.time.CycleRangeCalculator

object MovementDisplayLabels {
    private val dateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es-PE"))
            .withZone(CycleRangeCalculator.PERU_ZONE)

    private val timeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("h:mm a", Locale.forLanguageTag("es-PE"))

    fun formatDate(instant: Instant): String = dateFormatter.format(instant)

    fun formatDateTime(
        instant: Instant,
        referenceDate: LocalDate = LocalDate.now(CycleRangeCalculator.PERU_ZONE),
    ): String {
        val zoned = instant.atZone(CycleRangeCalculator.PERU_ZONE)
        val date = zoned.toLocalDate()
        return when (date) {
            referenceDate -> "Hoy, ${timeFormatter.format(zoned)}"
            referenceDate.minusDays(1) -> "Ayer, ${timeFormatter.format(zoned)}"
            else -> dateFormatter.format(instant)
        }
    }

    fun displayTitle(counterpartyName: String?, description: String?): String =
        counterpartyName?.takeIf { it.isNotBlank() }
            ?: description?.takeIf { it.isNotBlank() }
            ?: "Movimiento"
}
