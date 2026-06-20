package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.Category
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.repository.CategoryRepository
import pe.kipu.core.domain.repository.MovementRepository

class UpdateMovementCategoryUseCaseTest {

    private val now = Instant.parse("2026-06-16T12:00:00Z")

    @Test
    fun `updates category when movement and category exist`() = runTest {
        val movement = sampleMovement(categoryId = CategoryIds.FOOD)
        val movementRepository = FakeMovementRepository(listOf(movement))
        val categoryRepository = FakeCategoryRepository(
            listOf(Category(id = CategoryIds.FOOD, name = "Comida"), Category(id = CategoryIds.TRANSPORT, name = "Transporte")),
        )
        val useCase = UpdateMovementCategoryUseCase(movementRepository, categoryRepository)

        val result = useCase(movement.id, CategoryIds.TRANSPORT)

        assertTrue(result.isSuccess)
        assertEquals(CategoryIds.TRANSPORT, movementRepository.saved?.categoryId)
    }

    @Test
    fun `fails when category does not exist`() = runTest {
        val movement = sampleMovement(categoryId = CategoryIds.FOOD)
        val useCase = UpdateMovementCategoryUseCase(
            FakeMovementRepository(listOf(movement)),
            FakeCategoryRepository(emptyList()),
        )

        val result = useCase(movement.id, CategoryIds.TRANSPORT)

        assertTrue(result.isFailure)
    }

    private fun sampleMovement(categoryId: String): Movement = Movement(
        id = "movement-1",
        type = MovementType.EXPENSE,
        amount = Money.of(BigDecimal("10.00")).getOrError(),
        categoryId = categoryId,
        channel = PaymentChannel.YAPE,
        source = MovementSource.RECEIPT,
        status = MovementStatus.CONFIRMED,
        recordedAt = now,
        createdAt = now,
    )

    private class FakeMovementRepository(
        private val movements: List<Movement>,
    ) : MovementRepository {
        var saved: Movement? = null

        override fun observeMovements(): Flow<List<Movement>> = flowOf(movements)

        override suspend fun getById(id: String): Movement? = movements.find { it.id == id }

        override suspend fun findByCounterpartyName(counterpartyName: String): List<Movement> = emptyList()

        override suspend fun save(movement: Movement): Result<Unit> {
            saved = movement
            return Result.success(Unit)
        }

        override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    }

    private class FakeCategoryRepository(
        private val categories: List<Category>,
    ) : CategoryRepository {
        override fun observeCategories(): Flow<List<Category>> = flowOf(categories)

        override suspend fun getById(id: String): Category? = categories.find { it.id == id }

        override suspend fun save(category: Category): Result<Unit> = Result.success(Unit)

        override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    }
}
