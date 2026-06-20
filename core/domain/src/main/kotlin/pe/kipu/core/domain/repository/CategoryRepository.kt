package pe.kipu.core.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.kipu.core.domain.model.Category
import pe.kipu.core.domain.model.EntityId

interface CategoryRepository {
    fun observeCategories(): Flow<List<Category>>

    suspend fun getById(id: EntityId): Category?

    suspend fun save(category: Category): Result<Unit>

    suspend fun delete(id: EntityId): Result<Unit>
}
