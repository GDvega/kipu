package pe.kipu.feature.receipts.presentation

import pe.kipu.core.domain.model.Category
import pe.kipu.core.domain.model.ConfirmMovementResult
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.SuggestedMovement
import pe.kipu.core.domain.model.SuggestionConfidence

sealed interface ReceiptReviewUiState {
    data object Loading : ReceiptReviewUiState

    data class Processing(
        val previewBytes: ByteArray,
    ) : ReceiptReviewUiState

    data class Ready(
        val previewBytes: ByteArray,
        val baseSuggestion: SuggestedMovement?,
        val parseWarning: String?,
        val confidence: SuggestionConfidence?,
        val amountText: String,
        val counterpartyText: String,
        val messageText: String,
        val operationReferenceText: String,
        val selectedCategoryId: String,
        val categories: List<Category>,
        val categorySuggestionReason: String?,
        val channel: PaymentChannel,
        val isSaving: Boolean,
        val errorMessage: String?,
        val duplicatePending: ConfirmMovementResult.DuplicatePending?,
    ) : ReceiptReviewUiState

    data class Saved(
        val movement: Movement,
    ) : ReceiptReviewUiState

    /** User chose MERGE — duplicate acknowledged, no new movement saved. */
    data object DuplicateMerged : ReceiptReviewUiState

    data class Error(
        val message: String,
    ) : ReceiptReviewUiState
}
