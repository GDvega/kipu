package pe.kipu.core.domain.parser

import javax.inject.Inject
import pe.kipu.core.domain.model.NotificationParseResult
import pe.kipu.core.domain.model.PaymentChannel

class NotificationParserRouter @Inject constructor(
    private val yapeIncomeNotificationParser: YapeIncomeNotificationParser,
    private val plinIncomeNotificationParser: PlinIncomeNotificationParser,
) {

    fun parse(channel: PaymentChannel, rawText: String): NotificationParseResult = when (channel) {
        PaymentChannel.YAPE -> yapeIncomeNotificationParser.parse(rawText)
        PaymentChannel.PLIN -> plinIncomeNotificationParser.parse(rawText)
        else -> NotificationParseResult.UnsupportedChannel
    }
}
