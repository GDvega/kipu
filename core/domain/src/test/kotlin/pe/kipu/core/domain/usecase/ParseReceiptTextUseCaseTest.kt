package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import java.time.Instant
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.category.YapeMessageCategoryRules
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.ReceiptParseResult
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.parser.PlinReceiptParser
import pe.kipu.core.domain.parser.ReceiptParserRouter
import pe.kipu.core.domain.parser.YapeReceiptParser
import pe.kipu.core.domain.repository.MovementRepository
import pe.kipu.core.domain.test.ReceiptFixtureLoader

class ParseReceiptTextUseCaseTest {

    private val now = Instant.parse("2026-06-16T12:00:00Z")

    @Test
    fun `yape receipt with message suggests food category`() = runTest {
        val useCase = createUseCase(FakeMovementRepository())

        val text = ReceiptFixtureLoader.load("receipts/yape_standard.txt")
        val result = useCase(text) as ReceiptParseResult.Success

        assertEquals(CategoryIds.FOOD, result.suggestion.categoryId)
        assertEquals(YapeMessageCategoryRules.REASON_KEY, result.suggestion.categorySuggestionReason)
    }

    @Test
    fun `plin without message does not set category without history`() = runTest {
        val useCase = createUseCase(FakeMovementRepository())

        val text = ReceiptFixtureLoader.load("receipts/plin_no_message.txt")
        val result = useCase(text) as ReceiptParseResult.Success

        assertNull(result.suggestion.categoryId)
        assertEquals(PaymentChannel.PLIN, result.suggestion.channel)
    }

    @Test
    fun `plin without message suggests category from counterparty history`() = runTest {
        val history = listOf(
            sampleMovement(
                id = "m1",
                counterparty = "ANA TORRES",
                categoryId = CategoryIds.TRANSPORT,
            ),
        )
        val useCase = createUseCase(FakeMovementRepository(history))

        val text = ReceiptFixtureLoader.load("receipts/plin_no_message.txt")
        val result = useCase(text) as ReceiptParseResult.Success

        assertEquals(CategoryIds.TRANSPORT, result.suggestion.categoryId)
        assertEquals(SuggestCategoryFromPlinHistoryUseCase.REASON_KEY, result.suggestion.categorySuggestionReason)
    }

    private fun createUseCase(repository: MovementRepository): ParseReceiptTextUseCase =
        ParseReceiptTextUseCase(
            receiptParserRouter = ReceiptParserRouter(YapeReceiptParser(), PlinReceiptParser()),
            suggestCategoryFromYapeMessage = SuggestCategoryFromYapeMessageUseCase(),
            suggestCategoryFromPlinHistory = SuggestCategoryFromPlinHistoryUseCase(repository),
        )

    private fun sampleMovement(
        id: String,
        counterparty: String,
        categoryId: String,
    ) = Movement(
        id = id,
        type = MovementType.EXPENSE,
        amount = Money.of(BigDecimal("10.00")).getOrError(),
        categoryId = categoryId,
        channel = PaymentChannel.PLIN,
        source = MovementSource.MANUAL,
        status = MovementStatus.CONFIRMED,
        counterpartyName = counterparty,
        recordedAt = now,
        createdAt = now,
    )

    private class FakeMovementRepository(
        private val movements: List<Movement> = emptyList(),
    ) : MovementRepository {
        override fun observeMovements() = flowOf(movements)
        override suspend fun getById(id: String) = movements.find { it.id == id }

        override suspend fun findByCounterpartyName(counterpartyName: String): List<Movement> =
            movements.filter { it.counterpartyName.equals(counterpartyName, ignoreCase = true) }

        override suspend fun save(movement: Movement) = Result.success(Unit)
        override suspend fun delete(id: String) = Result.success(Unit)
    }
}
