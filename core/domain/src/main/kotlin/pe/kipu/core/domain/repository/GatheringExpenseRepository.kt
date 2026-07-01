package pe.kipu.core.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.GatheringExpense
import pe.kipu.core.domain.model.Money

interface GatheringExpenseRepository {
    fun observeTotalsByGathering(): Flow<Map<EntityId, Money>>

    fun observeExpensesByGathering(): Flow<Map<EntityId, List<GatheringExpense>>>

    fun observeLinkedMovementIds(): Flow<Set<EntityId>>

    /** Movement IDs linked to active (non-settled) gatherings — excluded from budget calculations. */
    fun observeActiveGatheringLinkedMovementIds(): Flow<Set<EntityId>>

    suspend fun isMovementLinked(movementId: EntityId): Boolean

    suspend fun save(expense: GatheringExpense): Result<Unit>
}
