package pe.kipu.core.domain.model

data class CashFlowSummary(
    val totalIncome: Money,
    val totalExpenses: Money,
    val netCash: Money,
    val totalGoalTarget: Money,
    val isGoalAtRisk: Boolean,
)
