package pe.kipu.core.domain.usecase

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.model.Category
import pe.kipu.core.domain.repository.CategoryRepository
import pe.kipu.core.domain.time.TimeProvider
import java.time.Instant

class CreateCategoryUseCaseTest {

    private val fixedInstant = Instant.parse("2026-06-20T12:00:00Z")
    private val timeProvider = TimeProvider { fixedInstant }

    @Test
    fun createsCategoryWithGeneratedId() = runTest {
        val repository = FakeCategoryRepository()
        val useCase = CreateCategoryUseCase(repository, timeProvider)

        val result = useCase("Netflix")

        assertTrue(result.isSuccess)
        assertEquals("category-${fixedInstant.toEpochMilli()}", result.getOrNull()?.id)
        assertEquals("Netflix", result.getOrNull()?.name)
        assertEquals(1, repository.saved.size)
    }

    @Test
    fun rejectsBlankName() = runTest {
        val useCase = CreateCategoryUseCase(FakeCategoryRepository(), timeProvider)

        val result = useCase("   ")

        assertTrue(result.isFailure)
    }

    @Test
    fun reusesExistingCategoryByName() = runTest {
        val existing = Category(id = "category-existing", name = "Netflix", iconKey = "other")
        val repository = FakeCategoryRepository(initial = listOf(existing))
        val useCase = CreateCategoryUseCase(repository, timeProvider)

        val result = useCase("netflix")

        assertTrue(result.isSuccess)
        assertEquals(existing, result.getOrNull())
        assertTrue(repository.saved.isEmpty())
    }

    private class FakeCategoryRepository(
        initial: List<Category> = emptyList(),
    ) : CategoryRepository {
        private val categories = initial.toMutableList()
        val saved = mutableListOf<Category>()

        override fun observeCategories() = flowOf(categories.toList())

        override suspend fun getById(id: String): Category? = categories.find { it.id == id }

        override suspend fun save(category: Category): Result<Unit> {
            saved.add(category)
            categories.add(category)
            return Result.success(Unit)
        }

        override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    }
}
