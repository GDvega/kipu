package pe.kipu.core.domain.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object MovementDisplayLabels {
    private val dateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es-PE"))
            .withZone(ZoneId.systemDefault())

    private val timeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("h:mm a", Locale.forLanguageTag("es-PE"))

    fun formatDate(instant: Instant): String = dateFormatter.format(instant)

    fun formatDateTime(instant: Instant): String {
        val zoned = instant.atZone(ZoneId.systemDefault())
        val date = zoned.toLocalDate()
        val today = LocalDate.now()
        return when (date) {
            today -> "Hoy, ${timeFormatter.format(zoned)}"
            today.minusDays(1) -> "Ayer, ${timeFormatter.format(zoned)}"
            else -> dateFormatter.format(instant)
        }
    }

    fun displayTitle(counterpartyName: String?, description: String?): String =
        counterpartyName?.takeIf { it.isNotBlank() }
            ?: description?.takeIf { it.isNotBlank() }
            ?: "Movimiento"
}
