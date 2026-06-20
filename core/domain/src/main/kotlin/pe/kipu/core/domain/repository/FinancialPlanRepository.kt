package pe.kipu.core.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.FinancialPlan

/**
 * Persists user financial plans.
 *
 * [save] validates structural fields via [FinancialPlan.validate] only.
 * Negativity checks belong to [pe.kipu.core.domain.usecase.ValidateFinancialPlanUseCase]
 * and must be enforced in explicit save flows (CRUD UI) before calling [save].
 */
interface FinancialPlanRepository {
    fun observePlans(): Flow<List<FinancialPlan>>

    suspend fun getById(id: EntityId): FinancialPlan?

    suspend fun save(plan: FinancialPlan): Result<Unit>

    suspend fun delete(id: EntityId): Result<Unit>
}
