package pe.kipu.core.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.Movement

interface MovementRepository {
    fun observeMovements(): Flow<List<Movement>>

    suspend fun getById(id: EntityId): Movement?

    suspend fun findByCounterpartyName(counterpartyName: String): List<Movement>

    suspend fun save(movement: Movement): Result<Unit>

    suspend fun delete(id: EntityId): Result<Unit>
}
