package pe.kipu.core.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.kipu.core.data.local.dao.MovementDao
import pe.kipu.core.data.mapper.toDomain
import pe.kipu.core.data.mapper.toEntity
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.repository.MovementRepository

@Singleton
class RoomMovementRepository @Inject constructor(
    private val movementDao: MovementDao,
) : MovementRepository {

    override fun observeMovements(): Flow<List<Movement>> =
        movementDao.observeAll()
            .map { entities -> entities.map { it.toDomain() } }

    override suspend fun getById(id: EntityId): Movement? =
        movementDao.getById(id)?.toDomain()

    override suspend fun findByCounterpartyName(counterpartyName: String): List<Movement> =
        movementDao.findByCounterpartyName(counterpartyName).map { it.toDomain() }

    override suspend fun save(movement: Movement): Result<Unit> {
        when (val validation = movement.validate()) {
            is DomainResult.Err -> return Result.failure(IllegalArgumentException(validation.error.message))
            is DomainResult.Ok -> Unit
        }
        return runCatching { movementDao.upsert(movement.toEntity()) }
    }

    override suspend fun delete(id: EntityId): Result<Unit> =
        runCatching { movementDao.deleteById(id) }
}
