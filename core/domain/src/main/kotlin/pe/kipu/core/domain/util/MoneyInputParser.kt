package pe.kipu.core.domain.util

import java.math.BigDecimal
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.Money

object MoneyInputParser {
    fun parsePen(input: String): DomainResult<Money> {
        val normalized = input.trim().replace(",", "")
        if (normalized.isEmpty()) {
            return DomainResult.Err(pe.kipu.core.domain.model.DomainError.InvalidAmount("Amount is required"))
        }
        return try {
            Money.of(BigDecimal(normalized))
        } catch (_: NumberFormatException) {
            DomainResult.Err(pe.kipu.core.domain.model.DomainError.InvalidAmount("Invalid amount format"))
        }
    }
}
