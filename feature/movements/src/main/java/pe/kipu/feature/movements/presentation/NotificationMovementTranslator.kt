package pe.kipu.feature.movements.presentation

import pe.kipu.core.domain.model.PaymentChannel

object NotificationMovementTranslator {

    fun channelLabel(channel: PaymentChannel): String = when (channel) {
        PaymentChannel.YAPE -> "Yape"
        PaymentChannel.PLIN -> "Plin"
        PaymentChannel.CASH -> "Efectivo"
        PaymentChannel.MANUAL -> "Manual"
        PaymentChannel.OTHER -> "Otro"
    }
}
