package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.model.Envelope
import pe.kipu.core.domain.model.EnvelopeBudgetState
import pe.kipu.core.domain.model.EnvelopeBudgetStatus
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.RecoveryEnvelopeAdjustment
import pe.kipu.core.domain.model.UnexpectedExpenseRecoveryPlan
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.repository.EnvelopeRepository

class ApplyRecoveryPlanUseCaseTest {
    private val repository = RecordingEnvelopeRepository(
        Envelope(
            id = "envelope-leisure",
            name = "Ocio",
            weeklyLimit = money("100.00"),
            categoryId = "category-other",
        ),
    )
    private val useCase = ApplyRecoveryPlanUseCase(repository)

    @Test
    fun `confirmed current proposal updates the envelope limit`() = runTest {
        val result = useCase(proposal(currentLimit = "100.00", proposedLimit = "60.00"), listOf(budget()))

        assertTrue(result.isSuccess)
        assertEquals(BigDecimal("60.00"), repository.envelope.weeklyLimit.amount)
    }

    @Test
    fun `stale proposal is rejected without overwriting a newer limit`() = runTest {
        repository.envelope = repository.envelope.copy(weeklyLimit = money("90.00"))

        val result = useCase(proposal(currentLimit = "100.00", proposedLimit = "60.00"), listOf(budget()))

        assertTrue(result.isFailure)
        assertEquals(BigDecimal("90.00"), repository.envelope.weeklyLimit.amount)
        assertEquals(0, repository.saveCount)
    }

    @Test
    fun `current spending above proposed limit rejects a proposal based on earlier spending`() = runTest {
        val proposal = proposal(currentLimit = "100.00", proposedLimit = "60.00")
        assertEquals(money("20.00"), proposal.adjustments.single().spentAmount)

        val result = useCase(proposal, listOf(budget(spent = "80.00")))

        assertTrue(result.isFailure)
        assertEquals(money("100.00"), repository.envelope.weeklyLimit)
        assertEquals(0, repository.saveCount)
    }

    @Test
    fun `all adjustments are validated before the first envelope is written`() = runTest {
        val second = repository.envelope.copy(id = "envelope-extra", name = "Extras")
        repository.envelopes[second.id] = second
        val firstProposal = proposal(currentLimit = "100.00", proposedLimit = "60.00")
        val proposal = firstProposal.copy(
            adjustments = firstProposal.adjustments + firstProposal.adjustments.single().copy(
                envelopeId = second.id,
                envelopeName = second.name,
            ),
        )
        val originalEnvelopes = repository.envelopes.toMap()

        val result = useCase(proposal, listOf(budget(), budget(second, spent = "80.00")))

        assertTrue(result.isFailure)
        assertEquals(originalEnvelopes, repository.envelopes)
        assertEquals(0, repository.saveCount)
    }

    private fun budget(envelope: Envelope = repository.envelope, spent: String = "20.00") = EnvelopeBudgetState(
        envelopeId = envelope.id,
        name = envelope.name,
        categoryId = envelope.categoryId,
        weeklyLimit = envelope.weeklyLimit,
        spentAmount = money(spent),
        remainingAmount = money((envelope.weeklyLimit.amount - BigDecimal(spent)).toPlainString()),
        percentUsed = CalculateEnvelopeBudgetStateUseCase.calculatePercentUsed(money(spent), envelope.weeklyLimit),
        status = if (BigDecimal(spent) >= envelope.weeklyLimit.amount.multiply(BigDecimal("0.80"))) {
            EnvelopeBudgetStatus.ADJUSTED
        } else {
            EnvelopeBudgetStatus.OK
        },
    )

    private fun proposal(currentLimit: String, proposedLimit: String) = UnexpectedExpenseRecoveryPlan(
        adjustments = listOf(
            RecoveryEnvelopeAdjustment(
                envelopeId = repository.envelope.id,
                envelopeName = repository.envelope.name,
                currentLimit = money(currentLimit),
                spentAmount = money("20.00"),
                proposedLimit = money(proposedLimit),
                reduction = money("40.00"),
            ),
        ),
        remainingGap = Money.ZERO,
        isFullyRecoverable = true,
    )

    private fun money(value: String) = Money.of(BigDecimal(value)).getOrError()

    private class RecordingEnvelopeRepository(initial: Envelope) : EnvelopeRepository {
        private val initialId = initial.id
        val envelopes = linkedMapOf(initial.id to initial)
        var envelope: Envelope
            get() = envelopes.getValue(initialId)
            set(value) { envelopes[initialId] = value }
        var saveCount = 0

        override fun observeEnvelopes(): Flow<List<Envelope>> = flowOf(envelopes.values.toList())
        override suspend fun getById(id: String): Envelope? = envelopes[id]
        override suspend fun save(envelope: Envelope): Result<Unit> {
            envelopes[envelope.id] = envelope
            saveCount++
            return Result.success(Unit)
        }
        override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    }
}
