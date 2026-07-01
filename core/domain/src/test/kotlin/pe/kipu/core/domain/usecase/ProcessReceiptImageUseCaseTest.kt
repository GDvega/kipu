package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import pe.kipu.core.domain.model.DomainError
import pe.kipu.core.domain.model.OcrImage
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.ReceiptParseResult
import pe.kipu.core.domain.ocr.ReceiptOcrEngine
import pe.kipu.core.domain.parser.PlinReceiptParser
import pe.kipu.core.domain.parser.ReceiptParserRouter
import pe.kipu.core.domain.parser.YapeReceiptParser

class ProcessReceiptImageUseCaseTest {

    private val yapeOcrText = """
        Yape!
        Pagaste
        S/ 25.50
        Para
        MARIA GARCIA RIOS
        16 jun. 2026 - 3:45 p. m.
        Nro. de operación
        000123456
        Mensaje
        almuerzo con amigos
    """.trimIndent()

    @Test
    fun `fake ocr text flows through parse pipeline`() = runTest {
        val fakeOcr = object : ReceiptOcrEngine {
            override suspend fun recognize(image: OcrImage): Result<String> = Result.success(yapeOcrText)
        }
        val parseReceiptText = ParseReceiptTextUseCase(
            receiptParserRouter = ReceiptParserRouter(YapeReceiptParser(), PlinReceiptParser()),
            suggestCategoryFromYapeMessage = SuggestCategoryFromYapeMessageUseCase(),
            suggestCategoryFromPlinHistory = SuggestCategoryFromPlinHistoryUseCase(FakeMovementRepository()),
        )
        val useCase = ProcessReceiptImageUseCase(
            receiptOcrEngine = fakeOcr,
            parseReceiptText = parseReceiptText,
        )

        val result = useCase(
            OcrImage(bytes = byteArrayOf(1), width = 10, height = 10),
        ) as ReceiptParseResult.Success

        assertEquals(PaymentChannel.YAPE, result.suggestion.channel)
        assertEquals(BigDecimal("25.50"), result.suggestion.amount?.amount)
        assertEquals("000123456", result.suggestion.operationReference)
    }

    @Test
    fun `ocr failure returns parse failure`() = runTest {
        val fakeOcr = object : ReceiptOcrEngine {
            override suspend fun recognize(image: OcrImage): Result<String> =
                Result.failure(IllegalStateException("ocr failed"))
        }
        val parseReceiptText = ParseReceiptTextUseCase(
            receiptParserRouter = ReceiptParserRouter(YapeReceiptParser(), PlinReceiptParser()),
            suggestCategoryFromYapeMessage = SuggestCategoryFromYapeMessageUseCase(),
            suggestCategoryFromPlinHistory = SuggestCategoryFromPlinHistoryUseCase(FakeMovementRepository()),
        )
        val useCase = ProcessReceiptImageUseCase(fakeOcr, parseReceiptText)

        val result = useCase(OcrImage(bytes = byteArrayOf(1), width = 10, height = 10))

        assertEquals(
            DomainError.InvalidField("Could not recognize receipt text"),
            (result as ReceiptParseResult.Failure).error,
        )
    }

    private class FakeMovementRepository : pe.kipu.core.domain.repository.MovementRepository {
        override fun observeMovements() =
            kotlinx.coroutines.flow.flowOf(emptyList<pe.kipu.core.domain.model.Movement>())

        override suspend fun getById(id: String) = null

        override suspend fun findByCounterpartyName(counterpartyName: String) = emptyList<pe.kipu.core.domain.model.Movement>()

        override suspend fun save(movement: pe.kipu.core.domain.model.Movement) = Result.success(Unit)

        override suspend fun delete(id: String) = Result.success(Unit)
    }
}
