package pe.kipu.core.domain.model

/**
 * Result of parsing OCR text from a payment receipt.
 */
sealed interface ReceiptParseResult {
    data class Success(val suggestion: SuggestedMovement) : ReceiptParseResult

    data class Failure(val error: DomainError) : ReceiptParseResult

    data object UnsupportedChannel : ReceiptParseResult
}
