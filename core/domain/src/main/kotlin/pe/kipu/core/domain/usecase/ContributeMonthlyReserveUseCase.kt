package pe.kipu.core.domain.usecase

import java.time.Instant
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.ReserveEvent
import pe.kipu.core.domain.model.ReserveEventType
import pe.kipu.core.domain.repository.ReserveEventRepository
import pe.kipu.core.domain.time.CycleRangeCalculator
import pe.kipu.core.domain.time.TimeProvider

class ContributeMonthlyReserveUseCase @Inject constructor(
    private val reserveEventRepository: ReserveEventRepository,
    private val timeProvider: TimeProvider,
) {
    /** Returns true when a contribution was recorded, false when this month was already covered. */
    suspend operator fun invoke(amount: Money): Result<Boolean> {
        if (amount.isZero()) {
            return Result.failure(IllegalArgumentException("Reserve contribution must be positive"))
        }
        val now = timeProvider.now()
        if (hasActiveMonthlyReserveContribution(reserveEventRepository.observeAll().first(), now)) {
            return Result.success(false)
        }
        return reserveEventRepository.record(
            ReserveEvent(
                id = UUID.randomUUID().toString(),
                type = ReserveEventType.CONTRIBUTION,
                amount = amount,
                occurredAt = now,
                createdAt = now,
            ),
        ).map { true }
    }
}

internal fun hasActiveMonthlyReserveContribution(
    events: List<ReserveEvent>,
    reference: Instant,
): Boolean {
    val month = YearMonth.from(reference.atZone(CycleRangeCalculator.PERU_ZONE))
    val reversedIds = events.asSequence()
        .filter { it.type == ReserveEventType.REVERSAL }
        .mapNotNull { it.reversesEventId }
        .toSet()
    return events.any { event ->
        event.type == ReserveEventType.CONTRIBUTION &&
            event.id !in reversedIds &&
            YearMonth.from(event.occurredAt.atZone(CycleRangeCalculator.PERU_ZONE)) == month
    }
}
