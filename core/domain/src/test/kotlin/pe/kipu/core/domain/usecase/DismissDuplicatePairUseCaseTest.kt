package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.duplicate.canonicalMovementDuplicatePairKey
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementDuplicatePair
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.repository.DuplicateDismissalRepository

class DismissDuplicatePairUseCaseTest {

    private val repository = FakeDuplicateDismissalRepository()
    private val useCase = DismissDuplicatePairUseCase(repository)
    private val now = Instant.parse("2026-06-16T15:00:00Z")

    @Test
    fun `persists canonical pair key`() = runTest {
        val result = useCase(
            MovementDuplicatePair(
                movementA = movement("movement-b"),
                movementB = movement("movement-a"),
                matchReasonKey = "duplicate_amount_counterparty_time",
            ),
        )

        assertTrue(result.isSuccess)
        assertEquals(
            setOf(canonicalMovementDuplicatePairKey("movement-a", "movement-b")),
            repository.dismissedKeys.value,
        )
    }

    private fun movement(id: String): Movement = Movement(
        id = id,
        type = MovementType.EXPENSE,
        amount = Money.of(BigDecimal("25.00")).getOrError(),
        categoryId = CategoryIds.FOOD,
        channel = PaymentChannel.YAPE,
        source = MovementSource.MANUAL,
        status = MovementStatus.CONFIRMED,
        recordedAt = now,
        createdAt = now,
    )

    private class FakeDuplicateDismissalRepository : DuplicateDismissalRepository {
        val dismissedKeys = MutableStateFlow<Set<String>>(emptySet())

        override fun observeDismissedPairKeys(): Flow<Set<String>> = dismissedKeys

        override suspend fun dismiss(pairKey: String): Result<Unit> {
            dismissedKeys.value = dismissedKeys.value + pairKey
            return Result.success(Unit)
        }
    }
}
