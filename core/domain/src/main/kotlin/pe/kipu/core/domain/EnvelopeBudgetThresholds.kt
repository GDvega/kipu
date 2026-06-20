package pe.kipu.core.domain

/**
 * Thresholds for envelope budget status calculation.
 */
object EnvelopeBudgetThresholds {
    /** Percent used at or above which status becomes [pe.kipu.core.domain.model.EnvelopeBudgetStatus.ADJUSTED]. */
    const val ADJUSTED_PERCENT: Int = 80
}
