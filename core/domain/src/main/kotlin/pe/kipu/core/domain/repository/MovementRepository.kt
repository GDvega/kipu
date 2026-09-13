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

    /** Revalidates both snapshots and preserves financial links in one transaction. */
    suspend fun mergeDuplicates(kept: Movement, removed: Movement): Result<Unit> =
        Result.failure(UnsupportedOperationException("Atomic duplicate merge is not supported"))
}
