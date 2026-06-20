package pe.kipu.feature.receipts.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pe.kipu.core.data.usecase.ProcessReceiptFromUriUseCase
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.ConfirmMovementResult
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.DuplicateResolution
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.ReceiptParseResult
import pe.kipu.core.domain.model.SuggestedMovement
import pe.kipu.core.domain.model.SuggestionConfidence
import pe.kipu.core.domain.ocr.ReceiptImageLoader
import pe.kipu.core.domain.repository.CategoryRepository
import pe.kipu.core.domain.time.TimeProvider
import pe.kipu.core.domain.usecase.ConfirmReceiptMovementUseCase
import pe.kipu.core.domain.util.MoneyInputParser
import pe.kipu.core.designsystem.component.formatPenAmountForDisplay
import pe.kipu.feature.receipts.navigation.ReceiptRoutes

@HiltViewModel
class ReceiptReviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val receiptImageLoader: ReceiptImageLoader,
    private val processReceiptFromUri: ProcessReceiptFromUriUseCase,
    private val confirmReceiptMovement: ConfirmReceiptMovementUseCase,
    private val categoryRepository: CategoryRepository,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val contentUri: String = ReceiptRoutes.decodeContentUri(
        checkNotNull(savedStateHandle.get<String>(ReceiptRoutes.CONTENT_URI_ARG)) {
            "Receipt content URI is required"
        },
    )

    private val _uiState = MutableStateFlow<ReceiptReviewUiState>(ReceiptReviewUiState.Loading)
    val uiState: StateFlow<ReceiptReviewUiState> = _uiState.asStateFlow()

    private var baseSuggestion: SuggestedMovement? = null
    private var previewBytes: ByteArray = byteArrayOf()
    private var parseWarning: String? = null
    private var confidence: SuggestionConfidence? = null
    private var amountText: String = ""
    private var counterpartyText: String = ""
    private var messageText: String = ""
    private var operationReferenceText: String = ""
    private var selectedCategoryId: String = CategoryIds.OTHER
    private var categorySuggestionReason: String? = null
    private var channel: PaymentChannel = PaymentChannel.OTHER
    private var suggestedRecordedAt: Instant? = null
    private var categories: List<pe.kipu.core.domain.model.Category> = emptyList()

    init {
        viewModelScope.launch {
            loadAndProcess()
        }
    }

    fun onAmountChanged(value: String) {
        amountText = value
        publishReady(isSaving = false, errorMessage = null, duplicatePending = null)
    }

    fun onCounterpartyChanged(value: String) {
        counterpartyText = value
        publishReady(isSaving = false, errorMessage = null, duplicatePending = null)
    }

    fun onMessageChanged(value: String) {
        messageText = value
        publishReady(isSaving = false, errorMessage = null, duplicatePending = null)
    }

    fun onOperationReferenceChanged(value: String) {
        operationReferenceText = value
        publishReady(isSaving = false, errorMessage = null, duplicatePending = null)
    }

    fun onCategorySelected(categoryId: String) {
        selectedCategoryId = categoryId
        if (categoryId != baseSuggestion?.categoryId) {
            categorySuggestionReason = null
        }
        publishReady(isSaving = false, errorMessage = null, duplicatePending = null)
    }

    fun onConfirm() {
        viewModelScope.launch {
            publishReady(isSaving = true, errorMessage = null, duplicatePending = null)
            when (val buildResult = buildSuggestion()) {
                is DomainResult.Err -> {
                    publishReady(
                        isSaving = false,
                        errorMessage = buildResult.error.message,
                        duplicatePending = null,
                    )
                }

                is DomainResult.Ok -> confirmSuggestion(buildResult.value)
            }
        }
    }

    fun onResolveDuplicate(resolution: DuplicateResolution) {
        if (resolution == DuplicateResolution.CANCEL) {
            publishReady(isSaving = false, errorMessage = null, duplicatePending = null)
            return
        }
        viewModelScope.launch {
            publishReady(isSaving = true, errorMessage = null, duplicatePending = null)
            when (val buildResult = buildSuggestion()) {
                is DomainResult.Err -> {
                    publishReady(
                        isSaving = false,
                        errorMessage = buildResult.error.message,
                        duplicatePending = null,
                    )
                }

                is DomainResult.Ok -> confirmSuggestion(buildResult.value, resolution)
            }
        }
    }

    private suspend fun loadAndProcess() {
        val imageResult = receiptImageLoader.load(contentUri)
        val image = imageResult.getOrElse {
            _uiState.value = ReceiptReviewUiState.Error("No pudimos abrir el comprobante")
            return
        }

        previewBytes = image.bytes.copyOf()
        _uiState.value = ReceiptReviewUiState.Processing(previewBytes = previewBytes)

        val categories = categoryRepository.observeCategories().first()
        this.categories = categories
        val parseResult = processReceiptFromUri(contentUri)

        when (parseResult) {
            is ReceiptParseResult.Success -> applySuggestion(parseResult.suggestion, categories, warning = null)
            ReceiptParseResult.UnsupportedChannel -> applySuggestion(
                suggestion = null,
                categories = categories,
                warning = "No reconocimos Yape ni Plin. Completa los datos manualmente.",
            )

            is ReceiptParseResult.Failure -> applySuggestion(
                suggestion = null,
                categories = categories,
                warning = "No pudimos leer el comprobante. Revisa los campos antes de guardar.",
            )
        }
    }

    private fun applySuggestion(
        suggestion: SuggestedMovement?,
        categories: List<pe.kipu.core.domain.model.Category>,
        warning: String?,
    ) {
        baseSuggestion = suggestion
        parseWarning = warning
        confidence = suggestion?.confidence
        amountText = suggestion?.amount?.amount?.let(::formatPenAmountForDisplay) ?: ""
        counterpartyText = suggestion?.counterpartyName.orEmpty()
        messageText = suggestion?.message.orEmpty()
        operationReferenceText = suggestion?.operationReference.orEmpty()
        selectedCategoryId = suggestion?.categoryId ?: CategoryIds.OTHER
        categorySuggestionReason = suggestion?.categorySuggestionReason
        channel = suggestion?.channel ?: PaymentChannel.OTHER
        suggestedRecordedAt = suggestion?.suggestedRecordedAt

        _uiState.value = ReceiptReviewUiState.Ready(
            previewBytes = previewBytes,
            baseSuggestion = suggestion,
            parseWarning = parseWarning,
            confidence = confidence,
            amountText = amountText,
            counterpartyText = counterpartyText,
            messageText = messageText,
            operationReferenceText = operationReferenceText,
            selectedCategoryId = selectedCategoryId,
            categories = categories,
            categorySuggestionReason = categorySuggestionReason,
            channel = channel,
            isSaving = false,
            errorMessage = null,
            duplicatePending = null,
        )
    }

    private suspend fun confirmSuggestion(
        suggestion: SuggestedMovement,
        resolution: DuplicateResolution? = null,
    ) {
        val recordedAt = suggestedRecordedAt ?: timeProvider.now()
        when (
            val result = confirmReceiptMovement(
                suggestion = suggestion,
                categoryId = selectedCategoryId,
                recordedAt = recordedAt,
                resolution = resolution,
            )
        ) {
            is ConfirmMovementResult.Saved -> _uiState.value = ReceiptReviewUiState.Saved(result.movement)
            is ConfirmMovementResult.DuplicatePending -> publishReady(
                isSaving = false,
                errorMessage = null,
                duplicatePending = result,
            )

            ConfirmMovementResult.Cancelled -> when (resolution) {
                DuplicateResolution.MERGE -> _uiState.value = ReceiptReviewUiState.DuplicateMerged
                else -> publishReady(
                    isSaving = false,
                    errorMessage = null,
                    duplicatePending = null,
                )
            }
        }
    }

    private fun buildSuggestion(): DomainResult<SuggestedMovement> {
        val moneyResult = MoneyInputParser.parsePen(amountText)
        val money = when (moneyResult) {
            is DomainResult.Err -> return moneyResult
            is DomainResult.Ok -> moneyResult.value
        }

        val draftId = baseSuggestion?.draftId ?: "draft-receipt-${timeProvider.now().toEpochMilli()}"
        val suggestion = SuggestedMovement(
            draftId = draftId,
            source = MovementSource.RECEIPT,
            confidence = confidence ?: SuggestionConfidence.LOW,
            type = MovementType.EXPENSE,
            amount = money,
            categoryId = selectedCategoryId,
            categorySuggestionReason = categorySuggestionReason,
            channel = channel,
            counterpartyName = counterpartyText.trim().takeIf { it.isNotEmpty() },
            message = messageText.trim().takeIf { it.isNotEmpty() },
            operationReference = operationReferenceText.trim().takeIf { it.isNotEmpty() },
            suggestedRecordedAt = suggestedRecordedAt,
        )
        return suggestion.validate().let { validation ->
            when (validation) {
                is DomainResult.Err -> validation
                is DomainResult.Ok -> DomainResult.Ok(suggestion)
            }
        }
    }

    private fun publishReady(
        isSaving: Boolean,
        errorMessage: String?,
        duplicatePending: ConfirmMovementResult.DuplicatePending?,
    ) {
        val current = _uiState.value
        if (current !is ReceiptReviewUiState.Ready && current !is ReceiptReviewUiState.Processing) return

        _uiState.value = ReceiptReviewUiState.Ready(
            previewBytes = previewBytes,
            baseSuggestion = baseSuggestion,
            parseWarning = parseWarning,
            confidence = confidence,
            amountText = amountText,
            counterpartyText = counterpartyText,
            messageText = messageText,
            operationReferenceText = operationReferenceText,
            selectedCategoryId = selectedCategoryId,
            categories = categories,
            categorySuggestionReason = categorySuggestionReason,
            channel = channel,
            isSaving = isSaving,
            errorMessage = errorMessage,
            duplicatePending = duplicatePending,
        )
    }
}
