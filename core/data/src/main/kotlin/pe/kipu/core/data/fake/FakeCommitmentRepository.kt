package pe.kipu.core.data.fake

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import pe.kipu.core.domain.model.Commitment
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.repository.CommitmentRepository

@Singleton
class FakeCommitmentRepository @Inject constructor() : CommitmentRepository {
    override fun observeCommitments(): Flow<List<Commitment>> = flowOf(emptyList())

    override suspend fun getById(id: EntityId): Commitment? = null

    override suspend fun save(commitment: Commitment): Result<Unit> = Result.success(Unit)

    override suspend fun delete(id: EntityId): Result<Unit> = Result.success(Unit)
}
