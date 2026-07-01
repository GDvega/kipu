package pe.kipu.core.domain.parser

import java.math.BigDecimal
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.Money

/**
 * Extracts PEN amounts from receipt OCR text (e.g. S/ 10.50, S/0.10, S/ 1,234.50, S/ 10.5).
 */
object PenAmountParser {
    private val AMOUNT_PATTERN = Regex(
        """(?i)S/\s*([0-9]{1,3}(?:,[0-9]{3})*|[0-9]+)(?:\.([0-9]{1,2}))?""",
    )

    fun parse(text: String): Money? {
        val match = AMOUNT_PATTERN.find(text) ?: return null
        val wholePart = match.groupValues[1].replace(",", "")
        val fractionalPart = normalizeFractional(match.groupValues.getOrNull(2))
        val decimal = BigDecimal("$wholePart.$fractionalPart")
        return when (val result = Money.of(decimal)) {
            is DomainResult.Ok -> result.value
            is DomainResult.Err -> null
        }
    }

    internal fun normalizeFractional(raw: String?): String {
        if (raw.isNullOrEmpty()) return "00"
        return when (raw.length) {
            1 -> "${raw}0"
            else -> raw.take(2)
        }
    }
}
