package pe.kipu.core.domain.parser

import javax.inject.Inject
import pe.kipu.core.domain.model.DomainError
import pe.kipu.core.domain.model.ReceiptParseResult

class ReceiptParserRouter @Inject constructor(
    private val yapeReceiptParser: YapeReceiptParser,
    private val plinReceiptParser: PlinReceiptParser,
) {

    fun parse(rawText: String): ReceiptParseResult {
        val text = ReceiptTextSanitizer.sanitize(rawText)
        if (text.isBlank()) {
            return ReceiptParseResult.Failure(DomainError.InvalidField("Empty receipt text"))
        }

        val isYape = YapeReceiptParser.YAPE_MARKER.containsMatchIn(text)
        val isPlin = PlinReceiptParser.PLIN_MARKER.containsMatchIn(text)

        return when {
            isYape && !isPlin -> yapeReceiptParser.parse(text)
            isPlin && !isYape -> plinReceiptParser.parse(text)
            isYape && isPlin -> {
                val yapeIndex = text.indexOf("yape", ignoreCase = true)
                val plinIndex = text.indexOf("plin", ignoreCase = true)
                if (yapeIndex <= plinIndex) yapeReceiptParser.parse(text) else plinReceiptParser.parse(text)
            }
            else -> ReceiptParseResult.UnsupportedChannel
        }
    }
}
