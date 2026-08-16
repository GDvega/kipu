package pe.kipu.feature.movements.presentation

import pe.kipu.core.domain.model.Category
import pe.kipu.core.domain.model.Commitment
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementDuplicatePair
import pe.kipu.feature.movements.ui.ManualMovementFormState

sealed interface MovementsUiState {
    data object Loading : MovementsUiState

    data class Content(
        val movements: List<Movement>,
        val categories: List<Category> = emptyList(),
        val categoryNamesById: Map<String, String> = emptyMap(),
        val selectedFilter: MovementChannelFilter = MovementChannelFilter.ALL,
        val categoryFilterId: String? = null,
        val categoryFilterName: String? = null,
        val pendingNotificationIncomes: List<Movement> = emptyList(),
        val duplicatePairs: List<MovementDuplicatePair> = emptyList(),
        val pendingResolution: MovementDuplicatePair? = null,
        val pendingNotificationConfirm: PendingNotificationConfirmState? = null,
        val categoryChangeTarget: Movement? = null,
        val goalLinkTarget: Movement? = null,
        val savingsGoals: List<Commitment> = emptyList(),
        val manualMovementForm: ManualMovementFormState? = null,
        val isActionInProgress: Boolean = false,
    ) : MovementsUiState {
        val filteredMovements: List<Movement> = movements
            .filter { it.matchesChannelFilter(selectedFilter) }
            .filter { it.matchesCategoryFilter(categoryFilterId) }
    }

    data class Error(val message: String) : MovementsUiState
}

data class PendingNotificationConfirmState(
    val movementId: String,
    val duplicateMatches: List<Movement>,
)

sealed interface MovementsEvent {
    data class ShowSnackbar(val message: String) : MovementsEvent
}
