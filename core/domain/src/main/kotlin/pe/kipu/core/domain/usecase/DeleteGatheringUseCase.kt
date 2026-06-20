package pe.kipu.core.domain.usecase

import javax.inject.Inject
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.repository.GatheringRepository

class DeleteGatheringUseCase @Inject constructor(
    private val gatheringRepository: GatheringRepository,
) {
    suspend operator fun invoke(id: EntityId): Result<Unit> = gatheringRepository.delete(id)
}
