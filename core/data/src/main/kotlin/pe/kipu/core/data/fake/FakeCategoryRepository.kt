package pe.kipu.core.data.fake

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import pe.kipu.core.domain.model.Category
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.repository.CategoryRepository

@Singleton
class FakeCategoryRepository @Inject constructor() : CategoryRepository {
    override fun observeCategories(): Flow<List<Category>> = flowOf(emptyList())

    override suspend fun getById(id: EntityId): Category? = null

    override suspend fun save(category: Category): Result<Unit> = Result.success(Unit)

    override suspend fun delete(id: EntityId): Result<Unit> = Result.success(Unit)
}
