package pe.kipu.core.domain.usecase

import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import pe.kipu.core.domain.AntSpendingThresholds
import pe.kipu.core.domain.model.AlertSeverity
import pe.kipu.core.domain.model.AntSpendingAlert
import pe.kipu.core.domain.model.AntSpendingAlertKeys
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType

class DetectAntSpendingUseCase @Inject constructor() {

    operator fun invoke(
        movements: List<Movement>,
        referenceInstant: Instant,
        isOverBudget: Boolean,
    ): List<AntSpendingAlert> {
        val windowStart = referenceInstant.minus(AntSpendingThresholds.WINDOW_HOURS.toLong(), ChronoUnit.HOURS)
        val eligible = movements.filter { movement ->
            movement.type == MovementType.EXPENSE &&
                movement.status == MovementStatus.CONFIRMED &&
                !movement.recordedAt.isBefore(windowStart) &&
                !movement.recordedAt.isAfter(referenceInstant) &&
                movement.amount.amount <= AntSpendingThresholds.MAX_SINGLE_AMOUNT
        }

        return eligible
            .groupBy { movement -> movement.categoryId }
            .mapNotNull { (categoryId, categoryMovements) ->
                if (categoryMovements.size < AntSpendingThresholds.MIN_TRANSACTION_COUNT) {
                    return@mapNotNull null
                }
                val totalAmount = categoryMovements.fold(pe.kipu.core.domain.model.Money.ZERO) { acc, movement ->
                    acc + movement.amount
                }
                AntSpendingAlert(
                    severity = if (isOverBudget) AlertSeverity.RED else AlertSeverity.AMBER,
                    transactionCount = categoryMovements.size,
                    totalAmount = totalAmount,
                    windowHours = AntSpendingThresholds.WINDOW_HOURS,
                    categoryId = categoryId,
                    messageKey = AntSpendingAlertKeys.CATEGORY,
                )
            }
    }
}
