package pe.kipu.core.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Represents a single category's slice of total expenses during a period.
 */
data class CategoryExpenseSlice(
    val categoryId: EntityId,
    val categoryName: String,
    val totalAmount: Money,
    val percentage: Float, // Normalized between 0.0f and 1.0f
    val transactionCount: Int,
    val colorIndex: Int,
) {
    val percentageFormatted: String
        get() {
            val pct = (percentage * 100).toInt()
            return "$pct%"
        }
}

/**
 * Aggregated category expense distribution during a budget cycle or period.
 */
data class CategoryExpenseDistribution(
    val totalSpent: Money,
    val slices: List<CategoryExpenseSlice>,
    val topCategory: CategoryExpenseSlice? = slices.firstOrNull(),
    val totalTransactions: Int = slices.sumOf { it.transactionCount },
) {
    val isEmpty: Boolean get() = slices.isEmpty() || totalSpent == Money.ZERO
}
