package pe.kipu.core.domain.usecase

import javax.inject.Inject
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.time.TimeProvider
import pe.kipu.core.domain.time.WeekRangeCalculator

class GetEnvelopeRecentMovementsUseCase @Inject constructor(
    private val weekRangeCalculator: WeekRangeCalculator,
    private val timeProvider: TimeProvider,
) {
    operator fun invoke(
        categoryId: EntityId,
        movements: List<Movement>,
        limit: Int = 3,
    ): List<Movement> {
        val weekRange = weekRangeCalculator.currentWeekRange(timeProvider.now())
        return movements
            .asSequence()
            .filter { movement ->
                movement.categoryId == categoryId &&
                    movement.type == MovementType.EXPENSE &&
                    movement.status == MovementStatus.CONFIRMED &&
                    weekRange.contains(movement.recordedAt)
            }
            .sortedByDescending { it.recordedAt }
            .take(limit)
            .toList()
    }
}
