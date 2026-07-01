package pe.kipu.feature.movements.presentation

import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.util.MoneyInputParser

object ManualMovementAmountValidator {
    const val EMPTY_AMOUNT_MESSAGE = "Ingresa un monto"
    const val ZERO_AMOUNT_MESSAGE = "El monto debe ser mayor a cero"
    const val INVALID_AMOUNT_MESSAGE = "Revisa el formato del monto"

    fun errorMessage(amountText: String): String? {
        val trimmed = amountText.trim()
        if (trimmed.isEmpty()) return EMPTY_AMOUNT_MESSAGE

        return when (val parsed = MoneyInputParser.parsePen(trimmed)) {
            is DomainResult.Err -> INVALID_AMOUNT_MESSAGE
            is DomainResult.Ok -> if (parsed.value.isZero()) ZERO_AMOUNT_MESSAGE else null
        }
    }

    fun isValid(amountText: String): Boolean = errorMessage(amountText) == null
}
