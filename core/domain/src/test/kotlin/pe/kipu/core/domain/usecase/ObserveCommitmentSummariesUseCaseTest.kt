package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import pe.kipu.core.domain.model.Commitment
import pe.kipu.core.domain.model.CommitmentStatusKeys
import pe.kipu.core.domain.model.CommitmentType
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.repository.CommitmentRepository
import pe.kipu.core.domain.repository.MovementRepository
import java.time.Instant

class ObserveCommitmentSummariesUseCaseTest {

    private val calculateSavingsGoalProgress = CalculateSavingsGoalProgressUseCase()

    @Test
    fun `maps savings goal progress and status key`() = runTest {
        val commitments = listOf(
            Commitment(
                id = "goal-1",
                type = CommitmentType.SAVINGS_GOAL,
                title = "Fondo emergencia",
                targetAmount = Money.of(BigDecimal("500.00")).getOrError(),
                currentAmount = Money.of(BigDecimal("120.00")).getOrError(),
            ),
        )
        val useCase = ObserveCommitmentSummariesUseCase(
            commitmentRepository = FakeCommitmentRepository(commitments),
            movementRepository = FakeMovementRepository(emptyList()),
            calculateSavingsGoalProgress = calculateSavingsGoalProgress,
            calculateCashFlowSummary = CalculateCashFlowSummaryUseCase(),
        )

        val summaries = useCase().first()

        assertEquals(1, summaries.size)
        assertEquals(24, summaries.first().savingsProgress?.progressPercent)
        assertEquals(CommitmentStatusKeys.SAVINGS_IN_PROGRESS, summaries.first().statusKey)
    }

    @Test
    fun `includes linked income movements in savings progress`() = runTest {
        val commitments = listOf(
            Commitment(
                id = "goal-1",
                type = CommitmentType.SAVINGS_GOAL,
                title = "Fondo emergencia",
                targetAmount = Money.of(BigDecimal("500.00")).getOrError(),
                currentAmount = Money.of(BigDecimal("100.00")).getOrError(),
            ),
        )
        val movements = listOf(
            Movement(
                id = "mov-1",
                type = MovementType.INCOME,
                amount = Money.of(BigDecimal("25.00")).getOrError(),
                categoryId = "category-other",
                channel = PaymentChannel.YAPE,
                source = MovementSource.MANUAL,
                status = MovementStatus.CONFIRMED,
                commitmentId = "goal-1",
                recordedAt = Instant.parse("2026-06-16T12:00:00Z"),
                createdAt = Instant.parse("2026-06-16T12:00:00Z"),
            ),
        )
        val useCase = ObserveCommitmentSummariesUseCase(
            commitmentRepository = FakeCommitmentRepository(commitments),
            movementRepository = FakeMovementRepository(movements),
            calculateSavingsGoalProgress = calculateSavingsGoalProgress,
            calculateCashFlowSummary = CalculateCashFlowSummaryUseCase(),
        )

        val summaries = useCase().first()

        assertEquals(25, summaries.first().savingsProgress?.progressPercent)
    }

    @Test
    fun `maps unsettled social debt as pending`() = runTest {
        val commitments = listOf(
            Commitment(
                id = "debt-1",
                type = CommitmentType.SOCIAL_DEBT,
                title = "Deuda con Juan",
                currentAmount = Money.of(BigDecimal("80.00")).getOrError(),
                counterpartyName = "Juan",
            ),
        )
        val useCase = ObserveCommitmentSummariesUseCase(
            commitmentRepository = FakeCommitmentRepository(commitments),
            movementRepository = FakeMovementRepository(emptyList()),
            calculateSavingsGoalProgress = calculateSavingsGoalProgress,
            calculateCashFlowSummary = CalculateCashFlowSummaryUseCase(),
        )

        val summaries = useCase().first()

        assertEquals(CommitmentStatusKeys.SOCIAL_DEBT_PENDING, summaries.first().statusKey)
    }

    private class FakeCommitmentRepository(
        private val commitments: List<Commitment>,
    ) : CommitmentRepository {
        override fun observeCommitments(): Flow<List<Commitment>> = flowOf(commitments)

        override suspend fun getById(id: String): Commitment? = commitments.find { it.id == id }

        override suspend fun save(commitment: Commitment): Result<Unit> = Result.success(Unit)

        override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    }

    private class FakeMovementRepository(
        private val movements: List<Movement>,
    ) : MovementRepository {
        override fun observeMovements(): Flow<List<Movement>> = flowOf(movements)

        override suspend fun getById(id: String): Movement? = movements.find { it.id == id }

        override suspend fun findByCounterpartyName(counterpartyName: String): List<Movement> = emptyList()

        override suspend fun save(movement: Movement): Result<Unit> = Result.success(Unit)

        override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    }
}
