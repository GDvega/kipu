package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.model.Envelope
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
        val result = useCase(proposal(currentLimit = "100.00", proposedLimit = "60.00"))

        assertTrue(result.isSuccess)
        assertEquals(BigDecimal("60.00"), repository.envelope.weeklyLimit.amount)
    }

    @Test
    fun `stale proposal is rejected without overwriting a newer limit`() = runTest {
        repository.envelope = repository.envelope.copy(weeklyLimit = money("90.00"))

        val result = useCase(proposal(currentLimit = "100.00", proposedLimit = "60.00"))

        assertTrue(result.isFailure)
        assertEquals(BigDecimal("90.00"), repository.envelope.weeklyLimit.amount)
        assertEquals(0, repository.saveCount)
    }

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
        var envelope = initial
        var saveCount = 0

        override fun observeEnvelopes(): Flow<List<Envelope>> = flowOf(listOf(envelope))
        override suspend fun getById(id: String): Envelope? = envelope.takeIf { it.id == id }
        override suspend fun save(envelope: Envelope): Result<Unit> {
            this.envelope = envelope
            saveCount++
            return Result.success(Unit)
        }
        override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    }
}
