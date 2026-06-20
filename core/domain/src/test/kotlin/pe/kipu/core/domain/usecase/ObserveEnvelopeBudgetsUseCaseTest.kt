package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.Envelope
import pe.kipu.core.domain.model.EnvelopeBudgetStatus
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.repository.EnvelopeRepository
import pe.kipu.core.domain.repository.MovementRepository
import pe.kipu.core.domain.time.FixedTimeProvider
import pe.kipu.core.domain.time.WeekRangeCalculator

class ObserveEnvelopeBudgetsUseCaseTest {

    @Test
    fun `emits recalculated budget states when movements change`() = runTest {
        val reference = Instant.parse("2026-06-17T15:00:00Z")
        val timeProvider = FixedTimeProvider(reference)
        val weekRangeCalculator = WeekRangeCalculator(timeProvider)
        val weekRange = weekRangeCalculator.currentWeekRange()
        val movementInstant = weekRange.start.plusSeconds(3_600)
        val envelope = Envelope(
            id = "envelope-food",
            name = "Comida",
            weeklyLimit = Money.of(BigDecimal("100.00")).getOrError(),
            categoryId = CategoryIds.FOOD,
        )
        val movement = Movement(
            id = "m1",
            type = MovementType.EXPENSE,
            amount = Money.of(BigDecimal("50.00")).getOrError(),
            categoryId = CategoryIds.FOOD,
            channel = PaymentChannel.YAPE,
            source = MovementSource.MANUAL,
            status = MovementStatus.CONFIRMED,
            recordedAt = movementInstant,
            createdAt = movementInstant,
        )
        val useCase = ObserveEnvelopeBudgetsUseCase(
            envelopeRepository = FakeEnvelopeRepository(listOf(envelope)),
            movementRepository = FakeMovementRepository(listOf(movement)),
            calculateEnvelopeBudgetState = CalculateEnvelopeBudgetStateUseCase(
                CalculateCategoryWeeklySpentUseCase(),
            ),
            weekRangeCalculator = weekRangeCalculator,
            timeProvider = timeProvider,
        )

        val states = useCase().first()

        assertEquals(1, states.size)
        assertEquals(50, states.first().percentUsed)
        assertEquals(EnvelopeBudgetStatus.OK, states.first().status)
    }

    private class FakeEnvelopeRepository(
        private val envelopes: List<Envelope>,
    ) : EnvelopeRepository {
        override fun observeEnvelopes(): Flow<List<Envelope>> = flowOf(envelopes)
        override suspend fun getById(id: String): Envelope? = envelopes.find { it.id == id }
        override suspend fun save(envelope: Envelope) = Result.success(Unit)
        override suspend fun delete(id: String) = Result.success(Unit)
    }

    private class FakeMovementRepository(
        private val movements: List<Movement>,
    ) : MovementRepository {
        override fun observeMovements(): Flow<List<Movement>> = flowOf(movements)
        override suspend fun getById(id: String): Movement? = movements.find { it.id == id }

        override suspend fun findByCounterpartyName(counterpartyName: String): List<Movement> =
            movements.filter { it.counterpartyName.equals(counterpartyName, ignoreCase = true) }

        override suspend fun save(movement: Movement) = Result.success(Unit)
        override suspend fun delete(id: String) = Result.success(Unit)
    }
}
