package pe.kipu.core.domain.model

import java.math.BigDecimal

data class CashFlowSummary(
    val totalIncome: Money,
    val totalExpenses: Money,
    /** Signed balance: initial balance + income - expenses. Unlike [Money], this can be negative. */
    val netCash: BigDecimal,
    val totalGoalRemaining: Money,
    val isGoalAtRisk: Boolean,
)
