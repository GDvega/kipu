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

    fun applyPreset(currentText: String, presetAmount: java.math.BigDecimal): String {
        val trimmed = currentText.trim()
        if (trimmed.isEmpty()) {
            return presetAmount.stripTrailingZeros().toPlainString()
        }
        val currentAmount = when (val parsed = MoneyInputParser.parsePen(trimmed)) {
            is DomainResult.Ok -> parsed.value.amount
            is DomainResult.Err -> null
        }
        return if (currentAmount != null) {
            val newAmount = currentAmount.add(presetAmount)
            if (newAmount.stripTrailingZeros().scale() > 0) {
                newAmount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
            } else {
                newAmount.stripTrailingZeros().toPlainString()
            }
        } else {
            presetAmount.stripTrailingZeros().toPlainString()
        }
    }
}

