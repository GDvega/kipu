package pe.kipu.core.domain.parser

import pe.kipu.core.domain.model.ReceiptParseResult

interface ReceiptParser {
    fun parse(rawText: String): ReceiptParseResult
}
