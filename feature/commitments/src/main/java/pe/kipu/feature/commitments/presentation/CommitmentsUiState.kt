package pe.kipu.feature.commitments.presentation

import pe.kipu.core.domain.model.CommitmentsInsights
import pe.kipu.feature.commitments.ui.CommitmentFormState

sealed interface CommitmentsUiState {
    data object Loading : CommitmentsUiState

    data class Content(
        val insights: CommitmentsInsights,
        val showFormDialog: Boolean = false,
        val formState: CommitmentFormState = CommitmentFormState(),
        val deleteTargetId: String? = null,
        val deleteTargetTitle: String? = null,
    ) : CommitmentsUiState

    data class Error(val message: String) : CommitmentsUiState
}
