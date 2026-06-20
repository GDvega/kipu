package pe.kipu.core.domain.parser

import javax.inject.Inject
import pe.kipu.core.domain.model.DomainError
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.NotificationParseResult
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.SuggestedMovement

/**
 * Parses Yape **income** notification text using local regex rules.
 *
 * Income signals: "te yapearon", "te yapeó", "recibiste", "te envió".
 * Outgoing payments ("enviaste", "pagaste") are rejected.
 */
class YapeIncomeNotificationParser @Inject constructor() : NotificationParser {

    override fun parse(rawText: String): NotificationParseResult {
        val text = ReceiptTextSanitizer.sanitize(rawText)
        if (text.isBlank()) {
            return NotificationParseResult.Failure(DomainError.InvalidField("Empty notification text"))
        }
        if (!YAPE_MARKER.containsMatchIn(text)) {
            return NotificationParseResult.Failure(DomainError.InvalidField("Text does not match Yape notification"))
        }
        if (NotificationIncomeFieldExtractor.hasExpenseSignal(text)) {
            return NotificationParseResult.Failure(DomainError.InvalidField("Outgoing payment notification"))
        }
        if (!NotificationIncomeFieldExtractor.hasIncomeSignal(text, PaymentChannel.YAPE)) {
            return NotificationParseResult.Failure(DomainError.InvalidField("No income signal in notification"))
        }

        val amount = PenAmountParser.parse(text)
            ?: return NotificationParseResult.Failure(DomainError.InvalidField("Could not extract amount"))
        val counterparty = NotificationIncomeFieldExtractor.extractCounterparty(text, PaymentChannel.YAPE)
        val confidence = NotificationIncomeFieldExtractor.resolveConfidence(
            amount = amount,
            hasIncomeSignal = true,
            counterparty = counterparty,
        )

        val suggestion = SuggestedMovement(
            draftId = NotificationIncomeFieldExtractor.createDraftId(
                channel = PaymentChannel.YAPE,
                operationReference = null,
                amountCents = ReceiptFieldExtractor.amountCents(amount),
                counterparty = counterparty,
            ),
            source = MovementSource.NOTIFICATION,
            confidence = confidence,
            type = MovementType.INCOME,
            amount = amount,
            channel = PaymentChannel.YAPE,
            counterpartyName = counterparty,
        )

        return NotificationParseResult.Success(suggestion)
    }

    companion object {
        internal val YAPE_MARKER: Regex = Regex("""(?i)\byape\b""")
    }
}
