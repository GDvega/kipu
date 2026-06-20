package pe.kipu.core.domain.usecase

import javax.inject.Inject
import pe.kipu.core.domain.model.DomainError
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.GatheringExpense
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.repository.GatheringExpenseRepository
import pe.kipu.core.domain.repository.GatheringRepository
import pe.kipu.core.domain.repository.MovementRepository
import pe.kipu.core.domain.util.GatheringParticipantValidator
import pe.kipu.core.domain.util.MovementDisplayLabels

class LinkMovementToGatheringUseCase @Inject constructor(
    private val gatheringRepository: GatheringRepository,
    private val movementRepository: MovementRepository,
    private val gatheringExpenseRepository: GatheringExpenseRepository,
) {

    suspend operator fun invoke(
        gatheringId: EntityId,
        movementId: EntityId,
        paidByParticipant: String,
    ): DomainResult<GatheringExpense> {
        val gathering = gatheringRepository.getById(gatheringId)
            ?: return DomainResult.Err(DomainError.NotFound("Gathering not found"))

        val movement = movementRepository.getById(movementId)
            ?: return DomainResult.Err(DomainError.NotFound("Movement not found"))

        if (movement.type != MovementType.EXPENSE) {
            return DomainResult.Err(DomainError.InvalidField("Only expense movements can be linked"))
        }
        if (movement.status != MovementStatus.CONFIRMED) {
            return DomainResult.Err(DomainError.InvalidField("Only confirmed movements can be linked"))
        }

        if (gatheringExpenseRepository.isMovementLinked(movementId)) {
            return DomainResult.Err(DomainError.InvalidField("Movement is already linked to a gathering"))
        }

        val paidBy = when (val validated = GatheringParticipantValidator.validatePaidBy(gathering, paidByParticipant)) {
            is DomainResult.Err -> return validated
            is DomainResult.Ok -> validated.value
        }

        val expense = GatheringExpense(
            id = "gathering-expense-movement-${movement.id}",
            gatheringId = gatheringId,
            amount = movement.amount,
            paidByParticipant = paidBy,
            description = MovementDisplayLabels.displayTitle(
                movement.counterpartyName,
                movement.description,
            ),
            movementId = movement.id,
            recordedAt = movement.recordedAt,
        )

        return when (val validation = expense.validate()) {
            is DomainResult.Err -> validation
            is DomainResult.Ok -> {
                gatheringExpenseRepository.save(expense).getOrElse {
                    return DomainResult.Err(DomainError.InvalidField("Could not link movement to gathering"))
                }
                DomainResult.Ok(expense)
            }
        }
    }
}
