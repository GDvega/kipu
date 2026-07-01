package pe.kipu.feature.commitments.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.kipu.core.domain.model.CommitmentSummary
import pe.kipu.core.domain.model.CommitmentType
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.usecase.DeleteCommitmentUseCase
import pe.kipu.core.domain.usecase.ObserveCommitmentsInsightsUseCase
import pe.kipu.core.domain.usecase.SaveCommitmentUseCase
import pe.kipu.core.domain.util.MoneyInputParser
import pe.kipu.feature.commitments.ui.CommitmentFormState

@HiltViewModel
class CommitmentsViewModel @Inject constructor(
    private val observeCommitmentsInsights: ObserveCommitmentsInsightsUseCase,
    private val saveCommitment: SaveCommitmentUseCase,
    private val deleteCommitment: DeleteCommitmentUseCase,
) : ViewModel() {

    private val showFormDialog = MutableStateFlow(false)
    private val formState = MutableStateFlow(CommitmentFormState())
    private val deleteTargetId = MutableStateFlow<String?>(null)
    private val deleteTargetTitle = MutableStateFlow<String?>(null)

    private val _uiState = MutableStateFlow<CommitmentsUiState>(CommitmentsUiState.Loading)
    val uiState: StateFlow<CommitmentsUiState> = _uiState.asStateFlow()

    private val reloadRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init {
        viewModelScope.launch {
            reloadRequests
                .onStart { emit(Unit) }
                .flatMapLatest { observeCommitmentsState() }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun retryLoad() {
        reloadRequests.tryEmit(Unit)
    }

    private fun observeCommitmentsState(): Flow<CommitmentsUiState> =
        combine(
            observeCommitmentsInsights(),
            showFormDialog,
            formState,
            deleteTargetId,
            deleteTargetTitle,
        ) { insights, showForm, form, deleteId, deleteTitle ->
            CommitmentsUiState.Content(
                insights = insights,
                showFormDialog = showForm,
                formState = form,
                deleteTargetId = deleteId,
                deleteTargetTitle = deleteTitle,
            )
        }.map<CommitmentsUiState.Content, CommitmentsUiState> { it }
            .catch {
                emit(CommitmentsUiState.Error("No pudimos cargar los compromisos"))
            }

    fun onCreateClick() {
        showFormDialog.value = true
        formState.value = CommitmentFormState()
    }

    fun onEditClick(summary: CommitmentSummary) {
        val commitment = summary.commitment
        showFormDialog.value = true
        formState.value = CommitmentFormState(
            type = commitment.type,
            title = commitment.title,
            targetAmountText = commitment.targetAmount?.amount?.stripTrailingZeros()?.toPlainString().orEmpty(),
            currentAmountText = commitment.currentAmount?.amount?.stripTrailingZeros()?.toPlainString().orEmpty(),
            counterpartyName = commitment.counterpartyName.orEmpty(),
            editingCommitmentId = commitment.id,
        )
    }

    fun onDismissForm() {
        showFormDialog.value = false
        formState.value = CommitmentFormState()
    }

    fun onTypeSelected(type: CommitmentType) {
        formState.update { it.copy(type = type, errorMessage = null) }
    }

    fun onTitleChanged(value: String) {
        formState.update { it.copy(title = value, errorMessage = null) }
    }

    fun onTargetAmountChanged(value: String) {
        formState.update { it.copy(targetAmountText = value, errorMessage = null) }
    }

    fun onCurrentAmountChanged(value: String) {
        formState.update { it.copy(currentAmountText = value, errorMessage = null) }
    }

    fun onCounterpartyChanged(value: String) {
        formState.update { it.copy(counterpartyName = value, errorMessage = null) }
    }

    fun onConfirmForm() {
        val form = formState.value
        viewModelScope.launch {
            formState.update { it.copy(isSaving = true, errorMessage = null) }

            val targetAmount = form.targetAmountText.takeIf { it.isNotBlank() }?.let { text ->
                when (val parsed = MoneyInputParser.parsePen(text)) {
                    is DomainResult.Ok -> parsed.value
                    is DomainResult.Err -> {
                        formState.update {
                            it.copy(isSaving = false, errorMessage = "Meta inválida")
                        }
                        return@launch
                    }
                }
            }

            val currentAmount = form.currentAmountText.takeIf { it.isNotBlank() }?.let { text ->
                when (val parsed = MoneyInputParser.parsePen(text)) {
                    is DomainResult.Ok -> parsed.value
                    is DomainResult.Err -> {
                        formState.update {
                            it.copy(isSaving = false, errorMessage = "Monto inválido")
                        }
                        return@launch
                    }
                }
            }

            saveCommitment(
                existingId = form.editingCommitmentId,
                type = form.type,
                title = form.title,
                targetAmount = if (form.type == CommitmentType.SAVINGS_GOAL) targetAmount else null,
                currentAmount = when (form.type) {
                    CommitmentType.SAVINGS_GOAL -> currentAmount
                    CommitmentType.SOCIAL_DEBT,
                    CommitmentType.PENDING_PAYMENT,
                    -> currentAmount
                },
                counterpartyName = if (form.type == CommitmentType.SOCIAL_DEBT) form.counterpartyName else null,
            )
                .onSuccess {
                    showFormDialog.value = false
                    formState.value = CommitmentFormState()
                }
                .onFailure {
                    formState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = "No pudimos guardar. Revisa los campos.",
                        )
                    }
                }
        }
    }

    fun onDeleteClick(summary: CommitmentSummary) {
        deleteTargetId.value = summary.commitment.id
        deleteTargetTitle.value = summary.commitment.title
    }

    fun onDismissDelete() {
        deleteTargetId.value = null
        deleteTargetTitle.value = null
    }

    fun onConfirmDelete() {
        val id = deleteTargetId.value ?: return
        viewModelScope.launch {
            deleteCommitment(id)
            deleteTargetId.value = null
            deleteTargetTitle.value = null
        }
    }
}
