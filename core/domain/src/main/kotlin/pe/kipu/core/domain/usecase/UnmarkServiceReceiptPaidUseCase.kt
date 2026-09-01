package pe.kipu.core.domain.usecase

import pe.kipu.core.domain.model.MovementAuditAction
import pe.kipu.core.domain.model.MovementAuditEntry
import pe.kipu.core.domain.receipt.MonthlyServiceReceipt
import pe.kipu.core.domain.repository.MonthlyServiceReceiptRepository
import pe.kipu.core.domain.repository.MovementRepository
import pe.kipu.core.domain.time.TimeProvider
import java.util.UUID
import javax.inject.Inject

class UnmarkServiceReceiptPaidUseCase @Inject constructor(
    private val monthlyServiceReceiptRepository: MonthlyServiceReceiptRepository,
    private val movementRepository: MovementRepository,
    private val timeProvider: TimeProvider,
) {

    suspend operator fun invoke(receipt: MonthlyServiceReceipt) {
        val now = timeProvider.now()
        val paidMovementId = receipt.paidMovementId

        val existingMovement = paidMovementId?.let { movementRepository.getById(it) }
        val auditEntry = if (paidMovementId != null && existingMovement != null) {
            MovementAuditEntry(
                id = UUID.randomUUID().toString(),
                movementId = paidMovementId,
                action = MovementAuditAction.DELETED,
                movementType = existingMovement.type,
                amount = existingMovement.amount,
                categoryId = existingMovement.categoryId,
                channel = existingMovement.channel,
                description = existingMovement.description,
                counterpartyName = receipt.title,
                details = "Desmarcado de servicio / gasto fijo: ${receipt.title}",
                timestamp = now,
            )
        } else null

        val updatedReceipt = receipt.copy(
            isPaid = false,
            paidMovementId = null,
            paidAt = null,
        )

        monthlyServiceReceiptRepository.unmarkPaid(
            receipt = updatedReceipt,
            movementId = paidMovementId,
            auditEntry = auditEntry,
        ).getOrThrow()
    }
}
