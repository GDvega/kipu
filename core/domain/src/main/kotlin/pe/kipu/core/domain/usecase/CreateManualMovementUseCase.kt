package pe.kipu.core.domain.usecase

import java.util.UUID
import javax.inject.Inject
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementAuditAction
import pe.kipu.core.domain.model.MovementAuditEntry
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.ReserveEvent
import pe.kipu.core.domain.model.ReserveEventType
import pe.kipu.core.domain.repository.MovementAuditRepository
import pe.kipu.core.domain.repository.MovementRepository
import pe.kipu.core.domain.repository.ReserveEventRepository
import pe.kipu.core.domain.repository.DirectLocalTransactionRunner
import pe.kipu.core.domain.repository.LocalTransactionRunner
import pe.kipu.core.domain.time.TimeProvider

/**
 * Persists a user-entered movement as [MovementStatus.CONFIRMED] and records an audit log.
 * Used for cash and other channels when no comprobante or notification is available.
 */
class CreateManualMovementUseCase @Inject constructor(
    private val movementRepository: MovementRepository,
    private val movementAuditRepository: MovementAuditRepository,
    private val timeProvider: TimeProvider,
    private val reserveEventRepository: ReserveEventRepository,
    private val localTransactionRunner: LocalTransactionRunner = DirectLocalTransactionRunner,
) {

    suspend operator fun invoke(
        type: MovementType,
        amount: Money,
        categoryId: EntityId,
        channel: PaymentChannel,
        description: String? = null,
        counterpartyName: String? = null,
        commitmentId: EntityId? = null,
        envelopeId: EntityId? = null,
        reserveAmount: Money = Money.ZERO,
    ): Result<Unit> {
        if (reserveAmount.amount > amount.amount) {
            return Result.failure(IllegalArgumentException("Reserve use cannot exceed the expense"))
        }
        if (!reserveAmount.isZero() && type != MovementType.EXPENSE) {
            return Result.failure(IllegalArgumentException("Reserve can only cover an expense"))
        }
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
            commitmentId = commitmentId,
            envelopeId = envelopeId,
            operationNumber = null,
            recordedAt = now,
            createdAt = now,
        )

        return when (val validation = movement.validate()) {
            is DomainResult.Err -> Result.failure(IllegalArgumentException(validation.error.message))
            is DomainResult.Ok -> {
                val auditEntry = MovementAuditEntry(
                    id = UUID.randomUUID().toString(),
                    movementId = movement.id,
                    action = MovementAuditAction.CREATED,
                    movementType = movement.type,
                    amount = movement.amount,
                    categoryId = movement.categoryId,
                    channel = movement.channel,
                    description = movement.description,
                    counterpartyName = movement.counterpartyName,
                    details = if (movement.type == MovementType.EXPENSE) "Gasto registrado" else "Ingreso registrado",
                    timestamp = now,
                )
                val reserveEvent = reserveAmount.takeUnless(Money::isZero)?.let {
                    ReserveEvent(
                        id = "reserve-use-${movement.id}",
                        type = ReserveEventType.USE,
                        amount = it,
                        sourceMovementId = movement.id,
                        occurredAt = now,
                        createdAt = now,
                    )
                }
                localTransactionRunner.run {
                    movementRepository.save(movement).getOrThrow()
                    movementAuditRepository.recordAudit(auditEntry).getOrThrow()
                    reserveEvent?.let { reserveEventRepository.record(it).getOrThrow() }
                }.map { Unit }
            }
        }
    }
}
