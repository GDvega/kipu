package pe.kipu.feature.commitments.presentation

import pe.kipu.core.domain.model.CommitmentsInsights
import pe.kipu.feature.commitments.ui.CommitmentFormState

data class SavingsContributionState(
    val commitmentId: String? = null,
    val commitmentTitle: String = "",
    val currentAmount: pe.kipu.core.domain.model.Money = pe.kipu.core.domain.model.Money.ZERO,
    val targetAmount: pe.kipu.core.domain.model.Money? = null,
    val isDeposit: Boolean = true,
    val amountText: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface CommitmentsUiState {
    data object Loading : CommitmentsUiState

    data class Content(
        val insights: CommitmentsInsights,
        val showFormDialog: Boolean = false,
        val formState: CommitmentFormState = CommitmentFormState(),
        val showContributionDialog: Boolean = false,
        val contributionState: SavingsContributionState = SavingsContributionState(),
        val deleteTargetId: String? = null,
        val deleteTargetTitle: String? = null,
        val isDeleting: Boolean = false,
        val deleteErrorMessage: String? = null,
    ) : CommitmentsUiState {
        val canConfirmDelete: Boolean
            get() = deleteTargetId != null && !isDeleting
    }

    data class Error(val message: String) : CommitmentsUiState
}

