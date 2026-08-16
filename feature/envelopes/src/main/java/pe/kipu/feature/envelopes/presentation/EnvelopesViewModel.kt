package pe.kipu.feature.envelopes.presentation

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
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.EnvelopeBudgetState
import pe.kipu.core.domain.repository.CategoryRepository
import pe.kipu.core.domain.repository.FinancialPlanRepository
import pe.kipu.core.domain.repository.MovementRepository
import pe.kipu.core.domain.usecase.CalculateWeeklyEnvelopeBalanceUseCase
import pe.kipu.core.domain.usecase.CreateEnvelopeUseCase
import pe.kipu.core.domain.usecase.DeleteEnvelopeUseCase
import pe.kipu.core.domain.usecase.GetEnvelopeRecentMovementsUseCase
import pe.kipu.core.domain.usecase.ObserveEnvelopeBudgetsUseCase
import pe.kipu.core.domain.usecase.UpdateEnvelopeWeeklyLimitUseCase
import pe.kipu.core.domain.util.MoneyInputParser
import pe.kipu.feature.envelopes.ui.EnvelopeCreateFormState

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EnvelopesViewModel @Inject constructor(
    private val observeEnvelopeBudgets: ObserveEnvelopeBudgetsUseCase,
    private val categoryRepository: CategoryRepository,
    private val movementRepository: MovementRepository,
    private val financialPlanRepository: FinancialPlanRepository,
    private val calculateWeeklyEnvelopeBalance: CalculateWeeklyEnvelopeBalanceUseCase,
    private val getEnvelopeRecentMovements: GetEnvelopeRecentMovementsUseCase,
    private val updateEnvelopeWeeklyLimit: UpdateEnvelopeWeeklyLimitUseCase,
    private val createEnvelope: CreateEnvelopeUseCase,
    private val deleteEnvelope: DeleteEnvelopeUseCase,
) : ViewModel() {

    private val adjustTarget = MutableStateFlow<EnvelopeBudgetState?>(null)
    private val showCreateDialog = MutableStateFlow(false)
    private val createForm = MutableStateFlow(EnvelopeCreateFormState())
    private val deleteState = MutableStateFlow(EnvelopeDeleteState())
    private val adjustState = MutableStateFlow(EnvelopeAdjustState())

    private val _uiState = MutableStateFlow<EnvelopesUiState>(EnvelopesUiState.Loading)
    val uiState: StateFlow<EnvelopesUiState> = _uiState.asStateFlow()

    private val reloadRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init {
        viewModelScope.launch {
            reloadRequests
                .onStart { emit(Unit) }
                .flatMapLatest { observeEnvelopesState() }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun retryLoad() {
        reloadRequests.tryEmit(Unit)
    }

    private fun observeEnvelopesState(): Flow<EnvelopesUiState> {
        val dataFlow = combine(
            observeEnvelopeBudgets(),
            categoryRepository.observeCategories(),
            movementRepository.observeMovements(),
            financialPlanRepository.observePlans(),
        ) { budgets, categories, movements, plans ->
            Quadruple(budgets, categories, movements, plans.firstOrNull())
        }

        val contentFlow = combine(
            dataFlow,
            adjustTarget,
            showCreateDialog,
            createForm,
            deleteState,
        ) { (budgets, categories, movements, plan), adjust, creating, form, deleting ->
            EnvelopesUiState.Content(
                budgets = budgets.map { budget ->
                    EnvelopeBudgetUiModel(
                        budget = budget,
                        recentMovements = getEnvelopeRecentMovements(
                            categoryId = budget.categoryId,
                            movements = movements,
                            cycle = plan?.budgetCycle ?: pe.kipu.core.domain.model.BudgetCycle.WEEKLY,
                        ),
                    )
                },
                categories = categories,
                usedCategoryIds = budgets.map { it.categoryId }.toSet(),
                planBalance = calculateWeeklyEnvelopeBalance(plan, budgets),
                adjustTarget = adjust,
                showCreateDialog = creating,
                createForm = form,
                deleteTarget = deleting.target,
                isDeleting = deleting.isDeleting,
                deleteErrorMessage = deleting.errorMessage,
                budgetCycle = plan?.budgetCycle ?: pe.kipu.core.domain.model.BudgetCycle.WEEKLY,
            )
        }

        return combine(contentFlow, adjustState) { content, adjusting ->
            content.copy(
                isAdjustingLimit = adjusting.isSaving,
                adjustLimitError = adjusting.errorMessage,
            )
        }.map<EnvelopesUiState.Content, EnvelopesUiState> { it }
            .catch {
                emit(EnvelopesUiState.Error("No pudimos cargar tus sobres"))
            }
    }

    fun onAdjustClick(budget: EnvelopeBudgetState) {
        adjustTarget.value = budget
        adjustState.value = EnvelopeAdjustState()
    }

    fun onDismissAdjust() {
        if (adjustState.value.isSaving) return
        adjustTarget.value = null
        adjustState.value = EnvelopeAdjustState()
    }

    fun onSaveWeeklyLimit(amountText: String) {
        val envelope = adjustTarget.value ?: return
        if (adjustState.value.isSaving) return
        when (val parsed = MoneyInputParser.parsePen(amountText)) {
            is DomainResult.Err -> {
                adjustState.value = EnvelopeAdjustState(errorMessage = "Ingresa un monto válido")
            }

            is DomainResult.Ok -> {
                adjustState.value = EnvelopeAdjustState(isSaving = true)
                viewModelScope.launch {
                    try {
                        updateEnvelopeWeeklyLimit(envelope.envelopeId, parsed.value)
                            .onSuccess {
                                adjustTarget.value = null
                                adjustState.value = EnvelopeAdjustState()
                            }
                            .onFailure { error ->
                                if (error is CancellationException) throw error
                                showAdjustError()
                            }
                    } catch (error: CancellationException) {
                        adjustState.update { it.copy(isSaving = false) }
                        throw error
                    } catch (_: Exception) {
                        showAdjustError()
                    }
                }
            }
        }
    }

    fun onCreateClick() {
        showCreateDialog.value = true
        createForm.value = EnvelopeCreateFormState()
    }

    fun onDismissCreate() {
        if (createForm.value.isSaving) return
        showCreateDialog.value = false
        createForm.value = EnvelopeCreateFormState()
    }

    fun onCreateNameChanged(value: String) {
        if (createForm.value.isSaving) return
        createForm.update { it.copy(name = value, errorMessage = null) }
    }

    fun onCreateCategorySelected(index: Int) {
        if (createForm.value.isSaving) return
        createForm.update { it.copy(selectedCategoryIndex = index, errorMessage = null) }
    }

    fun onCreateAmountChanged(value: String) {
        if (createForm.value.isSaving) return
        createForm.update { it.copy(amountText = value, errorMessage = null) }
    }

    fun onConfirmCreate() {
        val state = (_uiState.value as? EnvelopesUiState.Content) ?: return
        val form = createForm.value
        if (form.isSaving) return
        val available = state.categories.filter { it.id !in state.usedCategoryIds }
        val category = available.getOrNull(form.selectedCategoryIndex) ?: return

        createForm.value = form.copy(isSaving = true, errorMessage = null)
        viewModelScope.launch {
            try {
                when (val parsed = MoneyInputParser.parsePen(form.amountText)) {
                    is DomainResult.Err -> {
                        createForm.update {
                            it.copy(isSaving = false, errorMessage = "Ingresa un monto válido")
                        }
                    }

                    is DomainResult.Ok -> {
                        createEnvelope(
                            name = form.name,
                            categoryId = category.id,
                            weeklyLimit = parsed.value,
                        )
                            .onSuccess {
                                showCreateDialog.value = false
                                createForm.value = EnvelopeCreateFormState()
                            }
                            .onFailure { error ->
                                if (error is CancellationException) throw error
                                showCreateError()
                            }
                    }
                }
            } catch (error: CancellationException) {
                createForm.update { it.copy(isSaving = false) }
                throw error
            } catch (_: Exception) {
                showCreateError()
            }
        }
    }

    fun onDeleteClick(budget: EnvelopeBudgetState) {
        deleteState.value = EnvelopeDeleteState(target = budget)
    }

    fun onDismissDelete() {
        if (deleteState.value.isDeleting) return
        deleteState.value = EnvelopeDeleteState()
    }

    fun onConfirmDelete() {
        val state = deleteState.value
        val target = state.target ?: return
        if (state.isDeleting) return
        deleteState.value = state.copy(isDeleting = true, errorMessage = null)
        viewModelScope.launch {
            try {
                deleteEnvelope(target.envelopeId)
                    .onSuccess {
                        deleteState.value = EnvelopeDeleteState()
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

    private fun showAdjustError() {
        adjustState.value = EnvelopeAdjustState(
            errorMessage = "No pudimos actualizar el límite",
        )
    }

    private fun showCreateError() {
        createForm.update {
            it.copy(
                isSaving = false,
                errorMessage = "No pudimos crear el sobre. Revisa los datos.",
            )
        }
    }

    private fun showDeleteError() {
        deleteState.update {
            it.copy(
                isDeleting = false,
                errorMessage = "No pudimos eliminar el sobre",
            )
        }
    }
}

private data class EnvelopeDeleteState(
    val target: EnvelopeBudgetState? = null,
    val isDeleting: Boolean = false,
    val errorMessage: String? = null,
)

private data class EnvelopeAdjustState(
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
)
