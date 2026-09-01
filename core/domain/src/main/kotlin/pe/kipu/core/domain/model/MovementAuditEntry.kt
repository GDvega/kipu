package pe.kipu.core.domain.model

import java.time.Instant

enum class MovementAuditAction {
    CREATED,
    UPDATED,
    DELETED,
}

data class MovementAuditEntry(
    val id: String,
    val movementId: String,
    val action: MovementAuditAction,
    val movementType: MovementType,
    val amount: Money,
    val categoryId: EntityId,
    val categoryName: String? = null,
    val channel: PaymentChannel,
    val description: String? = null,
    val counterpartyName: String? = null,
    val details: String? = null,
    val timestamp: Instant,
)
