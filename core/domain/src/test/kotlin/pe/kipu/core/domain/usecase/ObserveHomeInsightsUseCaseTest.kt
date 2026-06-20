package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.AlertSeverity
import pe.kipu.core.domain.model.Envelope
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.repository.EnvelopeRepository
import pe.kipu.core.domain.model.UserPreferences
import pe.kipu.core.domain.repository.MovementRepository
import pe.kipu.core.domain.repository.UserPreferencesRepository
import pe.kipu.core.domain.time.FixedTimeProvider
import pe.kipu.core.domain.time.WeekRangeCalculator

class ObserveHomeInsightsUseCaseTest {

    private val peruZone: ZoneId = WeekRangeCalculator.PERU_ZONE
    private val wednesday = ZonedDateTime.of(2026, 6, 17, 10, 0, 0, 0, peruZone).toInstant()

    @Test
    fun `combines daily available and ant spending alerts`() = runTest {
        val envelope = Envelope(
            id = "envelope-food",
            name = "Comida",
            weeklyLimit = Money.of(BigDecimal("100.00")).getOrError(),
            categoryId = CategoryIds.FOOD,
        )
        val movements = listOf(
            movement("m1", "5.00", hoursAgo = 1),
            movement("m2", "6.00", hoursAgo = 2),
            movement("m3", "7.00", hoursAgo = 3),
            movement("m4", "50.00", hoursAgo = 4),
        )
        val useCase = createUseCase(
            envelopes = listOf(envelope),
            movements = movements,
            reference = wednesday,
        )

        val insights = useCase().first()

        assertEquals(1, insights.envelopeCount)
        assertEquals(4, insights.movementCount)
        assertEquals(Money.of(BigDecimal("6.40")).getOrError(), insights.dailyAvailable.dailyAvailable)
        assertEquals(1, insights.antSpendingAlerts.size)
        assertEquals(AlertSeverity.AMBER, insights.antSpendingAlerts.first().severity)
    }

    @Test
    fun `returns empty ant alerts when only isolated small expenses exist`() = runTest {
        val envelope = Envelope(
            id = "envelope-food",
            name = "Comida",
            weeklyLimit = Money.of(BigDecimal("100.00")).getOrError(),
            categoryId = CategoryIds.FOOD,
        )
        val useCase = createUseCase(
            envelopes = listOf(envelope),
            movements = listOf(movement("m1", "5.00", hoursAgo = 1)),
            reference = wednesday,
        )

        val insights = useCase().first()

        assertTrue(insights.antSpendingAlerts.isEmpty())
        assertEquals(Money.of(BigDecimal("19.00")).getOrError(), insights.dailyAvailable.dailyAvailable)
    }

    private fun createUseCase(
        envelopes: List<Envelope>,
        movements: List<Movement>,
        reference: Instant,
    ): ObserveHomeInsightsUseCase {
        val timeProvider = FixedTimeProvider(reference)
        val weekRangeCalculator = WeekRangeCalculator(timeProvider)
        val observeEnvelopeBudgets = ObserveEnvelopeBudgetsUseCase(
            envelopeRepository = FakeEnvelopeRepository(envelopes),
            movementRepository = FakeMovementRepository(movements),
            calculateEnvelopeBudgetState = CalculateEnvelopeBudgetStateUseCase(
                CalculateCategoryWeeklySpentUseCase(),
            ),
            weekRangeCalculator = weekRangeCalculator,
            timeProvider = timeProvider,
        )
        return ObserveHomeInsightsUseCase(
            observeEnvelopeBudgets = observeEnvelopeBudgets,
            movementRepository = FakeMovementRepository(movements),
            userPreferencesRepository = FakeUserPreferencesRepository(),
            calculateDailyAvailable = CalculateDailyAvailableUseCase(
                CalculateWeeklyEnvelopeTotalsUseCase(),
            ),
            detectAntSpending = DetectAntSpendingUseCase(),
            detectAntSpendingWeeklyLimitUseCase = DetectAntSpendingWeeklyLimitUseCase(),
            weekRangeCalculator = weekRangeCalculator,
            timeProvider = timeProvider,
        )
    }

    private fun movement(
        id: String,
        amount: String,
        hoursAgo: Long,
    ): Movement {
        val recordedAt = wednesday.minusSeconds(hoursAgo * 3_600)
        return Movement(
            id = id,
            type = MovementType.EXPENSE,
            amount = Money.of(BigDecimal(amount)).getOrError(),
            categoryId = CategoryIds.FOOD,
            channel = PaymentChannel.YAPE,
            source = MovementSource.MANUAL,
            status = MovementStatus.CONFIRMED,
            recordedAt = recordedAt,
            createdAt = recordedAt,
        )
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

    private class FakeUserPreferencesRepository : UserPreferencesRepository {
        override fun observePreferences(): Flow<UserPreferences> = flowOf(UserPreferences())
        override suspend fun updatePreferences(transform: (UserPreferences) -> UserPreferences) = Result.success(Unit)
        override suspend fun clear() = Result.success(Unit)
    }
}
