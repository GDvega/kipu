package pe.kipu.feature.movements.presentation

import pe.kipu.core.domain.model.Category
import pe.kipu.core.domain.model.Commitment
import pe.kipu.core.domain.model.Envelope
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementAuditAction
import pe.kipu.core.domain.model.MovementAuditEntry
import pe.kipu.core.domain.model.MovementDuplicatePair
import pe.kipu.core.domain.model.UnexpectedExpensePreview
import pe.kipu.core.domain.model.UnexpectedExpenseRecoveryPlan
import pe.kipu.feature.movements.ui.EditMovementFormState
import pe.kipu.feature.movements.ui.ManualMovementFormState

enum class MovementsTab(val label: String) {
    ACTIVE("Movimientos"),
    AUDIT("Historial y Auditoría"),
}

enum class MovementAuditFilter(val label: String) {
    ALL("Todos"),
    CREATED("Registrados"),
    UPDATED("Editados"),
    DELETED("Eliminados"),
}

sealed interface MovementsUiState {
    data object Loading : MovementsUiState

    data class Content(
        val movements: List<Movement>,
        val categories: List<Category> = emptyList(),
        val envelopes: List<Envelope> = emptyList(),
        val categoryNamesById: Map<String, String> = emptyMap(),
        val selectedTab: MovementsTab = MovementsTab.ACTIVE,
        val selectedFilter: MovementChannelFilter = MovementChannelFilter.ALL,
        val categoryFilterId: String? = null,
        val categoryFilterName: String? = null,
        val auditLogs: List<MovementAuditEntry> = emptyList(),
        val selectedAuditFilter: MovementAuditFilter = MovementAuditFilter.ALL,
        val pendingNotificationIncomes: List<Movement> = emptyList(),
        val duplicatePairs: List<MovementDuplicatePair> = emptyList(),
        val pendingResolution: MovementDuplicatePair? = null,
        val pendingNotificationConfirm: PendingNotificationConfirmState? = null,
        val categoryChangeTarget: Movement? = null,
        val goalLinkTarget: Movement? = null,
        val savingsGoals: List<Commitment> = emptyList(),
        val manualMovementForm: ManualMovementFormState? = null,
        val unexpectedExpenseConfirmation: UnexpectedExpenseConfirmationState? = null,
        val editMovementForm: EditMovementFormState? = null,
        val movementToDelete: Movement? = null,
        val isActionInProgress: Boolean = false,
    ) : MovementsUiState {
        val filteredMovements: List<Movement> = movements
            .filter { it.matchesChannelFilter(selectedFilter) }
            .filter { it.matchesCategoryFilter(categoryFilterId) }

        val filteredAuditLogs: List<MovementAuditEntry> = auditLogs
            .filter { log ->
                when (selectedAuditFilter) {
                    MovementAuditFilter.ALL -> true
                    MovementAuditFilter.CREATED -> log.action == MovementAuditAction.CREATED
                    MovementAuditFilter.UPDATED -> log.action == MovementAuditAction.UPDATED
                    MovementAuditFilter.DELETED -> log.action == MovementAuditAction.DELETED
                }
            }
    }

    data class Error(val message: String) : MovementsUiState
}

data class UnexpectedExpenseConfirmationState(
    val form: ManualMovementFormState,
    val preview: UnexpectedExpensePreview,
    val selectedEnvelopeIds: Set<String> = preview.recoveryPlan.adjustments
        .mapTo(linkedSetOf()) { it.envelopeId },
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
) {
    val selectedRecoveryPlan: UnexpectedExpenseRecoveryPlan
        get() = preview.recoveryPlanFor(selectedEnvelopeIds)
}

data class PendingNotificationConfirmState(
    val movementId: String,
    val duplicateMatches: List<Movement>,
)

sealed interface MovementsEvent {
    data class ShowSnackbar(val message: String) : MovementsEvent
}
