package pe.kipu.core.data.fake

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.FinancialPlan
import pe.kipu.core.domain.repository.FinancialPlanRepository

@Singleton
class FakeFinancialPlanRepository @Inject constructor() : FinancialPlanRepository {
    override fun observePlans(): Flow<List<FinancialPlan>> = flowOf(emptyList())

    override suspend fun getById(id: EntityId): FinancialPlan? = null

    override suspend fun save(plan: FinancialPlan): Result<Unit> = Result.success(Unit)

    override suspend fun delete(id: EntityId): Result<Unit> = Result.success(Unit)
}
