package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.Commitment
import pe.kipu.core.domain.model.Envelope
import pe.kipu.core.domain.model.FinancialPlan
import pe.kipu.core.domain.model.FinancialPlanValidationResult
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.plan.FinancialPlanIds
import pe.kipu.core.domain.repository.CommitmentRepository
import pe.kipu.core.domain.repository.EnvelopeRepository
import pe.kipu.core.domain.repository.FinancialPlanRepository

class SaveFinancialPlanUseCaseTest {

    private val validateFinancialPlan = ValidateFinancialPlanUseCase()

    @Test
    fun `saves plan and returns validation result`() = runTest {
        val envelopes = listOf(
            Envelope(
                id = "env-1",
                name = "Comida",
                weeklyLimit = Money.of(BigDecimal("150.00")).getOrError(),
                categoryId = CategoryIds.FOOD,
            ),
        )
        val useCase = SaveFinancialPlanUseCase(
            financialPlanRepository = FakeFinancialPlanRepository(emptyList()),
            envelopeRepository = FakeEnvelopeRepository(envelopes),
            commitmentRepository = FakeCommitmentRepository(emptyList()),
            validateFinancialPlan = validateFinancialPlan,
        )

        val result = useCase(
            planId = FinancialPlanIds.PRIMARY,
            estimatedMonthlyIncome = Money.of(BigDecimal("5000.00")).getOrError(),
            fixedExpenses = Money.of(BigDecimal("1000.00")).getOrError(),
        )

        assertTrue(result.isSuccess)
        assertEquals(FinancialPlanValidationResult.Valid, result.getOrNull()?.validation)
    }

    @Test
    fun `rejects plan when monthly validation is invalid`() = runTest {
        val envelopes = listOf(
            Envelope(
                id = "env-1",
                name = "Comida",
                weeklyLimit = Money.of(BigDecimal("500.00")).getOrError(),
                categoryId = CategoryIds.FOOD,
            ),
        )
        val repository = FakeFinancialPlanRepository(emptyList())
        val useCase = SaveFinancialPlanUseCase(
            financialPlanRepository = repository,
            envelopeRepository = FakeEnvelopeRepository(envelopes),
            commitmentRepository = FakeCommitmentRepository(emptyList()),
            validateFinancialPlan = validateFinancialPlan,
        )

        val result = useCase(
            planId = FinancialPlanIds.PRIMARY,
            estimatedMonthlyIncome = Money.of(BigDecimal("1000.00")).getOrError(),
            fixedExpenses = Money.of(BigDecimal("500.00")).getOrError(),
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is InvalidFinancialPlanException)
    }

    @Test
    fun `returns failure for invalid structural plan`() = runTest {
        val useCase = SaveFinancialPlanUseCase(
            financialPlanRepository = FakeFinancialPlanRepository(emptyList()),
            envelopeRepository = FakeEnvelopeRepository(emptyList()),
            commitmentRepository = FakeCommitmentRepository(emptyList()),
            validateFinancialPlan = validateFinancialPlan,
        )

        val result = useCase(
            planId = "",
            estimatedMonthlyIncome = Money.of(BigDecimal("1000.00")).getOrError(),
            fixedExpenses = Money.of(BigDecimal("100.00")).getOrError(),
        )

        assertTrue(result.isFailure)
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

    private class FakeCommitmentRepository(
        private val commitments: List<Commitment>,
    ) : CommitmentRepository {
        override fun observeCommitments(): Flow<List<Commitment>> = flowOf(commitments)

        override suspend fun getById(id: String): Commitment? = commitments.find { it.id == id }

        override suspend fun save(commitment: Commitment): Result<Unit> = Result.success(Unit)

        override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    }
}
