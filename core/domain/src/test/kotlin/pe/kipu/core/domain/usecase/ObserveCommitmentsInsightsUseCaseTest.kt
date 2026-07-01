package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.Commitment
import pe.kipu.core.domain.model.CommitmentType
import pe.kipu.core.domain.model.Envelope
import pe.kipu.core.domain.model.FinancialPlan
import pe.kipu.core.domain.model.FinancialPlanValidationResult
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.repository.CommitmentRepository
import pe.kipu.core.domain.repository.EnvelopeRepository
import pe.kipu.core.domain.repository.FinancialPlanRepository
import pe.kipu.core.domain.repository.MovementRepository

class ObserveCommitmentsInsightsUseCaseTest {

    @Test
    fun `combines summaries and plan validation`() = runTest {
        val commitments = listOf(
            Commitment(
                id = "goal-1",
                type = CommitmentType.SAVINGS_GOAL,
                title = "Fondo emergencia",
                targetAmount = Money.of(BigDecimal("500.00")).getOrError(),
                currentAmount = Money.of(BigDecimal("120.00")).getOrError(),
            ),
            Commitment(
                id = "debt-1",
                type = CommitmentType.SOCIAL_DEBT,
                title = "Deuda con Juan",
                currentAmount = Money.of(BigDecimal("80.00")).getOrError(),
                counterpartyName = "Juan",
            ),
        )
        val plan = FinancialPlan(
            id = "plan-1",
            estimatedMonthlyIncome = Money.of(BigDecimal("3000.00")).getOrError(),
            fixedExpenses = Money.of(BigDecimal("1800.00")).getOrError(),
        )
        val envelopes = listOf(
            envelope("150.00"),
            envelope("80.00"),
            envelope("60.00"),
        )

        val useCase = ObserveCommitmentsInsightsUseCase(
            observeCommitmentSummaries = ObserveCommitmentSummariesUseCase(
                commitmentRepository = FakeCommitmentRepository(commitments),
                movementRepository = FakeMovementRepository(),
                calculateSavingsGoalProgress = CalculateSavingsGoalProgressUseCase(),
                calculateCashFlowSummary = CalculateCashFlowSummaryUseCase(),
            ),
            financialPlanRepository = FakeFinancialPlanRepository(listOf(plan)),
            envelopeRepository = FakeEnvelopeRepository(envelopes),
            movementRepository = FakeMovementRepository(),
            validateFinancialPlan = ValidateFinancialPlanUseCase(),
        )

        val insights = useCase().first()

        assertEquals(2, insights.summaries.size)
        assertEquals(24, insights.summaries.first().savingsProgress?.progressPercent)
        assertTrue(insights.planValidation is FinancialPlanValidationResult.Invalid)
    }

    private fun envelope(weeklyLimit: String): Envelope = Envelope(
        id = "envelope-${weeklyLimit}",
        name = "Sobre",
        weeklyLimit = Money.of(BigDecimal(weeklyLimit)).getOrError(),
        categoryId = CategoryIds.FOOD,
    )

    private class FakeCommitmentRepository(
        private val commitments: List<Commitment>,
    ) : pe.kipu.core.domain.repository.CommitmentRepository {
        override fun observeCommitments(): Flow<List<Commitment>> = flowOf(commitments)

        override suspend fun getById(id: String): Commitment? = commitments.find { it.id == id }

        override suspend fun save(commitment: Commitment): Result<Unit> = Result.success(Unit)

        override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    }

    private class FakeFinancialPlanRepository(
        private val plans: List<FinancialPlan>,
    ) : FinancialPlanRepository {
        override fun observePlans(): Flow<List<FinancialPlan>> = flowOf(plans)

        override suspend fun getById(id: String): FinancialPlan? = plans.find { it.id == id }

        override suspend fun save(plan: FinancialPlan): Result<Unit> = Result.success(Unit)

        override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    }

    private class FakeEnvelopeRepository(
        private val envelopes: List<Envelope>,
    ) : EnvelopeRepository {
        override fun observeEnvelopes(): Flow<List<Envelope>> = flowOf(envelopes)

        override suspend fun getById(id: String): Envelope? = envelopes.find { it.id == id }

        override suspend fun save(envelope: Envelope): Result<Unit> = Result.success(Unit)

        override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    }

    private class FakeMovementRepository : MovementRepository {
        override fun observeMovements(): Flow<List<Movement>> = flowOf(emptyList())

        override suspend fun getById(id: String): Movement? = null

        override suspend fun findByCounterpartyName(counterpartyName: String): List<Movement> = emptyList()

        override suspend fun save(movement: Movement): Result<Unit> = Result.success(Unit)

        override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    }
}
