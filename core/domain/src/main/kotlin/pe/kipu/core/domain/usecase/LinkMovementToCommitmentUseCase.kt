package pe.kipu.core.domain.usecase

import javax.inject.Inject
import pe.kipu.core.domain.model.CommitmentType
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.repository.CommitmentRepository
import pe.kipu.core.domain.repository.MovementRepository

class LinkMovementToCommitmentUseCase @Inject constructor(
    private val movementRepository: MovementRepository,
    private val commitmentRepository: CommitmentRepository,
) {
    suspend operator fun invoke(
        movementId: EntityId,
        commitmentId: EntityId?,
    ): Result<Unit> {
        val movement = movementRepository.getById(movementId)
            ?: return Result.failure(IllegalArgumentException("Movement not found"))

        if (movement.status != MovementStatus.CONFIRMED) {
            return Result.failure(IllegalArgumentException("Only confirmed movements can be linked to a goal"))
        }

        if (movement.type != MovementType.INCOME) {
            return Result.failure(IllegalArgumentException("Only income movements can be linked to a savings goal"))
        }

        if (commitmentId != null) {
            val commitment = commitmentRepository.getById(commitmentId)
                ?: return Result.failure(IllegalArgumentException("Commitment not found"))
            if (commitment.type != CommitmentType.SAVINGS_GOAL) {
                return Result.failure(IllegalArgumentException("Only savings goals can receive linked movements"))
            }
        }

        val updated = movement.copy(commitmentId = commitmentId)
        return when (val validation = updated.validate()) {
            is DomainResult.Err -> Result.failure(IllegalArgumentException(validation.error.message))
            is DomainResult.Ok -> movementRepository.save(updated)
        }
    }
}
