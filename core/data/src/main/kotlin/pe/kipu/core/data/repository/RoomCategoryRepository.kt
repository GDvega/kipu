package pe.kipu.core.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.kipu.core.data.flow.withImmediateDefault
import pe.kipu.core.data.local.dao.CategoryDao
import pe.kipu.core.data.mapper.toDomain
import pe.kipu.core.data.mapper.toEntity
import pe.kipu.core.domain.model.Category
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.repository.CategoryRepository

@Singleton
class RoomCategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao,
) : CategoryRepository {

    override fun observeCategories(): Flow<List<Category>> =
        categoryDao.observeAll()
            .map { entities -> entities.map { it.toDomain() } }
            .withImmediateDefault(emptyList())

    override suspend fun getById(id: EntityId): Category? =
        categoryDao.getById(id)?.toDomain()

    override suspend fun save(category: Category): Result<Unit> {
        when (val validation = category.validate()) {
            is DomainResult.Err -> return Result.failure(IllegalArgumentException(validation.error.message))
            is DomainResult.Ok -> Unit
        }
        return runCatching { categoryDao.upsert(category.toEntity()) }
    }

    override suspend fun delete(id: EntityId): Result<Unit> =
        runCatching { categoryDao.deleteById(id) }
}
