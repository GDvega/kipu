package pe.kipu.core.domain.parser

import javax.inject.Inject
import pe.kipu.core.domain.model.DomainError
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.ReceiptParseResult
import pe.kipu.core.domain.model.SuggestedMovement

/**
 * Parses Plin payment receipt OCR text using local regex rules.
 *
 * Assumption: payment receipts are expenses ([MovementType.EXPENSE]).
 */
class PlinReceiptParser @Inject constructor() : ReceiptParser {

    override fun parse(rawText: String): ReceiptParseResult {
        val text = ReceiptTextSanitizer.sanitize(rawText)
        if (text.isBlank()) {
            return ReceiptParseResult.Failure(DomainError.InvalidField("Empty receipt text"))
        }
        if (!PLIN_MARKER.containsMatchIn(text)) {
            return ReceiptParseResult.Failure(DomainError.InvalidField("Text does not match Plin receipt"))
        }

        val amount = PenAmountParser.parse(text)
        val counterparty = ReceiptFieldExtractor.extractCounterparty(text)

        if (amount == null || counterparty == null) {
            return ReceiptParseResult.Failure(DomainError.InvalidField("Incomplete Plin receipt"))
        }

        val operationReference = ReceiptFieldExtractor.extractOperationReference(text)
        val message = ReceiptFieldExtractor.extractMessage(text)
        val suggestedRecordedAt = ReceiptFieldExtractor.extractDateTime(text)
        val confidence = ReceiptConfidenceResolver.resolve(
            operationReference = operationReference,
            hasDate = ReceiptFieldExtractor.hasDateSignal(text),
            hasTime = ReceiptFieldExtractor.hasTimeSignal(text),
        )

        val suggestion = SuggestedMovement(
            draftId = ReceiptFieldExtractor.createDraftId(
                channel = PaymentChannel.PLIN,
                operationReference = operationReference,
                amountCents = ReceiptFieldExtractor.amountCents(amount),
            ),
            source = MovementSource.RECEIPT,
            confidence = confidence,
            type = MovementType.EXPENSE,
            amount = amount,
            channel = PaymentChannel.PLIN,
            counterpartyName = counterparty,
            message = message,
            operationReference = operationReference,
            suggestedRecordedAt = suggestedRecordedAt,
        )

        return ReceiptParseResult.Success(suggestion)
    }

    companion object {
        internal val PLIN_MARKER: Regex = Regex("""(?i)\bplin\b""")
    }
}
