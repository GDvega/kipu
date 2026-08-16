package pe.kipu.core.domain.util

import java.math.BigDecimal
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.Money

object MoneyInputParser {
    fun parsePen(input: String): DomainResult<Money> {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return DomainResult.Err(pe.kipu.core.domain.model.DomainError.InvalidAmount("Amount is required"))
        }
        val normalized = normalize(trimmed)
            ?: return DomainResult.Err(
                pe.kipu.core.domain.model.DomainError.InvalidAmount("Invalid amount format"),
            )
        return try {
            Money.of(BigDecimal(normalized))
        } catch (_: NumberFormatException) {
            DomainResult.Err(pe.kipu.core.domain.model.DomainError.InvalidAmount("Invalid amount format"))
        }
    }

    private fun normalize(input: String): String? {
        if (input.any { it !in '0'..'9' && it != ',' && it != '.' }) return null

        val commaCount = input.count { it == ',' }
        val dotCount = input.count { it == '.' }
        return when {
            commaCount > 0 && dotCount > 0 -> normalizeMixedSeparators(input)
            commaCount > 0 -> normalizeSingleSeparator(input, ',')
            dotCount > 0 -> normalizeSingleSeparator(input, '.')
            input.all { it in '0'..'9' } -> input
            else -> null
        }
    }

    private fun normalizeMixedSeparators(input: String): String? {
        val decimalSeparator = if (input.lastIndexOf(',') > input.lastIndexOf('.')) ',' else '.'
        val groupingSeparator = if (decimalSeparator == ',') '.' else ','
        val decimalIndex = input.lastIndexOf(decimalSeparator)
        val wholePart = input.substring(0, decimalIndex)
        val fractionalPart = input.substring(decimalIndex + 1)

        if (fractionalPart.length !in 1..2 || fractionalPart.any { it !in '0'..'9' }) return null
        if (wholePart.contains(decimalSeparator)) return null

        val normalizedWhole = normalizeGroupedWhole(wholePart, groupingSeparator) ?: return null
        return "$normalizedWhole.$fractionalPart"
    }

    private fun normalizeSingleSeparator(input: String, separator: Char): String? {
        val parts = input.split(separator)
        if (parts.any { part -> part.isEmpty() || part.any { it !in '0'..'9' } }) return null

        if (parts.size == 2 && parts.last().length in 1..2) {
            return "${parts.first()}.${parts.last()}"
        }
        return if (isValidGroupedNumber(parts)) parts.joinToString(separator = "") else null
    }

    private fun normalizeGroupedWhole(input: String, separator: Char): String? {
        val parts = input.split(separator)
        return if (isValidGroupedNumber(parts)) parts.joinToString(separator = "") else null
    }

    private fun isValidGroupedNumber(parts: List<String>): Boolean =
        parts.size >= 2 &&
            parts.first().length in 1..3 &&
            parts.all { part -> part.isNotEmpty() && part.all { it in '0'..'9' } } &&
            parts.drop(1).all { it.length == 3 }
}
