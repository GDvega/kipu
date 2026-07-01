package pe.kipu.core.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.kipu.core.data.flow.withImmediateDefault
import pe.kipu.core.data.local.dao.FinancialPlanDao
import pe.kipu.core.data.mapper.toDomain
import pe.kipu.core.data.mapper.toEntity
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.FinancialPlan
import pe.kipu.core.domain.repository.FinancialPlanRepository

@Singleton
class RoomFinancialPlanRepository @Inject constructor(
    private val financialPlanDao: FinancialPlanDao,
) : FinancialPlanRepository {

    override fun observePlans(): Flow<List<FinancialPlan>> =
        financialPlanDao.observeAll()
            .map { entities -> entities.map { it.toDomain() } }
            .withImmediateDefault(emptyList())

    override suspend fun getById(id: EntityId): FinancialPlan? =
        financialPlanDao.getById(id)?.toDomain()

    override suspend fun save(plan: FinancialPlan): Result<Unit> {
        when (val validation = plan.validate()) {
            is DomainResult.Err -> return Result.failure(IllegalArgumentException(validation.error.message))
            is DomainResult.Ok -> Unit
        }
        return runCatching { financialPlanDao.upsert(plan.toEntity()) }
    }

    override suspend fun delete(id: EntityId): Result<Unit> =
        runCatching { financialPlanDao.deleteById(id) }
}
