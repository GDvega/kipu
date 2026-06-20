package pe.kipu.feature.movements.presentation

import pe.kipu.core.domain.duplicate.DuplicateMatchReasonKeys

object MovementDuplicateTranslator {

    fun matchReasonText(matchReasonKey: String): String = when (matchReasonKey) {
        DuplicateMatchReasonKeys.AMOUNT_COUNTERPARTY_TIME ->
            "Mismo monto, destinatario y hora similar"

        DuplicateMatchReasonKeys.OPERATION_NUMBER ->
            "Mismo número de operación"

        else ->
            "Posible movimiento duplicado"
    }
}
