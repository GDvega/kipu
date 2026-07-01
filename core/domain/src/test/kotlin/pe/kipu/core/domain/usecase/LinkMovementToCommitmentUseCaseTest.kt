package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.model.Commitment
import pe.kipu.core.domain.model.CommitmentType
import pe.kipu.core.domain.model.DomainResult
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

class LinkMovementToCommitmentUseCaseTest {

    private val now = Instant.parse("2026-06-16T12:00:00Z")
    private val movementRepository = FakeMovementRepository()
    private val commitmentRepository = FakeCommitmentRepository()
    private val useCase = LinkMovementToCommitmentUseCase(movementRepository, commitmentRepository)

    @Test
    fun `links confirmed income movement to savings goal`() = runTest {
        movementRepository.movements = listOf(incomeMovement(id = "mov-1"))
        commitmentRepository.commitments = listOf(savingsGoal(id = "goal-1"))

        val result = useCase("mov-1", "goal-1")

        assertTrue(result.isSuccess)
        assertEquals("goal-1", movementRepository.movements.first().commitmentId)
    }

    @Test
    fun `unlinks movement when commitment id is null`() = runTest {
        movementRepository.movements = listOf(incomeMovement(id = "mov-1", commitmentId = "goal-1"))
        commitmentRepository.commitments = listOf(savingsGoal(id = "goal-1"))

        val result = useCase("mov-1", null)

        assertTrue(result.isSuccess)
        assertEquals(null, movementRepository.movements.first().commitmentId)
    }

    @Test
    fun `rejects expense movement`() = runTest {
        movementRepository.movements = listOf(
            incomeMovement(id = "mov-1").copy(type = MovementType.EXPENSE),
        )
        commitmentRepository.commitments = listOf(savingsGoal(id = "goal-1"))

        val result = useCase("mov-1", "goal-1")

        assertTrue(result.isFailure)
    }

    @Test
    fun `rejects social debt commitment`() = runTest {
        movementRepository.movements = listOf(incomeMovement(id = "mov-1"))
        commitmentRepository.commitments = listOf(
            Commitment(
                id = "debt-1",
                type = CommitmentType.SOCIAL_DEBT,
                title = "Deuda",
                currentAmount = Money.of(BigDecimal("50.00")).getOrError(),
            ),
        )

        val result = useCase("mov-1", "debt-1")

        assertTrue(result.isFailure)
    }

    private fun incomeMovement(
        id: String,
        commitmentId: String? = null,
    ): Movement = Movement(
        id = id,
        type = MovementType.INCOME,
        amount = Money.of(BigDecimal("100.00")).getOrError(),
        categoryId = "category-other",
        channel = PaymentChannel.YAPE,
        source = MovementSource.MANUAL,
        status = MovementStatus.CONFIRMED,
        commitmentId = commitmentId,
        recordedAt = now,
        createdAt = now,
    )

    private fun savingsGoal(id: String): Commitment = Commitment(
        id = id,
        type = CommitmentType.SAVINGS_GOAL,
        title = "Meta",
        targetAmount = Money.of(BigDecimal("500.00")).getOrError(),
    )

    private class FakeMovementRepository : MovementRepository {
        var movements: List<Movement> = emptyList()

        override fun observeMovements() = kotlinx.coroutines.flow.flowOf(movements)

        override suspend fun getById(id: String): Movement? = movements.find { it.id == id }

        override suspend fun findByCounterpartyName(counterpartyName: String): List<Movement> = emptyList()

        override suspend fun save(movement: Movement): Result<Unit> {
            movements = movements.map { if (it.id == movement.id) movement else it }
            return Result.success(Unit)
        }

        override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    }

    private class FakeCommitmentRepository : CommitmentRepository {
        var commitments: List<Commitment> = emptyList()

        override fun observeCommitments() = kotlinx.coroutines.flow.flowOf(commitments)

        override suspend fun getById(id: String): Commitment? = commitments.find { it.id == id }

        override suspend fun save(commitment: Commitment): Result<Unit> = Result.success(Unit)

        override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    }
}
