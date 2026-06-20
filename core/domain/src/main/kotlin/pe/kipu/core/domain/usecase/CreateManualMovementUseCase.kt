package pe.kipu.core.domain.usecase

import javax.inject.Inject
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.repository.MovementRepository
import pe.kipu.core.domain.time.TimeProvider

/**
 * Persists a user-entered movement as [MovementStatus.CONFIRMED].
 * Used for cash and other channels when no comprobante or notification is available.
 */
class CreateManualMovementUseCase @Inject constructor(
    private val movementRepository: MovementRepository,
    private val timeProvider: TimeProvider,
) {

    suspend operator fun invoke(
        type: MovementType,
        amount: Money,
        categoryId: EntityId,
        channel: PaymentChannel,
        description: String? = null,
        counterpartyName: String? = null,
    ): Result<Unit> {
        val now = timeProvider.now()
        val movement = Movement(
            id = "manual-${now.toEpochMilli()}",
            type = type,
            amount = amount,
            categoryId = categoryId,
            channel = channel,
            source = MovementSource.MANUAL,
            status = MovementStatus.CONFIRMED,
            description = description?.trim()?.takeIf { it.isNotEmpty() },
            counterpartyName = counterpartyName?.trim()?.takeIf { it.isNotEmpty() },
            operationNumber = null,
            recordedAt = now,
            createdAt = now,
        )

        return when (val validation = movement.validate()) {
            is DomainResult.Err -> Result.failure(IllegalArgumentException(validation.error.message))
            is DomainResult.Ok -> movementRepository.save(movement)
        }
    }
}
