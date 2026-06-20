package pe.kipu.core.domain.model

import java.time.Instant

data class GatheringExpense(
    val id: EntityId,
    val gatheringId: EntityId,
    val amount: Money,
    val paidByParticipant: String,
    val description: String? = null,
    val movementId: EntityId? = null,
    val recordedAt: Instant,
) {
    fun validate(): DomainResult<Unit> = when {
        id.isBlank() -> DomainResult.Err(DomainError.InvalidId("Expense id must not be blank"))
        gatheringId.isBlank() -> DomainResult.Err(DomainError.InvalidId("Gathering id must not be blank"))
        paidByParticipant.isBlank() ->
            DomainResult.Err(DomainError.InvalidField("Paid-by participant is required"))
        amount.isZero() -> DomainResult.Err(DomainError.InvalidAmount("Expense amount must be greater than zero"))
        movementId != null && movementId.isBlank() ->
            DomainResult.Err(DomainError.InvalidId("Movement id must not be blank when provided"))
        description != null && description.isBlank() ->
            DomainResult.Err(DomainError.InvalidField("Description must not be blank when provided"))
        else -> DomainResult.Ok(Unit)
    }
}
