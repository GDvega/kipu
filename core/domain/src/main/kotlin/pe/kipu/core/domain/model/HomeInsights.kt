package pe.kipu.core.domain.model

/**
 * Aggregated home dashboard insights recalculated from movements and envelopes.
 */
data class HomeInsights(
    val cycleAvailable: CycleAvailableBudget,
    val antSpendingAlerts: List<AntSpendingAlert>,
    val movementCount: Int,
    val envelopeCount: Int,
    val periodSummary: HomePeriodSummary? = null,
    val recentMovements: List<Movement> = emptyList(),
    val userPreferences: UserPreferences,
    val cashFlowSummary: CashFlowSummary? = null,
    val financialPlan: FinancialPlan? = null,
    val categoryDistribution: CategoryExpenseDistribution? = null,
    val monthlyBudgetSummary: MonthlyBudgetSummary? = null,
    val reserveBalance: ReserveBalance? = null,
    val availableBalance: AvailableBalance? = null,
    val hasCurrentMonthReserveContribution: Boolean = false,
)

data class HomePeriodSummary(
    val totalCycleLimit: Money,
    val totalCycleSpent: Money,
    val daysRemainingInCycle: Int,
)

data class MonthlyBudgetSummary(
    val plannedIncome: Money,
    val actualExpenses: Money,
    val remaining: Money,
    val isOverBudget: Boolean,
)
