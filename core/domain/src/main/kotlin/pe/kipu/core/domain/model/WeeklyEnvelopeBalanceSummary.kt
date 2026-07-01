package pe.kipu.core.domain.model

import java.math.BigDecimal

enum class WeeklyEnvelopeBalanceStatus {
    NO_PLAN,
    BALANCED,
    UNALLOCATED,
    OVER_ALLOCATED,
}

/**
 * Weekly envelope allocation snapshot for zero-sum budgeting UI.
 */
data class WeeklyEnvelopeBalanceSummary(
    val weeklyIncome: Money?,
    val allocated: Money,
    val unallocated: Money?,
    val status: WeeklyEnvelopeBalanceStatus,
)
