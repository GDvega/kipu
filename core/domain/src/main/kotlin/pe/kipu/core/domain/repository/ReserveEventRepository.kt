package pe.kipu.core.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.kipu.core.domain.model.ReserveEvent

interface ReserveEventRepository {
    fun observeAll(): Flow<List<ReserveEvent>>

    suspend fun getById(id: String): ReserveEvent?

    suspend fun record(event: ReserveEvent): Result<Unit>
}
