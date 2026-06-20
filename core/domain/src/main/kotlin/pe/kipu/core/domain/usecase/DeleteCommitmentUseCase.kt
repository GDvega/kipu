package pe.kipu.core.domain.usecase

import javax.inject.Inject
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.repository.CommitmentRepository

class DeleteCommitmentUseCase @Inject constructor(
    private val commitmentRepository: CommitmentRepository,
) {
    suspend operator fun invoke(commitmentId: EntityId): Result<Unit> {
        if (commitmentRepository.getById(commitmentId) == null) {
            return Result.failure(IllegalArgumentException("Commitment not found"))
        }
        return commitmentRepository.delete(commitmentId)
    }
}
