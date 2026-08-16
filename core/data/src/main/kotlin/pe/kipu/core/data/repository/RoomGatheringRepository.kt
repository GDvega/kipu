package pe.kipu.core.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.kipu.core.data.local.dao.GatheringDao
import pe.kipu.core.data.mapper.toDomain
import pe.kipu.core.data.mapper.toEntity
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.Gathering
import pe.kipu.core.domain.repository.GatheringRepository

@Singleton
class RoomGatheringRepository @Inject constructor(
    private val gatheringDao: GatheringDao,
) : GatheringRepository {

    override fun observeGatherings(): Flow<List<Gathering>> =
        gatheringDao.observeAll()
            .map { entities -> entities.map { it.toDomain() } }

    override suspend fun getById(id: EntityId): Gathering? =
        gatheringDao.getById(id)?.toDomain()

    override suspend fun save(gathering: Gathering): Result<Unit> {
        when (val validation = gathering.validate()) {
            is DomainResult.Err -> return Result.failure(IllegalArgumentException(validation.error.message))
            is DomainResult.Ok -> Unit
        }
        return runCatching { gatheringDao.upsert(gathering.toEntity()) }
    }

    override suspend fun delete(id: EntityId): Result<Unit> =
        runCatching { gatheringDao.deleteById(id) }
}
