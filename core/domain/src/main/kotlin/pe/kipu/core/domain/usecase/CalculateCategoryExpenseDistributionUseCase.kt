package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import pe.kipu.core.domain.model.Category
import pe.kipu.core.domain.model.CategoryExpenseDistribution
import pe.kipu.core.domain.model.CategoryExpenseSlice
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.time.CycleRange

class CalculateCategoryExpenseDistributionUseCase @Inject constructor() {

    operator fun invoke(
        movements: List<Movement>,
        categories: List<Category>,
        cycleRange: CycleRange? = null,
    ): CategoryExpenseDistribution {
        val categoryMap = categories.associateBy { it.id }

        val confirmedExpenses = movements.filter { movement ->
            movement.type == MovementType.EXPENSE &&
                movement.status == MovementStatus.CONFIRMED &&
                (cycleRange == null || cycleRange.contains(movement.recordedAt))
        }

        if (confirmedExpenses.isEmpty()) {
            return CategoryExpenseDistribution(
                totalSpent = Money.ZERO,
                slices = emptyList(),
            )
        }

        val totalSpentAmount = confirmedExpenses.fold(Money.ZERO) { acc, mov -> acc + mov.amount }
        if (totalSpentAmount == Money.ZERO) {
            return CategoryExpenseDistribution(
                totalSpent = Money.ZERO,
                slices = emptyList(),
            )
        }

        val grouped = confirmedExpenses.groupBy { it.categoryId }

        val slices = grouped.map { (categoryId, catMovements) ->
            val catTotal = catMovements.fold(Money.ZERO) { acc, mov -> acc + mov.amount }
            val percentage = if (totalSpentAmount.amount > BigDecimal.ZERO) {
                catTotal.amount.divide(totalSpentAmount.amount, 4, RoundingMode.HALF_UP).toFloat()
            } else {
                0.0f
            }
            val categoryName = categoryId?.let { id ->
                categoryMap[id]?.name ?: when (id) {
                    pe.kipu.core.domain.category.CategoryIds.FOOD -> "Comida"
                    pe.kipu.core.domain.category.CategoryIds.TRANSPORT -> "Transporte"
                    pe.kipu.core.domain.category.CategoryIds.SERVICES -> "Servicios"
                    pe.kipu.core.domain.category.CategoryIds.OTHER -> "Otros"
                    else -> id.removePrefix("category-").replaceFirstChar { it.uppercase() }
                }
            } ?: "General"

            CategoryExpenseSlice(
                categoryId = categoryId ?: "general",
                categoryName = categoryName,
                totalAmount = catTotal,
                percentage = percentage,
                transactionCount = catMovements.size,
                colorIndex = 0,
            )
        }
            .sortedByDescending { it.totalAmount.amount }
            .mapIndexed { index, slice ->
                slice.copy(colorIndex = index)
            }

        return CategoryExpenseDistribution(
            totalSpent = totalSpentAmount,
            slices = slices,
            topCategory = slices.firstOrNull(),
            totalTransactions = confirmedExpenses.size,
        )
    }
}
