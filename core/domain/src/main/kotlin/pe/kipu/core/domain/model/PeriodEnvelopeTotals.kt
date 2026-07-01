package pe.kipu.core.domain.model

data class PeriodEnvelopeTotals(
    val totalLimit: Money,
    val totalSpent: Money,
    /** Global slack: `totalLimit - totalSpent`, or zero when over budget. */
    val totalRemaining: Money,
    val cycleDeficit: Money?,
)
