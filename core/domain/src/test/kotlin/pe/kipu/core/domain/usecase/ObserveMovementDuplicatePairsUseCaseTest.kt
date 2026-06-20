package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.duplicate.MovementDuplicateMatcher
import pe.kipu.core.domain.duplicate.canonicalKey
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.repository.DuplicateDismissalRepository
import pe.kipu.core.domain.repository.MovementRepository

class ObserveMovementDuplicatePairsUseCaseTest {

    private val now = Instant.parse("2026-06-16T15:00:00Z")
    private val movementRepository = FakeMovementRepository()
    private val dismissalRepository = FakeDuplicateDismissalRepository()
    private val useCase = ObserveMovementDuplicatePairsUseCase(
        movementRepository = movementRepository,
        duplicateDismissalRepository = dismissalRepository,
        findMovementDuplicatePairs = FindMovementDuplicatePairsUseCase(MovementDuplicateMatcher()),
    )

    @Test
    fun `filters dismissed duplicate pairs`() = runTest {
        val movementA = movement("movement-a")
        val movementB = movement("movement-b")
        movementRepository.movements = listOf(movementA, movementB)

        val pairsBeforeDismiss = useCase().first()
        assertEquals(1, pairsBeforeDismiss.size)

        dismissalRepository.dismiss(pairsBeforeDismiss.first().canonicalKey())

        val pairsAfterDismiss = useCase().first()
        assertTrue(pairsAfterDismiss.isEmpty())
    }

    private fun movement(id: String): Movement = Movement(
        id = id,
        type = MovementType.EXPENSE,
        amount = Money.of(BigDecimal("25.00")).getOrError(),
        categoryId = CategoryIds.FOOD,
        channel = PaymentChannel.YAPE,
        source = MovementSource.MANUAL,
        status = MovementStatus.CONFIRMED,
        counterpartyName = "Maria",
        recordedAt = now,
        createdAt = now,
    )

    private class FakeMovementRepository : MovementRepository {
        var movements: List<Movement> = emptyList()

        override fun observeMovements(): Flow<List<Movement>> = flowOf(movements)

        override suspend fun getById(id: String): Movement? = movements.find { it.id == id }

        override suspend fun findByCounterpartyName(counterpartyName: String): List<Movement> =
            movements.filter { it.counterpartyName.equals(counterpartyName, ignoreCase = true) }

        override suspend fun save(movement: Movement): Result<Unit> = Result.success(Unit)

        override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    }

    private class FakeDuplicateDismissalRepository : DuplicateDismissalRepository {
        private val dismissedPairKeys = MutableStateFlow<Set<String>>(emptySet())

        override fun observeDismissedPairKeys(): Flow<Set<String>> = dismissedPairKeys

        override suspend fun dismiss(pairKey: String): Result<Unit> {
            dismissedPairKeys.value = dismissedPairKeys.value + pairKey
            return Result.success(Unit)
        }
    }
}
