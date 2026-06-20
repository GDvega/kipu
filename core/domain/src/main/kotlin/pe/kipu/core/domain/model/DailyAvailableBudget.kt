package pe.kipu.core.domain.model

/**
 * Calculated daily spending allowance derived from weekly envelope budgets.
 */
data class DailyAvailableBudget(
    val weeklyRemaining: Money,
    val weeklyDeficit: Money?,
    val daysRemainingInWeek: Int,
    val dailyAvailable: Money?,
    val isOverBudget: Boolean,
)
