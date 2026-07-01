package pe.kipu.core.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.kipu.core.data.flow.withImmediateDefault
import pe.kipu.core.data.local.dao.EnvelopeDao
import pe.kipu.core.data.mapper.toDomain
import pe.kipu.core.data.mapper.toEntity
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.Envelope
import pe.kipu.core.domain.repository.EnvelopeRepository

@Singleton
class RoomEnvelopeRepository @Inject constructor(
    private val envelopeDao: EnvelopeDao,
) : EnvelopeRepository {

    override fun observeEnvelopes(): Flow<List<Envelope>> =
        envelopeDao.observeAll()
            .map { entities -> entities.map { it.toDomain() } }
            .withImmediateDefault(emptyList())

    override suspend fun getById(id: EntityId): Envelope? =
        envelopeDao.getById(id)?.toDomain()

    override suspend fun save(envelope: Envelope): Result<Unit> {
        when (val validation = envelope.validate()) {
            is DomainResult.Err -> return Result.failure(IllegalArgumentException(validation.error.message))
            is DomainResult.Ok -> Unit
        }
        return runCatching { envelopeDao.upsert(envelope.toEntity()) }
    }

    override suspend fun delete(id: EntityId): Result<Unit> =
        runCatching { envelopeDao.deleteById(id) }
}
