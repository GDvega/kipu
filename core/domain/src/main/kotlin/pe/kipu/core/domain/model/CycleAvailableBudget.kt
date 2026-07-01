package pe.kipu.core.domain.model

/**
 * Calculated spending allowance derived from envelope budgets based on the chosen cycle.
 */
data class CycleAvailableBudget(
    val cycle: BudgetCycle,
    val periodLabel: String,
    val cycleRemaining: Money,
    val cycleDeficit: Money?,
    val daysRemainingInCycle: Int,
    val cycleAvailable: Money?,
    val isOverBudget: Boolean,
)
