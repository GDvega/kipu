package pe.kipu.core.domain.model

import java.time.Instant

/**
 * A confirmed or pending financial movement.
 */
data class Movement(
    val id: EntityId,
    val type: MovementType,
    val amount: Money,
    val categoryId: EntityId,
    val channel: PaymentChannel,
    val source: MovementSource,
    val status: MovementStatus,
    val description: String? = null,
    val counterpartyName: String? = null,
    val operationNumber: String? = null,
    val commitmentId: EntityId? = null,
    val envelopeId: EntityId? = null,
    val recordedAt: Instant,
    val createdAt: Instant,
) {
    fun validate(): DomainResult<Unit> = when {
        id.isBlank() -> DomainResult.Err(DomainError.InvalidId("Movement id must not be blank"))
        categoryId.isBlank() -> DomainResult.Err(DomainError.InvalidId("Category id must not be blank"))
        operationNumber != null && operationNumber.isBlank() ->
            DomainResult.Err(DomainError.InvalidField("Operation number must not be blank when provided"))
        commitmentId != null && commitmentId.isBlank() ->
            DomainResult.Err(DomainError.InvalidId("Commitment id must not be blank when provided"))
        envelopeId != null && envelopeId.isBlank() ->
            DomainResult.Err(DomainError.InvalidId("Envelope id must not be blank when provided"))
        amount.isZero() && status == MovementStatus.CONFIRMED ->
            DomainResult.Err(DomainError.InvalidAmount("Confirmed movement amount must be greater than zero"))
        status == MovementStatus.PENDING_CONFIRMATION && source == MovementSource.MANUAL ->
            DomainResult.Err(DomainError.InvalidField("Manual movements cannot be pending confirmation"))
        else -> DomainResult.Ok(Unit)
    }
}
