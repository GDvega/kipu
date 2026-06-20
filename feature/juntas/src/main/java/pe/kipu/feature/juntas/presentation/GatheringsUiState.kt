package pe.kipu.feature.juntas.presentation

import pe.kipu.core.domain.model.GatheringSummary
import pe.kipu.core.domain.model.Movement

sealed interface GatheringsUiState {
    data object Loading : GatheringsUiState

    data class Content(
        val summaries: List<GatheringSummary>,
        val unlinkedMovements: List<Movement>,
        val dialogMode: GatheringDialogMode?,
        val formName: String,
        val formParticipants: String,
        val formAmount: String,
        val formPaidBy: String,
        val formDescription: String,
        val formMovementId: String?,
        val formError: String?,
        val isSaving: Boolean,
    ) : GatheringsUiState

    data class Error(val message: String) : GatheringsUiState
}

sealed interface GatheringDialogMode {
    data object Create : GatheringDialogMode

    data class Edit(val gatheringId: String) : GatheringDialogMode

    data class RecordExpense(val gatheringId: String) : GatheringDialogMode

    data class LinkMovement(val gatheringId: String) : GatheringDialogMode
}
