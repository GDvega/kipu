package pe.kipu.app

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.time.Instant
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pe.kipu.core.domain.duplicate.MovementDuplicateMatcher
import pe.kipu.core.domain.model.Category
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.OcrImage
import pe.kipu.core.domain.ocr.ReceiptImageLoader
import pe.kipu.core.domain.ocr.ReceiptOcrEngine
import pe.kipu.core.domain.parser.PlinReceiptParser
import pe.kipu.core.domain.parser.ReceiptParserRouter
import pe.kipu.core.domain.parser.YapeReceiptParser
import pe.kipu.core.domain.repository.CategoryRepository
import pe.kipu.core.domain.repository.MovementRepository
import pe.kipu.core.domain.time.TimeProvider
import pe.kipu.core.domain.usecase.ConfirmReceiptMovementUseCase
import pe.kipu.core.domain.usecase.ConfirmSuggestedMovementWithDuplicateCheckUseCase
import pe.kipu.core.domain.usecase.DetectDuplicateMovementUseCase
import pe.kipu.core.domain.usecase.ParseReceiptTextUseCase
import pe.kipu.core.domain.usecase.ProcessReceiptImageUseCase
import pe.kipu.core.domain.usecase.SuggestCategoryFromPlinHistoryUseCase
import pe.kipu.core.domain.usecase.SuggestCategoryFromYapeMessageUseCase
import pe.kipu.feature.receipts.ReceiptCaptureUriFactory
import pe.kipu.feature.receipts.navigation.ReceiptRoutes
import pe.kipu.feature.receipts.presentation.ReceiptReviewUiState
import pe.kipu.feature.receipts.presentation.ReceiptReviewViewModel

@RunWith(AndroidJUnit4::class)
class ReceiptReviewViewModelErrorInstrumentedTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun unexpectedImageLoadFailureCanBeRetried() {
        var attempts = 0
        val viewModel = createViewModel(
            contentUri = "content://example/receipt.jpg",
            receiptImageLoader = object : ReceiptImageLoader {
                override suspend fun load(contentUri: String): Result<OcrImage> =
                    if (attempts++ == 0) {
                        throw IllegalStateException("expected load failure")
                    } else {
                        Result.success(OcrImage(bytes = byteArrayOf(), width = 1, height = 1))
                    }
            },
        )

        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value is ReceiptReviewUiState.Error
        }
        assertEquals(
            "No pudimos procesar el comprobante",
            (viewModel.uiState.value as ReceiptReviewUiState.Error).message,
        )

        viewModel.retryProcess()
        viewModel.retryProcess()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value is ReceiptReviewUiState.Ready
        }
        assertEquals(2, attempts)
    }

    @Test
    fun clearingReviewDeletesItsOwnCameraCapture() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val captureUri = ReceiptCaptureUriFactory.create(context)
        val captureFile = File(context.cacheDir, "receipts/${captureUri.lastPathSegment}")
        captureFile.writeBytes(MINIMAL_JPEG)
        val store = ViewModelStore()

        try {
            composeRule.runOnIdle {
                ViewModelProvider(store, viewModelFactory(captureUri.toString()))[
                    ReceiptReviewViewModel::class.java
                ]
                store.clear()
            }

            assertFalse(captureFile.exists())
        } finally {
            captureFile.delete()
        }
    }

    private fun createViewModel(
        contentUri: String,
        receiptImageLoader: ReceiptImageLoader,
    ): ReceiptReviewViewModel = ReceiptReviewViewModel(
        savedStateHandle = SavedStateHandle(
            mapOf(ReceiptRoutes.CONTENT_URI_ARG to encode(contentUri)),
        ),
        appContext = InstrumentationRegistry.getInstrumentation().targetContext,
        receiptImageLoader = receiptImageLoader,
        processReceiptImage = processReceiptImage(),
        confirmReceiptMovement = unusedConfirmReceiptMovement(),
        categoryRepository = EmptyCategoryRepository,
        timeProvider = TimeProvider { Instant.EPOCH },
    )

    private fun viewModelFactory(contentUri: String): ViewModelProvider.Factory =
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                createViewModel(
                    contentUri = contentUri,
                    receiptImageLoader = object : ReceiptImageLoader {
                        override suspend fun load(contentUri: String): Result<OcrImage> =
                            Result.failure(IllegalStateException("not needed"))
                    },
                ) as T
        }

    private fun encode(value: String): String = java.net.URLEncoder.encode(value, Charsets.UTF_8.name())

    private fun processReceiptImage(): ProcessReceiptImageUseCase = ProcessReceiptImageUseCase(
        receiptOcrEngine = object : ReceiptOcrEngine {
            override suspend fun recognize(image: OcrImage): Result<String> = Result.success("comprobante")
        },
        parseReceiptText = ParseReceiptTextUseCase(
            receiptParserRouter = ReceiptParserRouter(YapeReceiptParser(), PlinReceiptParser()),
            suggestCategoryFromYapeMessage = SuggestCategoryFromYapeMessageUseCase(),
            suggestCategoryFromPlinHistory = SuggestCategoryFromPlinHistoryUseCase(EmptyMovementRepository),
        ),
    )

    private fun unusedConfirmReceiptMovement(): ConfirmReceiptMovementUseCase = ConfirmReceiptMovementUseCase(
        confirmWithDuplicateCheck = ConfirmSuggestedMovementWithDuplicateCheckUseCase(
            movementRepository = EmptyMovementRepository,
            detectDuplicateMovement = DetectDuplicateMovementUseCase(MovementDuplicateMatcher()),
        ),
        timeProvider = TimeProvider { Instant.EPOCH },
    )

    private object EmptyCategoryRepository : CategoryRepository {
        override fun observeCategories() = flowOf(emptyList<Category>())
        override suspend fun getById(id: String): Category? = null
        override suspend fun save(category: Category): Result<Unit> = Result.success(Unit)
        override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    }

    private object EmptyMovementRepository : MovementRepository {
        override fun observeMovements() = flowOf(emptyList<Movement>())
        override suspend fun getById(id: String): Movement? = null
        override suspend fun findByCounterpartyName(counterpartyName: String): List<Movement> = emptyList()
        override suspend fun save(movement: Movement): Result<Unit> = Result.success(Unit)
        override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    }

    private companion object {
        val MINIMAL_JPEG: ByteArray = byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte(),
        )
    }
}
