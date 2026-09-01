package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import javax.inject.Inject
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.ReserveBalance
import pe.kipu.core.domain.model.ReserveEvent
import pe.kipu.core.domain.model.ReserveEventType
import pe.kipu.core.domain.model.getOrError

class CalculateReserveBalanceUseCase @Inject constructor() {
    operator fun invoke(events: List<ReserveEvent>): ReserveBalance {
        val reversedIds = events
            .asSequence()
            .filter { it.type == ReserveEventType.REVERSAL }
            .mapNotNull { it.reversesEventId }
            .toSet()
        val activeEvents = events.filter { it.type != ReserveEventType.REVERSAL && it.id !in reversedIds }
        val totalAddedValue = activeEvents
            .asSequence()
            .filter { it.type == ReserveEventType.CONTRIBUTION || it.type == ReserveEventType.REFUND }
            .fold(BigDecimal.ZERO) { total, event -> total + event.amount.amount }
        val totalUsedValue = activeEvents
            .asSequence()
            .filter { it.type == ReserveEventType.USE }
            .fold(BigDecimal.ZERO) { total, event -> total + event.amount.amount }
        val balance = totalAddedValue - totalUsedValue

        return ReserveBalance(
            totalAdded = Money.of(totalAddedValue).getOrError(),
            totalUsed = Money.of(totalUsedValue).getOrError(),
            balance = balance.setScale(2),
            isOverdrawn = balance.signum() < 0,
        )
    }
}
