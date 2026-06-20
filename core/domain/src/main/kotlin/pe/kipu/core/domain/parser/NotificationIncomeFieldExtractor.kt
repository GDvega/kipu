package pe.kipu.core.domain.parser

import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.SuggestionConfidence

internal object NotificationIncomeFieldExtractor {

    private val YAPE_INCOME_SIGNAL = Regex(
        """(?i)\b(?:te\s+yapearon|te\s+yapeó|recibiste|te\s+envió)\b""",
    )

    private val PLIN_INCOME_SIGNAL = Regex(
        """(?i)\b(?:recibiste\s+un\s+plin|recibiste|te\s+envió)\b""",
    )

    private val EXPENSE_SIGNAL = Regex(
        """(?i)\b(?:enviaste|pagaste|yapeaste\s+a|transferiste|pagaste\s+con\s+plin)\b""",
    )

    private val YAPE_SENDER_BEFORE_VERB = Regex(
        """(?i)\b(?!yape\b|plin\b)([\p{L}][\p{L}0-9\s\.\-]{2,60}?)\s+te\s+yapeó""",
    )

    private val FROM_COUNTERPARTY = Regex(
        """(?i)\bde\s+([\p{L}][\p{L}0-9\s\.\-]{2,60}?)(?=\s*$)""",
    )

    private val SENDER_TE_ENVIO = Regex(
        """(?i)\b(?!yape\b|plin\b)([\p{L}][\p{L}0-9\s\.\-]{2,60}?)\s+te\s+envió""",
    )

    fun hasIncomeSignal(text: String, channel: PaymentChannel): Boolean = when (channel) {
        PaymentChannel.YAPE -> YAPE_INCOME_SIGNAL.containsMatchIn(text)
        PaymentChannel.PLIN -> PLIN_INCOME_SIGNAL.containsMatchIn(text)
        else -> false
    }

    fun hasExpenseSignal(text: String): Boolean = EXPENSE_SIGNAL.containsMatchIn(text)

    fun extractCounterparty(text: String, channel: PaymentChannel): String? {
        val candidates = buildList {
            YAPE_SENDER_BEFORE_VERB.findAll(text).forEach { add(it) }
            SENDER_TE_ENVIO.findAll(text).forEach { add(it) }
            FROM_COUNTERPARTY.findAll(text).forEach { add(it) }
        }
        return candidates
            .mapNotNull { it.groupValues.getOrNull(1)?.trim()?.takeIf { name -> name.isNotBlank() } }
            .firstOrNull { name -> !isChannelMarker(name) }
    }

    private fun isChannelMarker(name: String): Boolean =
        name.equals("yape", ignoreCase = true) || name.equals("plin", ignoreCase = true)

    fun resolveConfidence(amount: Money?, hasIncomeSignal: Boolean, counterparty: String?): SuggestionConfidence =
        when {
            amount != null && hasIncomeSignal && !counterparty.isNullOrBlank() -> SuggestionConfidence.HIGH
            amount != null && hasIncomeSignal -> SuggestionConfidence.LOW
            else -> SuggestionConfidence.LOW
        }

    fun createDraftId(
        channel: PaymentChannel,
        operationReference: String?,
        amountCents: Long?,
        counterparty: String?,
    ): String {
        operationReference?.let { return "notif-op-$it" }
        val counterpartyKey = counterparty
            ?.lowercase()
            ?.replace(Regex("""\s+"""), "-")
            ?.take(40)
            ?: "unknown"
        return "notif-${channel.name.lowercase()}-$amountCents-$counterpartyKey"
    }
}
