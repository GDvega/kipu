package pe.kipu.core.domain.model

/**
 * Confidence level for inferred movement fields (OCR, parser, notification).
 */
enum class SuggestionConfidence {
    /** Strong suggestion from a reliable parser or OCR match. */
    HIGH,

    /** Weak or partial inference; user review is expected. */
    LOW,
}
