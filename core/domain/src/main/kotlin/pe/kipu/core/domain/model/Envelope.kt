package pe.kipu.core.domain.model

/**
 * Weekly envelope budget linked to a category.
 *
 * [spentAmount] is not loaded from Room; use [pe.kipu.core.domain.model.EnvelopeBudgetState.spentAmount]
 * from envelope budget UseCases for the current week.
 */
data class Envelope(
    val id: EntityId,
    val name: String,
    val weeklyLimit: Money,
    val categoryId: EntityId,
    val spentAmount: Money = Money.ZERO,
) {
    fun validate(): DomainResult<Unit> = when {
        id.isBlank() -> DomainResult.Err(DomainError.InvalidId("Envelope id must not be blank"))
        name.isBlank() -> DomainResult.Err(DomainError.InvalidField("Envelope name must not be blank"))
        categoryId.isBlank() -> DomainResult.Err(DomainError.InvalidId("Category id must not be blank"))
        weeklyLimit.isZero() -> DomainResult.Err(DomainError.InvalidAmount("Weekly limit must be greater than zero"))
        else -> DomainResult.Ok(Unit)
    }
}
