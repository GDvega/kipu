package pe.kipu.feature.juntas.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.GatheringSummary
import pe.kipu.core.domain.usecase.DeleteGatheringUseCase
import pe.kipu.core.domain.usecase.LinkMovementToGatheringUseCase
import pe.kipu.core.domain.usecase.ObserveGatheringSummariesUseCase
import pe.kipu.core.domain.usecase.RecordGatheringExpenseUseCase
import pe.kipu.core.domain.usecase.SaveGatheringUseCase
import pe.kipu.core.domain.usecase.UpdateGatheringUseCase

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GatheringsViewModel @Inject constructor(
    private val observeGatheringSummaries: ObserveGatheringSummariesUseCase,
    private val saveGathering: SaveGatheringUseCase,
    private val updateGathering: UpdateGatheringUseCase,
    private val deleteGathering: DeleteGatheringUseCase,
    private val recordGatheringExpense: RecordGatheringExpenseUseCase,
    private val linkMovementToGathering: LinkMovementToGatheringUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<GatheringsUiState>(GatheringsUiState.Loading)
    val uiState: StateFlow<GatheringsUiState> = _uiState.asStateFlow()

    private val reloadRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init {
        viewModelScope.launch {
            reloadRequests
                .onStart { emit(Unit) }
                .flatMapLatest {
                    observeGatheringSummaries()
                        .map { dashboard ->
                            val current = _uiState.value
                            when (current) {
                                is GatheringsUiState.Content -> current.copy(
                                    summaries = dashboard.summaries,
                                    unlinkedMovements = dashboard.unlinkedMovements,
                                )

                                else -> GatheringsUiState.Content(
                                    summaries = dashboard.summaries,
                                    unlinkedMovements = dashboard.unlinkedMovements,
                                    dialogMode = null,
                                    formName = "",
                                    formParticipants = "",
                                    formAmount = "",
                                    formPaidBy = "",
                                    formDescription = "",
                                    formMovementId = null,
                                    formError = null,
                                    isSaving = false,
                                )
                            }
                        }
                        .map<GatheringsUiState.Content, GatheringsUiState> { it }
                        .catch {
                            emit(GatheringsUiState.Error("No pudimos cargar tus cuentas compartidas"))
                        }
                }.collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun retryLoad() {
        reloadRequests.tryEmit(Unit)
    }

    fun onCreateClick() = openFormDialog(GatheringDialogMode.Create)

    fun onEditClick(summary: GatheringSummary) {
        updateContent {
            it.copy(
                dialogMode = GatheringDialogMode.Edit(summary.gathering.id),
                formName = summary.gathering.name,
                formParticipants = summary.gathering.participantNames.joinToString("\n"),
                formAmount = "",
                formPaidBy = summary.gathering.participantNames.firstOrNull().orEmpty(),
                formDescription = "",
                formMovementId = null,
                formError = null,
            )
        }
    }

    fun onRecordExpenseClick(gatheringId: String) {
        openFormDialog(
            GatheringDialogMode.RecordExpense(gatheringId),
            formPaidBy = currentParticipants(gatheringId).firstOrNull().orEmpty(),
        )
    }

    fun onLinkMovementClick(gatheringId: String) {
        openFormDialog(
            GatheringDialogMode.LinkMovement(gatheringId),
            formPaidBy = currentParticipants(gatheringId).firstOrNull().orEmpty(),
        )
    }

    fun onDismissDialog() {
        updateContent {
            it.copy(
                dialogMode = null,
                formName = "",
                formParticipants = "",
                formAmount = "",
                formPaidBy = "",
                formDescription = "",
                formMovementId = null,
                formError = null,
                isSaving = false,
            )
        }
    }

    fun onFormNameChanged(value: String) = updateContent { it.copy(formName = value, formError = null) }

    fun onFormParticipantsChanged(value: String) =
        updateContent { it.copy(formParticipants = value, formError = null) }

    fun onFormAmountChanged(value: String) = updateContent { it.copy(formAmount = value, formError = null) }

    fun onFormPaidByChanged(value: String) = updateContent { it.copy(formPaidBy = value, formError = null) }

    fun onFormDescriptionChanged(value: String) =
        updateContent { it.copy(formDescription = value, formError = null) }

    fun onFormMovementSelected(movementId: String) =
        updateContent { it.copy(formMovementId = movementId, formError = null) }

    fun onConfirmDialog() {
        val state = _uiState.value as? GatheringsUiState.Content ?: return
        when (val mode = state.dialogMode) {
            GatheringDialogMode.Create -> confirmCreate(state)
            is GatheringDialogMode.Edit -> confirmEdit(state, mode.gatheringId)
            is GatheringDialogMode.RecordExpense -> confirmRecordExpense(state, mode.gatheringId)
            is GatheringDialogMode.LinkMovement -> confirmLinkMovement(state, mode.gatheringId)
            null -> Unit
        }
    }

    fun onDeleteGathering(id: String) {
        viewModelScope.launch {
            deleteGathering(id)
        }
    }

    private fun openFormDialog(mode: GatheringDialogMode, formPaidBy: String = "") {
        updateContent {
            it.copy(
                dialogMode = mode,
                formName = "",
                formParticipants = "",
                formAmount = "",
                formPaidBy = formPaidBy,
                formDescription = "",
                formMovementId = null,
                formError = null,
            )
        }
    }

    private fun currentParticipants(gatheringId: String): List<String> {
        val state = _uiState.value as? GatheringsUiState.Content ?: return emptyList()
        return state.summaries.firstOrNull { it.gathering.id == gatheringId }
            ?.gathering
            ?.participantNames
            .orEmpty()
    }

    private fun confirmCreate(state: GatheringsUiState.Content) {
        viewModelScope.launch {
            updateContent { it.copy(isSaving = true, formError = null) }
            when (
                val result = saveGathering(
                    name = state.formName,
                    participantsInput = state.formParticipants,
                )
            ) {
                is DomainResult.Ok -> onDismissDialog()
                is DomainResult.Err -> updateContent {
                    it.copy(isSaving = false, formError = result.error.message)
                }
            }
        }
    }

    private fun confirmEdit(state: GatheringsUiState.Content, gatheringId: String) {
        viewModelScope.launch {
            updateContent { it.copy(isSaving = true, formError = null) }
            when (
                val result = updateGathering(
                    id = gatheringId,
                    name = state.formName,
                    participantsInput = state.formParticipants,
                )
            ) {
                is DomainResult.Ok -> onDismissDialog()
                is DomainResult.Err -> updateContent {
                    it.copy(isSaving = false, formError = result.error.message)
                }
            }
        }
    }

    private fun confirmRecordExpense(state: GatheringsUiState.Content, gatheringId: String) {
        viewModelScope.launch {
            updateContent { it.copy(isSaving = true, formError = null) }
            when (
                val result = recordGatheringExpense(
                    gatheringId = gatheringId,
                    amountInput = state.formAmount,
                    paidByParticipant = state.formPaidBy,
                    description = state.formDescription,
                )
            ) {
                is DomainResult.Ok -> onDismissDialog()
                is DomainResult.Err -> updateContent {
                    it.copy(isSaving = false, formError = result.error.message)
                }
            }
        }
    }

    private fun confirmLinkMovement(state: GatheringsUiState.Content, gatheringId: String) {
        val movementId = state.formMovementId ?: run {
            updateContent { it.copy(formError = "Selecciona un movimiento") }
            return
        }
        viewModelScope.launch {
            updateContent { it.copy(isSaving = true, formError = null) }
            when (
                val result = linkMovementToGathering(
                    gatheringId = gatheringId,
                    movementId = movementId,
                    paidByParticipant = state.formPaidBy,
                )
            ) {
                is DomainResult.Ok -> onDismissDialog()
                is DomainResult.Err -> updateContent {
                    it.copy(isSaving = false, formError = result.error.message)
                }
            }
        }
    }

    private fun updateContent(transform: (GatheringsUiState.Content) -> GatheringsUiState.Content) {
        val current = _uiState.value
        if (current is GatheringsUiState.Content) {
            _uiState.value = transform(current)
        }
    }
}
