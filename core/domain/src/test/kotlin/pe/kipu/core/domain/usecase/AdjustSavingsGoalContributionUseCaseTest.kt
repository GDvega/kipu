package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.model.Commitment
import pe.kipu.core.domain.model.CommitmentType
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.repository.CommitmentRepository

class AdjustSavingsGoalContributionUseCaseTest {
    private val goal = Commitment(
        id = "goal", type = CommitmentType.SAVINGS_GOAL, title = "Laptop", targetAmount = money("1000"),
        currentAmount = money("100"), dueDate = LocalDate.of(2026, 12, 1), counterpartyName = "Tienda",
        savingsHorizonMonths = 4,
    )
    private val repository = GoalRepository(goal)
    private val useCase = AdjustSavingsGoalContributionUseCase(repository)

    @Test
    fun `consecutive deposits use current stored amount and preserve metadata`() = runTest {
        useCase(goal.id, money("50"), isDeposit = true).getOrThrow()
        useCase(goal.id, money("30"), isDeposit = true).getOrThrow()
        assertEquals(goal.copy(currentAmount = money("180")), repository.getById(goal.id))
    }

    @Test
    fun `withdrawal reverses declared savings without creating movements`() = runTest {
        useCase(goal.id, money("50"), isDeposit = true).getOrThrow()
        useCase(goal.id, money("50"), isDeposit = false).getOrThrow()
        assertEquals(goal, repository.getById(goal.id))
    }

    @Test
    fun `invalid withdrawal cannot silently reset savings to zero`() = runTest {
        assertTrue(useCase(goal.id, money("101"), isDeposit = false).isFailure)
        assertEquals(goal, repository.getById(goal.id))
    }

    @Test
    fun `missing settled non goal and non PEN destinations are rejected`() = runTest {
        assertTrue(useCase("missing", money("50"), true).isFailure)
        listOf(
            goal.copy(isSettled = true), goal.copy(type = CommitmentType.PENDING_PAYMENT),
            goal.copy(currencyCode = "USD"),
        ).forEach { invalid ->
            repository.save(invalid)
            assertTrue(useCase(goal.id, money("50"), true).isFailure)
            assertEquals(invalid, repository.getById(goal.id))
        }
    }

    private class GoalRepository(goal: Commitment) : CommitmentRepository {
        private val items = MutableStateFlow(listOf(goal))
        override fun observeCommitments() = items
        override suspend fun getById(id: String) = items.value.find { it.id == id }
        override suspend fun save(commitment: Commitment): Result<Unit> {
            items.value = items.value.filterNot { it.id == commitment.id } + commitment
            return Result.success(Unit)
        }
        override suspend fun delete(id: String) = Result.success(Unit)
    }

    private companion object {
        fun money(value: String) = Money.of(BigDecimal(value)).getOrError()
    }
}
