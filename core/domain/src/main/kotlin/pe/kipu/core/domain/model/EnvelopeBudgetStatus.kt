package pe.kipu.core.domain.model

/**
 * Budget health for a weekly envelope.
 */
enum class EnvelopeBudgetStatus {
    /** Spending is within the weekly limit below the adjusted threshold. */
    OK,

    /** Spending is at or above [pe.kipu.core.domain.EnvelopeBudgetThresholds.ADJUSTED_PERCENT] of the limit. */
    ADJUSTED,

    /** Spending exceeds the weekly limit. */
    EXCEEDED,
}
