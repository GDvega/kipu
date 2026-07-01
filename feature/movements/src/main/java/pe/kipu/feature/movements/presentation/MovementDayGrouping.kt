package pe.kipu.feature.movements.presentation

import java.time.LocalDate
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.util.RelativeDateFormatter

data class MovementDayGroup(
    val dayKey: LocalDate,
    val headerLabel: String,
    val movements: List<Movement>,
)

fun groupMovementsByDay(movements: List<Movement>): List<MovementDayGroup> =
    movements
        .groupBy { movement -> RelativeDateFormatter.dayKey(movement.recordedAt) }
        .entries
        .sortedByDescending { entry -> entry.key }
        .map { (day, dayMovements) ->
            MovementDayGroup(
                dayKey = day,
                headerLabel = RelativeDateFormatter.formatDayHeader(dayMovements.first().recordedAt),
                movements = dayMovements.sortedByDescending { movement -> movement.recordedAt },
            )
        }
