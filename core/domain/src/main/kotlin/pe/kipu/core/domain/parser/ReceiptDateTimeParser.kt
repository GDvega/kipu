package pe.kipu.core.domain.parser

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Parses receipt date/time strings from Peruvian OCR text into [Instant].
 *
 * Uses [PERU_ZONE] (America/Lima). Returns null when only a date is found without a time;
 * date-only receipts do not produce midnight defaults.
 */
object ReceiptDateTimeParser {

    private val PERU_ZONE: ZoneId = ZoneId.of("America/Lima")

    private val SPANISH_DATE_TIME = Regex(
        """(?i)(\d{1,2})\s+(ene|feb|mar|abr|may|jun|jul|ago|sep|set|oct|nov|dic)\w*\.?\s+(\d{4})\s*-\s*(\d{1,2}):(\d{2})\s*(a\.?\s*m\.?|p\.?\s*m\.?)""",
    )

    private val SLASH_DATE_TIME = Regex(
        """(\d{1,2})/(\d{1,2})/(\d{4})\s+(\d{1,2}):(\d{2})""",
    )

    private val MONTH_NUMBERS: Map<String, Int> = mapOf(
        "ene" to 1,
        "feb" to 2,
        "mar" to 3,
        "abr" to 4,
        "may" to 5,
        "jun" to 6,
        "jul" to 7,
        "ago" to 8,
        "sep" to 9,
        "set" to 9,
        "oct" to 10,
        "nov" to 11,
        "dic" to 12,
    )

    fun parse(text: String): Instant? {
        SPANISH_DATE_TIME.find(text)?.let { match ->
            val day = match.groupValues[1].toInt()
            val month = MONTH_NUMBERS[match.groupValues[2].lowercase().take(3)] ?: return null
            val year = match.groupValues[3].toInt()
            val hour = to24Hour(
                hour = match.groupValues[4].toInt(),
                amPm = match.groupValues[6],
            )
            val minute = match.groupValues[5].toInt()
            return toInstant(year, month, day, hour, minute)
        }

        SLASH_DATE_TIME.find(text)?.let { match ->
            val day = match.groupValues[1].toInt()
            val month = match.groupValues[2].toInt()
            val year = match.groupValues[3].toInt()
            val hour = match.groupValues[4].toInt()
            val minute = match.groupValues[5].toInt()
            return toInstant(year, month, day, hour, minute)
        }

        return null
    }

    private fun to24Hour(hour: Int, amPm: String): Int {
        val isPm = amPm.contains('p', ignoreCase = true)
        return when {
            isPm && hour < 12 -> hour + 12
            !isPm && hour == 12 -> 0
            else -> hour
        }
    }

    private fun toInstant(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
    ): Instant = ZonedDateTime.of(year, month, day, hour, minute, 0, 0, PERU_ZONE).toInstant()
}
