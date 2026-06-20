package pe.kipu.core.data.fake

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.Envelope
import pe.kipu.core.domain.repository.EnvelopeRepository

@Singleton
class FakeEnvelopeRepository @Inject constructor() : EnvelopeRepository {
    override fun observeEnvelopes(): Flow<List<Envelope>> = flowOf(emptyList())

    override suspend fun getById(id: EntityId): Envelope? = null

    override suspend fun save(envelope: Envelope): Result<Unit> = Result.success(Unit)

    override suspend fun delete(id: EntityId): Result<Unit> = Result.success(Unit)
}
