package pe.kipu.core.domain.usecase

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.Gathering
import pe.kipu.core.domain.model.GatheringExpense
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.repository.GatheringExpenseRepository
import pe.kipu.core.domain.repository.GatheringRepository
import java.math.BigDecimal
import java.time.Instant

class UpdateGatheringUseCaseTest {

    private val repository = FakeGatheringRepository(
        Gathering(
            id = "gathering-1",
            name = "Asado",
            participantCount = 2,
            participantNames = listOf("Ana", "Luis"),
            isSettled = true,
        ),
    )
    private val expenseRepository = FakeGatheringExpenseRepository()
    private val useCase = UpdateGatheringUseCase(repository, expenseRepository)

    @Test
    fun updatesExistingGathering() = runTest {
        val result = useCase(
            id = "gathering-1",
            name = " Cena ",
            participantsInput = "Ana\nPedro",
        )

        assertTrue(result is DomainResult.Ok)
        assertEquals("Cena", repository.saved.last().name)
        assertEquals(2, repository.saved.last().participantCount)
        assertTrue(repository.saved.last().isSettled)
    }

    @Test
    fun rejectsRemovingParticipantReferencedByAnExpense() = runTest {
        expenseRepository.expenses = listOf(expense(paidBy = "Ana"))

        val result = useCase(
            id = "gathering-1",
            name = "Cena",
            participantsInput = "Luis\nPedro",
        )

        assertTrue(result is DomainResult.Err)
        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun rejectsUnknownGathering() = runTest {
        val result = useCase(
            id = "missing",
            name = "Test",
            participantsInput = "Ana",
        )

        assertTrue(result is DomainResult.Err)
    }

    private class FakeGatheringRepository(
        initial: Gathering,
    ) : GatheringRepository {
        val saved = mutableListOf<Gathering>()
        private val gatherings = MutableStateFlow(listOf(initial))

        override fun observeGatherings() = gatherings

        override suspend fun getById(id: String): Gathering? =
            gatherings.value.firstOrNull { it.id == id }

        override suspend fun save(gathering: Gathering): Result<Unit> {
            saved += gathering
            gatherings.value = listOf(gathering)
            return Result.success(Unit)
        }

        override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    }

    private class FakeGatheringExpenseRepository : GatheringExpenseRepository {
        var expenses: List<GatheringExpense> = emptyList()

        override fun observeTotalsByGathering() = flowOf(emptyMap<String, Money>())
        override fun observeExpensesByGathering() = flowOf(expenses.groupBy { it.gatheringId })
        override fun observeLinkedMovementIds() = flowOf(emptySet<String>())
        override fun observeActiveGatheringLinkedMovementIds() = flowOf(emptySet<String>())
        override suspend fun isMovementLinked(movementId: String) = false
        override suspend fun save(expense: GatheringExpense) = Result.success(Unit)
    }

    private fun expense(paidBy: String) = GatheringExpense(
        id = "expense-1",
        gatheringId = "gathering-1",
        amount = Money.of(BigDecimal("50.00")).getOrError(),
        paidByParticipant = paidBy,
        recordedAt = Instant.EPOCH,
    )
}
