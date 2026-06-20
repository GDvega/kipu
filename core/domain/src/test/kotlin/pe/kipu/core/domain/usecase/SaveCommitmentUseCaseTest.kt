package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.model.Commitment
import pe.kipu.core.domain.model.CommitmentType
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.repository.CommitmentRepository
import pe.kipu.core.domain.time.FixedTimeProvider
import java.time.Instant

class SaveCommitmentUseCaseTest {

    private val now = Instant.parse("2026-06-16T15:00:00Z")
    private val repository = FakeCommitmentRepository()
    private val useCase = SaveCommitmentUseCase(
        commitmentRepository = repository,
        timeProvider = FixedTimeProvider(now),
    )

    @Test
    fun createsSavingsGoal() = runTest {
        val result = useCase(
            type = CommitmentType.SAVINGS_GOAL,
            title = "Viaje",
            targetAmount = Money.of(BigDecimal("500.00")).getOrError(),
            currentAmount = Money.of(BigDecimal("100.00")).getOrError(),
        )

        assertTrue(result.isSuccess)
        assertEquals(1, repository.saved.size)
        assertEquals("commitment-${now.toEpochMilli()}", repository.saved.first().id)
    }

    @Test
    fun updatesExistingCommitment() = runTest {
        repository.commitments.value = listOf(
            Commitment(
                id = "commitment-1",
                type = CommitmentType.SOCIAL_DEBT,
                title = "Deuda",
                currentAmount = Money.of(BigDecimal("50.00")).getOrError(),
                counterpartyName = "Ana",
            ),
        )

        val result = useCase(
            existingId = "commitment-1",
            type = CommitmentType.SOCIAL_DEBT,
            title = "Deuda actualizada",
            currentAmount = Money.of(BigDecimal("40.00")).getOrError(),
            counterpartyName = "Ana",
        )

        assertTrue(result.isSuccess)
        assertEquals("Deuda actualizada", repository.saved.last().title)
    }

    @Test
    fun rejectsBlankTitle() = runTest {
        val result = useCase(
            type = CommitmentType.SOCIAL_DEBT,
            title = "  ",
            currentAmount = Money.of(BigDecimal("10.00")).getOrError(),
        )

        assertTrue(result.isFailure)
        assertEquals(0, repository.saved.size)
    }

    private class FakeCommitmentRepository : CommitmentRepository {
        val saved = mutableListOf<Commitment>()
        val commitments = MutableStateFlow<List<Commitment>>(emptyList())

        override fun observeCommitments() = commitments

        override suspend fun getById(id: String): Commitment? =
            commitments.value.find { it.id == id }

        override suspend fun save(commitment: Commitment): Result<Unit> {
            saved += commitment
            commitments.value = commitments.value.filterNot { it.id == commitment.id } + commitment
            return Result.success(Unit)
        }

        override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    }
}

private fun <T> pe.kipu.core.domain.model.DomainResult<T>.getOrError(): T = when (this) {
    is pe.kipu.core.domain.model.DomainResult.Ok -> value
    is pe.kipu.core.domain.model.DomainResult.Err -> error("Expected Ok")
}
