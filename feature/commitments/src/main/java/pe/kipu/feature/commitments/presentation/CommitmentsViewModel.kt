package pe.kipu.feature.commitments.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CommitmentsViewModel @Inject constructor(
    private val observeCommitmentsInsights: ObserveCommitmentsInsightsUseCase,
    private val saveCommitment: SaveCommitmentUseCase,
    private val deleteCommitment: DeleteCommitmentUseCase,
) : ViewModel() {

    private val showFormDialog = MutableStateFlow(false)
    private val formState = MutableStateFlow(CommitmentFormState())
    private val showContributionDialog = MutableStateFlow(false)
    private val contributionState = MutableStateFlow(SavingsContributionState())
    private val deleteState = MutableStateFlow(CommitmentDeleteState())

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
            combine(showFormDialog, formState) { show, form -> show to form },
            combine(showContributionDialog, contributionState) { show, state -> show to state },
            deleteState,
        ) { insights, formPair, contribPair, deleting ->
            CommitmentsUiState.Content(
                insights = insights,
                showFormDialog = formPair.first,
                formState = formPair.second,
                showContributionDialog = contribPair.first,
                contributionState = contribPair.second,
                deleteTargetId = deleting.targetId,
                deleteTargetTitle = deleting.targetTitle,
                isDeleting = deleting.isDeleting,
                deleteErrorMessage = deleting.errorMessage,
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
        if (formState.value.isSaving) return
        showFormDialog.value = false
        formState.value = CommitmentFormState()
    }

    fun onTypeSelected(type: CommitmentType) {
        if (formState.value.isSaving) return
        formState.update { it.copy(type = type, errorMessage = null) }
    }

    fun onTitleChanged(value: String) {
        if (formState.value.isSaving) return
        formState.update { it.copy(title = value, errorMessage = null) }
    }

    fun onTargetAmountChanged(value: String) {
        if (formState.value.isSaving) return
        formState.update { it.copy(targetAmountText = value, errorMessage = null) }
    }

    fun onCurrentAmountChanged(value: String) {
        if (formState.value.isSaving) return
        formState.update { it.copy(currentAmountText = value, errorMessage = null) }
    }

    fun onCounterpartyChanged(value: String) {
        if (formState.value.isSaving) return
        formState.update { it.copy(counterpartyName = value, errorMessage = null) }
    }

    fun onConfirmForm() {
        val form = formState.value
        if (form.isSaving) return
        formState.value = form.copy(isSaving = true, errorMessage = null)
        viewModelScope.launch {
            try {
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
                    .onFailure { error ->
                        if (error is CancellationException) throw error
                        showSaveError()
                    }
            } catch (error: CancellationException) {
                formState.update { it.copy(isSaving = false) }
                throw error
            } catch (_: Exception) {
                showSaveError()
            }
        }
    }

    fun onDeleteClick(summary: CommitmentSummary) {
        deleteState.value = CommitmentDeleteState(
            targetId = summary.commitment.id,
            targetTitle = summary.commitment.title,
        )
    }

    fun onDismissDelete() {
        if (deleteState.value.isDeleting) return
        deleteState.value = CommitmentDeleteState()
    }

    fun onConfirmDelete() {
        val state = deleteState.value
        val id = state.targetId ?: return
        if (state.isDeleting) return
        deleteState.value = state.copy(isDeleting = true, errorMessage = null)
        viewModelScope.launch {
            try {
                deleteCommitment(id)
                    .onSuccess {
                        deleteState.value = CommitmentDeleteState()
                    }
                    .onFailure { error ->
                        if (error is CancellationException) throw error
                        showDeleteError()
                    }
            } catch (error: CancellationException) {
                deleteState.update { it.copy(isDeleting = false) }
                throw error
            } catch (_: Exception) {
                showDeleteError()
            }
        }
    }

    fun onContributeClick(summary: CommitmentSummary) {
        val commitment = summary.commitment
        showContributionDialog.value = true
        contributionState.value = SavingsContributionState(
            commitmentId = commitment.id,
            commitmentTitle = commitment.title,
            currentAmount = commitment.currentAmount ?: pe.kipu.core.domain.model.Money.ZERO,
            targetAmount = commitment.targetAmount,
            isDeposit = true,
            amountText = "",
            isSaving = false,
            errorMessage = null,
        )
    }

    fun onContributionIsDepositChanged(isDeposit: Boolean) {
        if (contributionState.value.isSaving) return
        contributionState.update { it.copy(isDeposit = isDeposit, errorMessage = null) }
    }

    fun onContributionAmountChanged(amountText: String) {
        if (contributionState.value.isSaving) return
        contributionState.update { it.copy(amountText = amountText, errorMessage = null) }
    }

    fun onContributionPresetSelected(amount: java.math.BigDecimal) {
        if (contributionState.value.isSaving) return
        contributionState.update {
            it.copy(
                amountText = amount.stripTrailingZeros().toPlainString(),
                errorMessage = null,
            )
        }
    }

    fun onDismissContribution() {
        if (contributionState.value.isSaving) return
        showContributionDialog.value = false
        contributionState.value = SavingsContributionState()
    }

    fun onConfirmContribution() {
        val state = contributionState.value
        val commitmentId = state.commitmentId ?: return
        if (state.isSaving) return
        val amountDelta = when (val parsed = MoneyInputParser.parsePen(state.amountText)) {
            is DomainResult.Ok -> parsed.value
            is DomainResult.Err -> {
                contributionState.update { it.copy(errorMessage = "Monto inválido") }
                return
            }
        }
        if (amountDelta.isZero()) {
            contributionState.update { it.copy(errorMessage = "Ingresa un monto mayor a cero") }
            return
        }

        val newCurrentAmount = if (state.isDeposit) {
            state.currentAmount + amountDelta
        } else {
            when (val res = state.currentAmount - amountDelta) {
                is DomainResult.Ok -> res.value
                is DomainResult.Err -> pe.kipu.core.domain.model.Money.ZERO
            }
        }


        contributionState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                saveCommitment(
                    existingId = commitmentId,
                    type = CommitmentType.SAVINGS_GOAL,
                    title = state.commitmentTitle,
                    targetAmount = state.targetAmount,
                    currentAmount = newCurrentAmount,
                )
                    .onSuccess {
                        showContributionDialog.value = false
                        contributionState.value = SavingsContributionState()
                    }
                    .onFailure { error ->
                        if (error is CancellationException) throw error
                        contributionState.update {
                            it.copy(
                                isSaving = false,
                                errorMessage = "No pudimos actualizar el ahorro.",
                            )
                        }
                    }
            } catch (error: CancellationException) {
                contributionState.update { it.copy(isSaving = false) }
                throw error
            } catch (_: Exception) {
                contributionState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "No pudimos actualizar el ahorro.",
                    )
                }
            }
        }
    }

    private fun showSaveError() {
        formState.update {
            it.copy(
                isSaving = false,
                errorMessage = "No pudimos guardar. Revisa los campos.",
            )
        }
    }

    private fun showDeleteError() {
        deleteState.update {
            it.copy(
                isDeleting = false,
                errorMessage = "No pudimos eliminar el compromiso",
            )
        }
    }
}


private data class CommitmentDeleteState(
    val targetId: String? = null,
    val targetTitle: String? = null,
    val isDeleting: Boolean = false,
    val errorMessage: String? = null,
)
