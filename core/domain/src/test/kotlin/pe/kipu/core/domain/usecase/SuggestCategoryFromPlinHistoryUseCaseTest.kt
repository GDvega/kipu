package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.repository.MovementRepository

class SuggestCategoryFromPlinHistoryUseCaseTest {

    private val now = Instant.parse("2026-06-16T12:00:00Z")

    @Test
    fun `suggests transport from plin history match`() = runTest {
        val repository = FakeMovementRepository(
            listOf(
                sampleMovement(
                    id = "m1",
                    counterparty = "LUIS LOPEZ",
                    categoryId = CategoryIds.TRANSPORT,
                ),
                sampleMovement(
                    id = "m2",
                    counterparty = "LUIS LOPEZ",
                    categoryId = CategoryIds.TRANSPORT,
                ),
            ),
        )
        val useCase = SuggestCategoryFromPlinHistoryUseCase(repository)

        val result = useCase("LUIS LOPEZ")

        assertEquals(CategoryIds.TRANSPORT, result?.categoryId)
        assertEquals(SuggestCategoryFromPlinHistoryUseCase.REASON_KEY, result?.reason)
    }

    @Test
    fun `no history returns no suggestion`() = runTest {
        val useCase = SuggestCategoryFromPlinHistoryUseCase(FakeMovementRepository(emptyList()))

        assertNull(useCase("DESCONOCIDO"))
        assertNull(useCase(null))
    }

    private fun sampleMovement(
        id: String,
        counterparty: String,
        categoryId: String,
    ) = Movement(
        id = id,
        type = MovementType.EXPENSE,
        amount = Money.of(BigDecimal("10.00")).getOrError(),
        categoryId = categoryId,
        channel = PaymentChannel.PLIN,
        source = MovementSource.MANUAL,
        status = MovementStatus.CONFIRMED,
        counterpartyName = counterparty,
        recordedAt = now,
        createdAt = now,
    )

    private class FakeMovementRepository(
        private val movements: List<Movement>,
    ) : MovementRepository {
        override fun observeMovements(): Flow<List<Movement>> = flowOf(movements)

        override suspend fun getById(id: String): Movement? = movements.find { it.id == id }

        override suspend fun findByCounterpartyName(counterpartyName: String): List<Movement> =
            movements.filter { it.counterpartyName.equals(counterpartyName, ignoreCase = true) }

        override suspend fun save(movement: Movement): Result<Unit> = Result.success(Unit)

        override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    }
}
