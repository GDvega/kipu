package pe.kipu.feature.receipts.presentation

import pe.kipu.core.domain.model.DuplicateResolution
import pe.kipu.core.domain.model.Movement

object ReceiptCategorySuggestionTranslator {
    fun toDisplayText(reasonKey: String?): String? = when (reasonKey) {
        null -> null
        "plin_history_match" -> "Sugerido por tu historial en Plin"
        "yape_history_match" -> "Sugerido por tu historial en Yape"
        "receipt_keyword_match" -> "Sugerido por palabras del comprobante"
        else -> "Sugerencia de categoría"
    }
}

fun receiptChannelLabel(channel: pe.kipu.core.domain.model.PaymentChannel): String = when (channel) {
    pe.kipu.core.domain.model.PaymentChannel.YAPE -> "Yape"
    pe.kipu.core.domain.model.PaymentChannel.PLIN -> "Plin"
    pe.kipu.core.domain.model.PaymentChannel.CASH -> "Efectivo"
    pe.kipu.core.domain.model.PaymentChannel.MANUAL -> "Manual"
    pe.kipu.core.domain.model.PaymentChannel.OTHER -> "Otro"
}

fun duplicateMovementSummary(movement: Movement): String {
    val title = movement.counterpartyName ?: movement.description ?: "Movimiento"
    val amount = pe.kipu.core.designsystem.component.formatPenAmountForDisplay(movement.amount.amount)
    return "$title · $amount"
}
