package pe.kipu.core.domain.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import pe.kipu.core.domain.model.Gathering
import pe.kipu.core.domain.repository.GatheringRepository

class ObserveGatheringsUseCase @Inject constructor(
    private val gatheringRepository: GatheringRepository,
) {
    operator fun invoke(): Flow<List<Gathering>> = gatheringRepository.observeGatherings()
}
