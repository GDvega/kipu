package pe.kipu.core.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.Envelope

interface EnvelopeRepository {
    fun observeEnvelopes(): Flow<List<Envelope>>

    suspend fun getById(id: EntityId): Envelope?

    suspend fun save(envelope: Envelope): Result<Unit>

    suspend fun delete(id: EntityId): Result<Unit>
}
