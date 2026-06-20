package pe.kipu.core.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.Gathering

interface GatheringRepository {
    fun observeGatherings(): Flow<List<Gathering>>

    suspend fun getById(id: EntityId): Gathering?

    suspend fun save(gathering: Gathering): Result<Unit>

    suspend fun delete(id: EntityId): Result<Unit>
}
