package pe.kipu.core.data.fake

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.repository.MovementRepository

@Singleton
class FakeMovementRepository @Inject constructor() : MovementRepository {
    override fun observeMovements(): Flow<List<Movement>> = flowOf(emptyList())

    override suspend fun getById(id: EntityId): Movement? = null

    override suspend fun findByCounterpartyName(counterpartyName: String): List<Movement> = emptyList()

    override suspend fun save(movement: Movement): Result<Unit> = Result.success(Unit)

    override suspend fun delete(id: EntityId): Result<Unit> = Result.success(Unit)
}
