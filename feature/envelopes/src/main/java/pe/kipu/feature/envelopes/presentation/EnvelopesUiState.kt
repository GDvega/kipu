package pe.kipu.feature.envelopes.presentation

import pe.kipu.core.domain.model.Category
import pe.kipu.core.domain.model.EnvelopeBudgetState
import pe.kipu.core.domain.model.Movement
import pe.kipu.feature.envelopes.ui.EnvelopeCreateFormState

data class EnvelopeBudgetUiModel(
    val budget: EnvelopeBudgetState,
    val recentMovements: List<Movement>,
)

sealed interface EnvelopesUiState {
    data object Loading : EnvelopesUiState

    data class Content(
        val budgets: List<EnvelopeBudgetUiModel>,
        val categories: List<Category>,
        val usedCategoryIds: Set<String>,
        val adjustTarget: EnvelopeBudgetState? = null,
        val showCreateDialog: Boolean = false,
        val createForm: EnvelopeCreateFormState = EnvelopeCreateFormState(),
        val deleteTarget: EnvelopeBudgetState? = null,
    ) : EnvelopesUiState

    data class Error(val message: String) : EnvelopesUiState
}
