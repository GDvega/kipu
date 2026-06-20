package pe.kipu.feature.movements.presentation

import pe.kipu.core.domain.model.PaymentChannel

enum class ManualMovementChannelOption(
    val label: String,
    val channel: PaymentChannel,
) {
    CASH("Efectivo", PaymentChannel.CASH),
    YAPE("Yape", PaymentChannel.YAPE),
    PLIN("Plin", PaymentChannel.PLIN),
    BANK("Banco", PaymentChannel.MANUAL),
    OTHER("Otro", PaymentChannel.OTHER),
}
