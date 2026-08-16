package pe.kipu.core.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.kipu.core.data.local.dao.GatheringExpenseDao
import pe.kipu.core.data.mapper.toDomain
import pe.kipu.core.data.mapper.toEntity
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.GatheringExpense
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.repository.GatheringExpenseRepository

@Singleton
class RoomGatheringExpenseRepository @Inject constructor(
    private val gatheringExpenseDao: GatheringExpenseDao,
) : GatheringExpenseRepository {

    override fun observeTotalsByGathering(): Flow<Map<EntityId, Money>> =
        gatheringExpenseDao.observeTotalsByGathering()
            .map { rows ->
                rows.associate { row ->
                    row.gatheringId to Money.of(BigDecimal.valueOf(row.totalCents, 2)).getOrError()
                }
            }

    override fun observeExpensesByGathering(): Flow<Map<EntityId, List<GatheringExpense>>> =
        gatheringExpenseDao.observeAll()
            .map { entities ->
                entities
                    .map { it.toDomain() }
                    .groupBy { expense -> expense.gatheringId }
            }

    override fun observeLinkedMovementIds(): Flow<Set<EntityId>> =
        gatheringExpenseDao.observeLinkedMovementIds()
            .map { ids -> ids.toSet() }

    override fun observeActiveGatheringLinkedMovementIds(): Flow<Set<EntityId>> =
        gatheringExpenseDao.observeActiveGatheringLinkedMovementIds()
            .map { ids -> ids.toSet() }

    override suspend fun isMovementLinked(movementId: EntityId): Boolean =
        gatheringExpenseDao.isMovementLinked(movementId)

    override suspend fun save(expense: GatheringExpense): Result<Unit> {
        when (val validation = expense.validate()) {
            is DomainResult.Err -> return Result.failure(IllegalArgumentException(validation.error.message))
            is DomainResult.Ok -> Unit
        }
        return runCatching { gatheringExpenseDao.upsert(expense.toEntity()) }
    }
}
