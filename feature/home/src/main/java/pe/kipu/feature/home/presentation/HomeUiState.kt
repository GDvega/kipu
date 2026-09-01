package pe.kipu.feature.home.presentation

import pe.kipu.core.domain.model.Category
import pe.kipu.core.domain.model.Envelope
import pe.kipu.core.domain.model.HomeInsights
import pe.kipu.core.domain.receipt.MonthlyServiceReceipt
import pe.kipu.core.domain.model.UnexpectedExpensePreview
import pe.kipu.core.domain.voice.VoiceFinancialIntent

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Content(
        val insights: HomeInsights,
        val categoryNamesById: Map<String, String> = emptyMap(),
        val userCategories: List<Category> = emptyList(),
        val envelopes: List<Envelope> = emptyList(),
        val monthlyReceipts: List<MonthlyServiceReceipt> = emptyList(),
    ) : HomeUiState

    data class Error(val message: String) : HomeUiState
}

data class VoiceUnexpectedExpenseState(
    val intent: VoiceFinancialIntent.Expense,
    val preview: UnexpectedExpensePreview,
    val selectedEnvelopeIds: Set<String> = preview.recoveryPlan.adjustments
        .mapTo(linkedSetOf()) { it.envelopeId },
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
) {
    val selectedRecoveryPlan get() = preview.recoveryPlanFor(selectedEnvelopeIds)
}
