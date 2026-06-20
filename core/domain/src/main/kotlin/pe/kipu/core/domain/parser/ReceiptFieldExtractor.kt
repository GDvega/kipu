package pe.kipu.core.domain.parser

import pe.kipu.core.domain.model.SuggestionConfidence
import java.time.Instant

internal object ReceiptConfidenceResolver {

    fun resolve(
        operationReference: String?,
        hasDate: Boolean,
        hasTime: Boolean,
    ): SuggestionConfidence = when {
        operationReference != null -> SuggestionConfidence.HIGH
        hasDate && hasTime -> SuggestionConfidence.HIGH
        else -> SuggestionConfidence.LOW
    }
}

internal object ReceiptFieldExtractor {

    private val COUNTERPARTY_PATTERN = Regex(
        """(?i)\b(?:para|a)\s+([A-ZÁÉÍÓÚÑ][A-ZÁÉÍÓÚÑ0-9\s\.\-]{2,60}?)(?=\s+\d{1,2}\s|\s+\d{1,2}/|\s+nro|\s+mensaje|\s+operaci|\s*$)""",
    )

    private val OPERATION_PATTERN = Regex(
        """(?i)(?:nro\.?\s*de\s*operaci[oó]n|operaci[oó]n)\s+([A-Z0-9\-]{4,24})""",
    )

    private val MESSAGE_PATTERN = Regex(
        """(?i)\bmensaje\s+(.+)$""",
    )

    private val DATE_PATTERN = Regex(
        """(?i)(\d{1,2}\s+(?:ene|feb|mar|abr|may|jun|jul|ago|sep|set|oct|nov|dic)\w*\.?\s+\d{4}|\d{1,2}/\d{1,2}/\d{4})""",
    )

    private val TIME_PATTERN = Regex(
        """(?i)(\d{1,2}:\d{2}\s*(?:a\.?\s*m\.?|p\.?\s*m\.?)?)""",
    )

    fun extractCounterparty(text: String): String? =
        COUNTERPARTY_PATTERN.find(text)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.takeIf { it.isNotBlank() }

    fun extractOperationReference(text: String): String? =
        OPERATION_PATTERN.find(text)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.takeIf { it.isNotBlank() }

    fun extractMessage(text: String): String? =
        MESSAGE_PATTERN.find(text)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.takeIf { it.isNotBlank() }

    fun extractDateTime(text: String): Instant? = ReceiptDateTimeParser.parse(text)

    fun hasDateSignal(text: String): Boolean = DATE_PATTERN.containsMatchIn(text)

    fun hasTimeSignal(text: String): Boolean = TIME_PATTERN.containsMatchIn(text)

    fun createDraftId(channel: pe.kipu.core.domain.model.PaymentChannel, operationReference: String?, amountCents: Long?): String {
        operationReference?.let { return "draft-op-$it" }
        return "draft-${channel.name.lowercase()}-${amountCents ?: 0L}"
    }

    fun amountCents(amount: pe.kipu.core.domain.model.Money): Long =
        amount.amount.movePointRight(2).longValueExact()
}
