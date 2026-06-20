package pe.kipu.core.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.kipu.core.data.local.dao.CommitmentDao
import pe.kipu.core.data.mapper.toDomain
import pe.kipu.core.data.mapper.toEntity
import pe.kipu.core.domain.model.Commitment
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.repository.CommitmentRepository

@Singleton
class RoomCommitmentRepository @Inject constructor(
    private val commitmentDao: CommitmentDao,
) : CommitmentRepository {

    override fun observeCommitments(): Flow<List<Commitment>> =
        commitmentDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getById(id: EntityId): Commitment? =
        commitmentDao.getById(id)?.toDomain()

    override suspend fun save(commitment: Commitment): Result<Unit> {
        when (val validation = commitment.validate()) {
            is DomainResult.Err -> return Result.failure(IllegalArgumentException(validation.error.message))
            is DomainResult.Ok -> Unit
        }
        return runCatching { commitmentDao.upsert(commitment.toEntity()) }
    }

    override suspend fun delete(id: EntityId): Result<Unit> =
        runCatching { commitmentDao.deleteById(id) }
}
