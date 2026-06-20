package pe.kipu.core.domain.model

/**
 * Aggregated home dashboard insights recalculated from movements and envelopes.
 */
data class HomeInsights(
    val dailyAvailable: DailyAvailableBudget,
    val antSpendingAlerts: List<AntSpendingAlert>,
    val movementCount: Int,
    val envelopeCount: Int,
)
