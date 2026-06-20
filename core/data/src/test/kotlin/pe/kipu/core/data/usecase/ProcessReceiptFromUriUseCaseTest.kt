package pe.kipu.core.data.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.model.OcrImage
import pe.kipu.core.domain.model.ReceiptParseResult
import pe.kipu.core.domain.ocr.ReceiptImageLoader
import pe.kipu.core.domain.ocr.ReceiptOcrEngine
import pe.kipu.core.domain.parser.PlinReceiptParser
import pe.kipu.core.domain.parser.ReceiptParserRouter
import pe.kipu.core.domain.parser.YapeReceiptParser
import pe.kipu.core.domain.usecase.ParseReceiptTextUseCase
import pe.kipu.core.domain.usecase.SuggestCategoryFromPlinHistoryUseCase
import pe.kipu.core.domain.usecase.SuggestCategoryFromYapeMessageUseCase

class ProcessReceiptFromUriUseCaseTest {

    private val fakeImage = OcrImage(
        bytes = byteArrayOf(1, 2, 3),
        width = 100,
        height = 200,
    )

    private val parseReceiptText = ParseReceiptTextUseCase(
        receiptParserRouter = ReceiptParserRouter(YapeReceiptParser(), PlinReceiptParser()),
        suggestCategoryFromYapeMessage = SuggestCategoryFromYapeMessageUseCase(),
        suggestCategoryFromPlinHistory = SuggestCategoryFromPlinHistoryUseCase(FakeMovementRepository()),
    )

    @Test
    fun returnsParseResultWhenImageLoads() = runTest {
        val yapeOcrText = """
            Yape!
            Pagaste
            S/ 25.50
            Para
            MARIA GARCIA RIOS
        """.trimIndent()
        val useCase = ProcessReceiptFromUriUseCase(
            receiptImageLoader = FakeReceiptImageLoader(Result.success(fakeImage)),
            processReceiptImage = ProcessReceiptImageUseCase(
                receiptOcrEngine = FakeReceiptOcrEngine(Result.success(yapeOcrText)),
                parseReceiptText = parseReceiptText,
            ),
        )

        val result = useCase("content://test/receipt.jpg")

        assertTrue(result is ReceiptParseResult.Success)
    }

    @Test
    fun returnsFailureWhenImageLoadFails() = runTest {
        val useCase = ProcessReceiptFromUriUseCase(
            receiptImageLoader = FakeReceiptImageLoader(Result.failure(IllegalStateException("denied"))),
            processReceiptImage = ProcessReceiptImageUseCase(
                receiptOcrEngine = FakeReceiptOcrEngine(Result.success("text")),
                parseReceiptText = parseReceiptText,
            ),
        )

        val result = useCase("content://test/missing.jpg")

        assertTrue(result is ReceiptParseResult.Failure)
        assertEquals(
            "Could not load receipt image",
            (result as ReceiptParseResult.Failure).error.message,
        )
    }

    private class FakeReceiptImageLoader(
        private val result: Result<OcrImage>,
    ) : ReceiptImageLoader {
        override suspend fun load(contentUri: String): Result<OcrImage> = result
    }

    private class FakeReceiptOcrEngine(
        private val result: Result<String>,
    ) : ReceiptOcrEngine {
        override suspend fun recognize(image: OcrImage): Result<String> = result
    }

    private class FakeMovementRepository : pe.kipu.core.domain.repository.MovementRepository {
        override fun observeMovements() =
            kotlinx.coroutines.flow.flowOf(emptyList<pe.kipu.core.domain.model.Movement>())

        override suspend fun getById(id: String) = null

        override suspend fun findByCounterpartyName(counterpartyName: String) =
            emptyList<pe.kipu.core.domain.model.Movement>()

        override suspend fun save(movement: pe.kipu.core.domain.model.Movement) = Result.success(Unit)

        override suspend fun delete(id: String) = Result.success(Unit)
    }
}
