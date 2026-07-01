package pe.kipu.core.domain.model

import java.time.LocalDate
import pe.kipu.core.domain.plan.GoalCurrency

/**
 * Savings goal, social debt or pending payment tracked by the user.
 */
data class Commitment(
    val id: EntityId,
    val type: CommitmentType,
    val title: String,
    val targetAmount: Money? = null,
    val currentAmount: Money? = null,
    val dueDate: LocalDate? = null,
    val counterpartyName: String? = null,
    val isSettled: Boolean = false,
    val currencyCode: String = GoalCurrency.PEN.code,
    /** Months to reach [targetAmount]; used for monthly savings quota in plan validation. */
    val savingsHorizonMonths: Int? = null,
) {
    fun validate(): DomainResult<Unit> = when {
        id.isBlank() -> DomainResult.Err(DomainError.InvalidId("Commitment id must not be blank"))
        title.isBlank() -> DomainResult.Err(DomainError.InvalidField("Commitment title must not be blank"))
        type == CommitmentType.SAVINGS_GOAL && targetAmount == null ->
            DomainResult.Err(DomainError.InvalidField("Savings goal requires a target amount"))
        isSettled && type == CommitmentType.PENDING_PAYMENT && currentAmount == null ->
            DomainResult.Err(DomainError.InvalidField("Settled pending payment requires a current amount"))
        else -> DomainResult.Ok(Unit)
    }
}
