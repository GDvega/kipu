package pe.kipu.core.domain.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.kipu.core.domain.model.Commitment
import pe.kipu.core.domain.model.CommitmentType
import pe.kipu.core.domain.repository.CommitmentRepository

class ObserveSavingsGoalCommitmentsUseCase @Inject constructor(
    private val commitmentRepository: CommitmentRepository,
) {
    operator fun invoke(): Flow<List<Commitment>> =
        commitmentRepository.observeCommitments().map { commitments ->
            commitments.filter { it.type == CommitmentType.SAVINGS_GOAL }
        }
}
