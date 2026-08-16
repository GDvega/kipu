package pe.kipu.feature.receipts.presentation

import android.content.Context
import android.content.ContextWrapper
import androidx.lifecycle.SavedStateHandle
import java.io.File
import java.math.BigDecimal
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.duplicate.MovementDuplicateMatcher
import pe.kipu.core.domain.model.Category
import pe.kipu.core.domain.model.DuplicateResolution
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.OcrImage
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.getOrError
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
import pe.kipu.feature.receipts.navigation.ReceiptRoutes

@OptIn(ExperimentalCoroutinesApi::class)
class ReceiptReviewViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val now = Instant.parse("2026-06-16T20:45:00Z")
    private val timeProvider = object : TimeProvider {
        override fun now(): Instant = now
    }

    private val validUri = "content://media/external/images/123"
    private val encodedUri = ReceiptRoutes.review(validUri).substringAfter("receipts/review/")

    private lateinit var fakeImageLoader: FakeReceiptImageLoader
    private lateinit var fakeCategoryRepository: FakeCategoryRepository
    private lateinit var fakeMovementRepository: FakeMovementRepository
    private lateinit var fakeOcrEngine: FakeReceiptOcrEngine
    private lateinit var router: ReceiptParserRouter
    private lateinit var parseReceiptText: ParseReceiptTextUseCase
    private lateinit var processReceiptImage: ProcessReceiptImageUseCase
    private lateinit var confirmReceiptMovement: ConfirmReceiptMovementUseCase
    private lateinit var detectDuplicate: DetectDuplicateMovementUseCase
    private lateinit var confirmWithDuplicateCheck: ConfirmSuggestedMovementWithDuplicateCheckUseCase

    private val sampleCategory = Category(
        id = CategoryIds.FOOD,
        name = "Alimentación",
        iconKey = "restaurant",
    )

    private val standardYapeReceiptText = """
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

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeImageLoader = FakeReceiptImageLoader()
        fakeCategoryRepository = FakeCategoryRepository(listOf(sampleCategory))
        fakeMovementRepository = FakeMovementRepository()
        fakeOcrEngine = FakeReceiptOcrEngine()
        router = ReceiptParserRouter(
            yapeReceiptParser = YapeReceiptParser(),
            plinReceiptParser = PlinReceiptParser(),
        )
        parseReceiptText = ParseReceiptTextUseCase(
            receiptParserRouter = router,
            suggestCategoryFromYapeMessage = SuggestCategoryFromYapeMessageUseCase(),
            suggestCategoryFromPlinHistory = SuggestCategoryFromPlinHistoryUseCase(fakeMovementRepository),
        )
        processReceiptImage = ProcessReceiptImageUseCase(fakeOcrEngine, parseReceiptText)
        detectDuplicate = DetectDuplicateMovementUseCase(MovementDuplicateMatcher())
        confirmWithDuplicateCheck = ConfirmSuggestedMovementWithDuplicateCheckUseCase(
            movementRepository = fakeMovementRepository,
            detectDuplicateMovement = detectDuplicate,
        )
        confirmReceiptMovement = ConfirmReceiptMovementUseCase(
            confirmWithDuplicateCheck = confirmWithDuplicateCheck,
            timeProvider = timeProvider,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        contentUriArg: String = encodedUri,
        appContext: Context = FakeContext(),
    ): ReceiptReviewViewModel {
        val savedStateHandle = SavedStateHandle(mapOf(ReceiptRoutes.CONTENT_URI_ARG to contentUriArg))
        return ReceiptReviewViewModel(
            savedStateHandle = savedStateHandle,
            appContext = appContext,
            receiptImageLoader = fakeImageLoader,
            processReceiptImage = processReceiptImage,
            confirmReceiptMovement = confirmReceiptMovement,
            categoryRepository = fakeCategoryRepository,
            timeProvider = timeProvider,
        )
    }

    @Test
    fun init_whenSuccessfulParse_transitionsToReadyWithSuggestion() = runTest(testDispatcher) {
        fakeOcrEngine.textToReturn = standardYapeReceiptText

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ReceiptReviewUiState.Ready)
        val ready = state as ReceiptReviewUiState.Ready
        assertEquals("25.50", ready.amountText)
        assertEquals("MARIA GARCIA RIOS", ready.counterpartyText)
        assertEquals("almuerzo con amigos", ready.messageText)
        assertEquals("000123456", ready.operationReferenceText)
        assertNull(ready.parseWarning)
        assertNull(ready.errorMessage)
        assertNull(ready.duplicatePending)
    }

    @Test
    fun init_whenImageLoaderFails_transitionsToError() = runTest(testDispatcher) {
        fakeImageLoader.shouldFail = true

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ReceiptReviewUiState.Error)
        assertEquals("No pudimos abrir el comprobante", (state as ReceiptReviewUiState.Error).message)
    }

    @Test
    fun init_whenUnsupportedChannel_transitionsToReadyWithWarning() = runTest(testDispatcher) {
        fakeOcrEngine.textToReturn = "Transferencia realizada con éxito en Banco X"

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ReceiptReviewUiState.Ready)
        val ready = state as ReceiptReviewUiState.Ready
        assertNotNull(ready.parseWarning)
        assertTrue(ready.parseWarning!!.contains("No reconocimos Yape ni Plin"))
    }

    @Test
    fun init_whenParseFails_transitionsToReadyWithWarning() = runTest(testDispatcher) {
        fakeOcrEngine.textToReturn = "   "

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ReceiptReviewUiState.Ready)
        val ready = state as ReceiptReviewUiState.Ready
        assertNotNull(ready.parseWarning)
        assertTrue(ready.parseWarning!!.contains("No pudimos leer el comprobante"))
    }

    @Test
    fun retryProcess_afterError_reloadsAndTransitionsToReady() = runTest(testDispatcher) {
        fakeImageLoader.shouldFail = true
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is ReceiptReviewUiState.Error)

        fakeImageLoader.shouldFail = false
        fakeOcrEngine.textToReturn = "Otro banco desconocido"
        viewModel.retryProcess()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is ReceiptReviewUiState.Ready)
    }

    @Test
    fun userEdits_updateStateCorrectly() = runTest(testDispatcher) {
        fakeOcrEngine.textToReturn = "Desconocido"
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAmountChanged("50.00")
        viewModel.onCounterpartyChanged("Supermercado")
        viewModel.onMessageChanged("Compras del mes")
        viewModel.onOperationReferenceChanged("REF-999")
        viewModel.onCategorySelected(CategoryIds.FOOD)

        val ready = viewModel.uiState.value as ReceiptReviewUiState.Ready
        assertEquals("50.00", ready.amountText)
        assertEquals("Supermercado", ready.counterpartyText)
        assertEquals("Compras del mes", ready.messageText)
        assertEquals("REF-999", ready.operationReferenceText)
        assertEquals(CategoryIds.FOOD, ready.selectedCategoryId)
    }

    @Test
    fun onConfirm_withInvalidAmount_showsErrorMessage() = runTest(testDispatcher) {
        fakeOcrEngine.textToReturn = "Desconocido"
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAmountChanged("0.00")
        viewModel.onConfirm()
        advanceUntilIdle()

        val ready = viewModel.uiState.value as ReceiptReviewUiState.Ready
        assertNotNull(ready.errorMessage)
    }

    @Test
    fun onConfirm_validMovementWithoutDuplicate_savesAndTransitionsToSaved() = runTest(testDispatcher) {
        fakeOcrEngine.textToReturn = standardYapeReceiptText

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onConfirm()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ReceiptReviewUiState.Saved)
        val saved = state as ReceiptReviewUiState.Saved
        assertEquals(Money.of(BigDecimal("25.50")).getOrError(), saved.movement.amount)
        assertEquals("MARIA GARCIA RIOS", saved.movement.counterpartyName)
    }

    @Test
    fun onConfirm_whenDuplicateExists_setsDuplicatePendingState() = runTest(testDispatcher) {
        val money = Money.of(BigDecimal("25.50")).getOrError()
        val existing = Movement(
            id = "existing-mov-1",
            type = MovementType.EXPENSE,
            amount = money,
            categoryId = CategoryIds.FOOD,
            channel = PaymentChannel.YAPE,
            source = MovementSource.MANUAL,
            status = MovementStatus.CONFIRMED,
            counterpartyName = "MARIA GARCIA RIOS",
            operationNumber = "000123456",
            recordedAt = now,
            createdAt = now,
        )
        fakeMovementRepository.save(existing)

        fakeOcrEngine.textToReturn = standardYapeReceiptText

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onConfirm()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ReceiptReviewUiState.Ready)
        val ready = state as ReceiptReviewUiState.Ready
        assertNotNull(ready.duplicatePending)
    }

    @Test
    fun onResolveDuplicate_merge_transitionsToDuplicateMerged() = runTest(testDispatcher) {
        val money = Money.of(BigDecimal("25.50")).getOrError()
        val existing = Movement(
            id = "existing-mov-1",
            type = MovementType.EXPENSE,
            amount = money,
            categoryId = CategoryIds.FOOD,
            channel = PaymentChannel.YAPE,
            source = MovementSource.MANUAL,
            status = MovementStatus.CONFIRMED,
            counterpartyName = "MARIA GARCIA RIOS",
            operationNumber = "000123456",
            recordedAt = now,
            createdAt = now,
        )
        fakeMovementRepository.save(existing)

        fakeOcrEngine.textToReturn = standardYapeReceiptText

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onConfirm()
        advanceUntilIdle()

        viewModel.onResolveDuplicate(DuplicateResolution.MERGE)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is ReceiptReviewUiState.DuplicateMerged)
    }

    @Test
    fun onResolveDuplicate_cancel_clearsDuplicatePending() = runTest(testDispatcher) {
        val money = Money.of(BigDecimal("25.50")).getOrError()
        val existing = Movement(
            id = "existing-mov-1",
            type = MovementType.EXPENSE,
            amount = money,
            categoryId = CategoryIds.FOOD,
            channel = PaymentChannel.YAPE,
            source = MovementSource.MANUAL,
            status = MovementStatus.CONFIRMED,
            counterpartyName = "MARIA GARCIA RIOS",
            operationNumber = "000123456",
            recordedAt = now,
            createdAt = now,
        )
        fakeMovementRepository.save(existing)

        fakeOcrEngine.textToReturn = standardYapeReceiptText

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onConfirm()
        advanceUntilIdle()

        viewModel.onResolveDuplicate(DuplicateResolution.CANCEL)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ReceiptReviewUiState.Ready)
        assertNull((state as ReceiptReviewUiState.Ready).duplicatePending)
    }

    // --- Fakes ---

    private class FakeContext : ContextWrapper(null) {
        override fun getPackageName(): String = "pe.kipu.app"
        override fun getCacheDir(): File = File(System.getProperty("java.io.tmpdir"), "kipu-test-cache")
    }

    private class FakeReceiptImageLoader : ReceiptImageLoader {
        var shouldFail = false
        override suspend fun load(contentUri: String): Result<OcrImage> {
            if (shouldFail) return Result.failure(IllegalArgumentException("Failed to open URI"))
            return Result.success(
                OcrImage(
                    bytes = byteArrayOf(1, 2, 3),
                    width = 100,
                    height = 100,
                ),
            )
        }
    }

    private class FakeReceiptOcrEngine : ReceiptOcrEngine {
        var textToReturn: String = "Yape S/ 25.50 a Bodega Juan"
        override suspend fun recognize(image: OcrImage): Result<String> = Result.success(textToReturn)
    }

    private class FakeCategoryRepository(initial: List<Category>) : CategoryRepository {
        val categories = MutableStateFlow(initial)
        override fun observeCategories(): Flow<List<Category>> = categories
        override suspend fun getById(id: String): Category? = categories.value.find { it.id == id }
        override suspend fun save(category: Category): Result<Unit> {
            categories.value = categories.value + category
            return Result.success(Unit)
        }
        override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    }

    private class FakeMovementRepository : MovementRepository {
        val movements = MutableStateFlow<Map<String, Movement>>(emptyMap())

        override fun observeMovements(): Flow<List<Movement>> = MutableStateFlow(movements.value.values.toList())
        override suspend fun getById(id: String): Movement? = movements.value[id]
        override suspend fun findByCounterpartyName(counterpartyName: String): List<Movement> {
            return movements.value.values.filter { it.counterpartyName == counterpartyName }
        }
        override suspend fun save(movement: Movement): Result<Unit> {
            movements.value = movements.value + (movement.id to movement)
            return Result.success(Unit)
        }
        override suspend fun delete(id: String): Result<Unit> {
            movements.value = movements.value - id
            return Result.success(Unit)
        }
    }
}
