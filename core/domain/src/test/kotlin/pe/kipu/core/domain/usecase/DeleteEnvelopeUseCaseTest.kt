package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.model.Envelope
import pe.kipu.core.domain.model.FinancialPlan
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.plan.FinancialPlanIds
import pe.kipu.core.domain.repository.EnvelopePlanRepository
import pe.kipu.core.domain.repository.EnvelopeRepository
import pe.kipu.core.domain.repository.FinancialPlanRepository

class DeleteEnvelopeUseCaseTest {
    private val envelope = Envelope(
        id = "envelope-1",
        name = "Comida",
        weeklyLimit = money("100.00"),
        categoryId = "category-food",
    )
    private val envelopeRepository = FakeEnvelopeRepository(envelope)
    private val financialPlanRepository = FakeFinancialPlanRepository(envelope.id)
    private val envelopePlanRepository = FakeEnvelopePlanRepository()
    private val useCase = DeleteEnvelopeUseCase(
        envelopeRepository = envelopeRepository,
        financialPlanRepository = financialPlanRepository,
        envelopePlanRepository = envelopePlanRepository,
    )

    @Test
    fun `deletes envelope and unlinks it from plan through one repository operation`() = runTest {
        val result = useCase(envelope.id)

        assertTrue(result.isSuccess)
        assertEquals(envelope.id, envelopePlanRepository.deletedEnvelopeId)
        assertTrue(envelope.id !in envelopePlanRepository.updatedPlan?.envelopeIds.orEmpty())
    }

    @Test
    fun `does not execute transaction when envelope does not exist`() = runTest {
        val result = useCase("missing")

        assertTrue(result.isFailure)
        assertEquals(null, envelopePlanRepository.deletedEnvelopeId)
    }

    private class FakeEnvelopeRepository(envelope: Envelope) : EnvelopeRepository {
        private val envelopes = MutableStateFlow(listOf(envelope))

        override fun observeEnvelopes() = envelopes
        override suspend fun getById(id: String) = envelopes.value.firstOrNull { it.id == id }
        override suspend fun save(envelope: Envelope) = Result.success(Unit)
        override suspend fun delete(id: String) = Result.success(Unit)
    }

    private class FakeFinancialPlanRepository(envelopeId: String) : FinancialPlanRepository {
        private val plan = FinancialPlan(
            id = FinancialPlanIds.PRIMARY,
            estimatedMonthlyIncome = money("3000.00"),
            fixedExpenses = money("1000.00"),
            envelopeIds = listOf(envelopeId),
        )

        override fun observePlans() = MutableStateFlow(listOf(plan))
        override suspend fun getById(id: String) = plan.takeIf { it.id == id }
        override suspend fun save(plan: FinancialPlan) = Result.success(Unit)
        override suspend fun delete(id: String) = Result.success(Unit)
    }

    private class FakeEnvelopePlanRepository : EnvelopePlanRepository {
        var deletedEnvelopeId: String? = null
        var updatedPlan: FinancialPlan? = null

        override suspend fun saveEnvelopeWithPlan(
            envelope: Envelope,
            plan: FinancialPlan?,
        ) = Result.success(Unit)

        override suspend fun deleteEnvelopeWithPlan(
            envelopeId: String,
            plan: FinancialPlan?,
        ): Result<Unit> {
            deletedEnvelopeId = envelopeId
            updatedPlan = plan
            return Result.success(Unit)
        }
    }

    private companion object {
        fun money(value: String): Money = when (val result = Money.of(BigDecimal(value))) {
            is pe.kipu.core.domain.model.DomainResult.Ok -> result.value
            is pe.kipu.core.domain.model.DomainResult.Err -> error(result.error.message)
        }
    }
}
