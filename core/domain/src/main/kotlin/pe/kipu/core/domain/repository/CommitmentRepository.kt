package pe.kipu.core.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.kipu.core.domain.model.Commitment
import pe.kipu.core.domain.model.EntityId

interface CommitmentRepository {
    fun observeCommitments(): Flow<List<Commitment>>

    suspend fun getById(id: EntityId): Commitment?

    suspend fun save(commitment: Commitment): Result<Unit>

    suspend fun delete(id: EntityId): Result<Unit>
}
