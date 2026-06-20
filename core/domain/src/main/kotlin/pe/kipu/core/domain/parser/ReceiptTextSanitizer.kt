package pe.kipu.core.domain.parser

/**
 * Sanitizes untrusted OCR text before regex parsing.
 */
object ReceiptTextSanitizer {
    const val MAX_LENGTH: Int = 20_000

    fun sanitize(raw: String): String =
        raw.trim()
            .replace(Regex("\\s+"), " ")
            .take(MAX_LENGTH)
}
