package pe.kipu.core.domain.usecase

import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.receipt.MonthlyServiceReceipt
import pe.kipu.core.domain.model.MovementAuditAction
import pe.kipu.core.domain.model.MovementAuditEntry
import pe.kipu.core.domain.repository.MonthlyServiceReceiptRepository
import pe.kipu.core.domain.time.TimeProvider
import java.util.UUID
import javax.inject.Inject

class MarkServiceReceiptPaidUseCase @Inject constructor(
    private val monthlyServiceReceiptRepository: MonthlyServiceReceiptRepository,
    private val timeProvider: TimeProvider,
) {

    suspend operator fun invoke(
        receipt: MonthlyServiceReceipt,
        actualAmount: Money? = null,
        channel: PaymentChannel = PaymentChannel.CASH,
        envelopeId: String? = null,
        movementId: String = UUID.randomUUID().toString(),
    ): Movement {
        val now = timeProvider.now()
        val amount = actualAmount ?: receipt.configuredAmount

        val movement = Movement(
            id = movementId,
            type = MovementType.EXPENSE,
            amount = amount,
            categoryId = CategoryIds.SERVICES,
            channel = channel,
            source = pe.kipu.core.domain.model.MovementSource.MANUAL,
            status = MovementStatus.CONFIRMED,
            description = "Pago ${receipt.title}",
            envelopeId = envelopeId,
            recordedAt = now,
            createdAt = now,
        )

        val updatedReceipt = receipt.copy(
            isPaid = true,
            paidMovementId = movement.id,
            paidAt = now,
        )

        val auditEntry = MovementAuditEntry(
            id = UUID.randomUUID().toString(),
            movementId = movement.id,
            action = MovementAuditAction.CREATED,
            movementType = movement.type,
            amount = movement.amount,
            categoryId = movement.categoryId,
            channel = movement.channel,
            description = movement.description,
            counterpartyName = receipt.title,
            details = "Pago de servicio: ${receipt.title}",
            timestamp = now,
        )

        monthlyServiceReceiptRepository.markPaid(
            receipt = updatedReceipt,
            movement = movement,
            auditEntry = auditEntry,
        ).getOrThrow()

        return movement
    }
}
