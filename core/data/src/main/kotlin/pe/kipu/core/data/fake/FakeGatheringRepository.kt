package pe.kipu.core.data.fake

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.Gathering
import pe.kipu.core.domain.repository.GatheringRepository

@Singleton
class FakeGatheringRepository @Inject constructor() : GatheringRepository {
    override fun observeGatherings(): Flow<List<Gathering>> = flowOf(emptyList())

    override suspend fun getById(id: EntityId): Gathering? = null

    override suspend fun save(gathering: Gathering): Result<Unit> = Result.success(Unit)

    override suspend fun delete(id: EntityId): Result<Unit> = Result.success(Unit)
}
