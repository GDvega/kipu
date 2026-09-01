package pe.kipu.core.domain.usecase

import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.MovementAuditAction
import pe.kipu.core.domain.model.MovementAuditEntry
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.repository.CategoryRepository
import pe.kipu.core.domain.repository.MovementAuditRepository
import pe.kipu.core.domain.repository.MovementRepository
import pe.kipu.core.domain.repository.ReserveEventRepository
import pe.kipu.core.domain.repository.DirectLocalTransactionRunner
import pe.kipu.core.domain.repository.LocalTransactionRunner
import pe.kipu.core.domain.time.TimeProvider

/**
 * Updates an existing movement with modified fields and records an UPDATED audit log.
 * Validates category existence, field constraints, and unlinks commitments if converted to expense.
 */
class UpdateMovementUseCase @Inject constructor(
    private val movementRepository: MovementRepository,
    private val categoryRepository: CategoryRepository,
    private val movementAuditRepository: MovementAuditRepository,
    private val reserveEventRepository: ReserveEventRepository,
    private val timeProvider: TimeProvider,
    private val localTransactionRunner: LocalTransactionRunner = DirectLocalTransactionRunner,
) {
    suspend operator fun invoke(
        movementId: EntityId,
        type: MovementType,
        amount: Money,
        categoryId: EntityId,
        channel: PaymentChannel,
        description: String? = null,
        counterpartyName: String? = null,
        recordedAt: Instant? = null,
    ): Result<Unit> {
        val category = categoryRepository.getById(categoryId)
            ?: return Result.failure(IllegalArgumentException("Category not found"))

        val existing = movementRepository.getById(movementId)
            ?: return Result.failure(IllegalArgumentException("Movement not found"))
        val activeReserveUse = reserveEventRepository.activeUseForMovement(movementId)

        val updatedCommitmentId = if (type == MovementType.EXPENSE) null else existing.commitmentId

        val updatedMovement = existing.copy(
            type = type,
            amount = amount,
            categoryId = categoryId,
            channel = channel,
            description = description?.trim()?.takeIf { it.isNotEmpty() },
            counterpartyName = counterpartyName?.trim()?.takeIf { it.isNotEmpty() },
            commitmentId = updatedCommitmentId,
            recordedAt = recordedAt ?: existing.recordedAt,
        )

        return when (val validation = updatedMovement.validate()) {
            is DomainResult.Err -> Result.failure(IllegalArgumentException(validation.error.message))
            is DomainResult.Ok -> {
                val changes = mutableListOf<String>()
                if (existing.amount != amount) {
                    changes.add("Monto: S/ ${existing.amount.amount.stripTrailingZeros().toPlainString()} → S/ ${amount.amount.stripTrailingZeros().toPlainString()}")
                }
                if (existing.categoryId != categoryId) {
                    changes.add("Categoría: ${category.name}")
                }
                if (existing.channel != channel) {
                    changes.add("Canal: ${channel.name}")
                }
                if (existing.type != type) {
                    changes.add("Tipo: ${type.name}")
                }
                if (existing.description != updatedMovement.description) {
                    changes.add("Descripción actualizada")
                }
                val detailsText = if (changes.isNotEmpty()) changes.joinToString("; ") else "Edición de movimiento"

                val now = timeProvider.now()
                val desiredReserveAmount = if (type == MovementType.EXPENSE) {
                    activeReserveUse?.amount?.takeIf { it.amount <= amount.amount } ?: amount
                } else {
                    Money.ZERO
                }
                val reserveUseChanged = activeReserveUse != null && desiredReserveAmount != activeReserveUse.amount
                val auditEntry = MovementAuditEntry(
                    id = UUID.randomUUID().toString(),
                    movementId = updatedMovement.id,
                    action = MovementAuditAction.UPDATED,
                    movementType = updatedMovement.type,
                    amount = updatedMovement.amount,
                    categoryId = updatedMovement.categoryId,
                    categoryName = category.name,
                    channel = updatedMovement.channel,
                    description = updatedMovement.description,
                    counterpartyName = updatedMovement.counterpartyName,
                    details = detailsText,
                    timestamp = now,
                )
                localTransactionRunner.run {
                    movementRepository.save(updatedMovement).getOrThrow()
                    movementAuditRepository.recordAudit(auditEntry).getOrThrow()
                    if (reserveUseChanged) {
                        reserveEventRepository.record(activeReserveUse.reversal(now)).getOrThrow()
                        desiredReserveAmount.takeUnless(Money::isZero)?.let { reserveAmount ->
                            reserveEventRepository.record(
                                replacementReserveUse(updatedMovement.id, reserveAmount, now),
                            ).getOrThrow()
                        }
                    }
                }.map { Unit }
            }
        }
    }
}
