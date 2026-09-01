package pe.kipu.core.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import pe.kipu.core.domain.model.Category
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.MovementAuditAction
import pe.kipu.core.domain.model.MovementAuditEntry
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.repository.CategoryRepository
import pe.kipu.core.domain.repository.MovementAuditRepository
import java.math.BigDecimal
import java.time.Instant

class ObserveMovementAuditLogsUseCaseTest {

    private val auditRepo = FakeMovementAuditRepo()
    private val categoryRepo = FakeCategoryRepo()
    private val useCase = ObserveMovementAuditLogsUseCase(auditRepo, categoryRepo)

    @Test
    fun `enriches audit entries with category names`() = runBlocking {
        categoryRepo.setCategories(
            listOf(
                Category(id = "cat-food", name = "Alimentos"),
                Category(id = "cat-services", name = "Servicios"),
            ),
        )

        auditRepo.recordAudit(
            MovementAuditEntry(
                id = "audit-1",
                movementId = "mov-1",
                action = MovementAuditAction.CREATED,
                movementType = MovementType.EXPENSE,
                amount = Money.of(BigDecimal("25.00")).getOrError(),
                categoryId = "cat-food",
                channel = PaymentChannel.YAPE,
                description = "Almuerzo",
                timestamp = Instant.parse("2026-08-22T12:00:00Z"),
            ),
        )

        val logs = useCase().first()

        assertEquals(1, logs.size)
        assertEquals("Alimentos", logs[0].categoryName)
        assertEquals(MovementAuditAction.CREATED, logs[0].action)
    }

    private class FakeMovementAuditRepo : MovementAuditRepository {
        private val logsFlow = MutableStateFlow<List<MovementAuditEntry>>(emptyList())

        override fun observeAuditLogs(): Flow<List<MovementAuditEntry>> = logsFlow

        override suspend fun recordAudit(entry: MovementAuditEntry): Result<Unit> {
            logsFlow.value = listOf(entry) + logsFlow.value
            return Result.success(Unit)
        }

        override suspend fun getAll(): List<MovementAuditEntry> = logsFlow.value
    }

    private class FakeCategoryRepo : CategoryRepository {
        private val categoriesFlow = MutableStateFlow<List<Category>>(emptyList())

        fun setCategories(categories: List<Category>) {
            categoriesFlow.value = categories
        }

        override fun observeCategories(): Flow<List<Category>> = categoriesFlow
        override suspend fun getById(id: EntityId): Category? = categoriesFlow.value.find { it.id == id }
        override suspend fun save(category: Category): Result<Unit> = Result.success(Unit)
        override suspend fun delete(id: EntityId): Result<Unit> = Result.success(Unit)
    }
}
