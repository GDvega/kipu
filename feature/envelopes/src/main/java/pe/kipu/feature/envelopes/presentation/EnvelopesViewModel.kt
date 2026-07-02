package pe.kipu.feature.envelopes.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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
    private val deleteTarget = MutableStateFlow<EnvelopeBudgetState?>(null)
    private val adjustLimitError = MutableStateFlow<String?>(null)

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
            deleteTarget,
        ) { (budgets, categories, movements, plan), adjust, creating, form, delete ->
            EnvelopesUiState.Content(
                budgets = budgets.map { budget ->
                    EnvelopeBudgetUiModel(
                        budget = budget,
                        recentMovements = getEnvelopeRecentMovements(
                            categoryId = budget.categoryId,
                            movements = movements,
                        ),
                    )
                },
                categories = categories,
                usedCategoryIds = budgets.map { it.categoryId }.toSet(),
                planBalance = calculateWeeklyEnvelopeBalance(plan, budgets),
                adjustTarget = adjust,
                showCreateDialog = creating,
                createForm = form,
                deleteTarget = delete,
                budgetCycle = plan?.budgetCycle ?: pe.kipu.core.domain.model.BudgetCycle.WEEKLY,
            )
        }

        return combine(contentFlow, adjustLimitError) { content, limitError ->
            content.copy(adjustLimitError = limitError)
        }.map<EnvelopesUiState.Content, EnvelopesUiState> { it }
            .catch {
                emit(EnvelopesUiState.Error("No pudimos cargar tus sobres"))
            }
    }

    fun onAdjustClick(budget: EnvelopeBudgetState) {
        adjustTarget.value = budget
        adjustLimitError.value = null
    }

    fun onDismissAdjust() {
        adjustTarget.value = null
        adjustLimitError.value = null
    }

    fun onSaveWeeklyLimit(amountText: String) {
        val envelope = adjustTarget.value ?: return
        viewModelScope.launch {
            when (val parsed = MoneyInputParser.parsePen(amountText)) {
                is DomainResult.Err -> adjustLimitError.value = "Ingresa un monto válido"
                is DomainResult.Ok -> {
                    updateEnvelopeWeeklyLimit(envelope.envelopeId, parsed.value)
                    adjustTarget.value = null
                    adjustLimitError.value = null
                }
            }
        }
    }

    fun onCreateClick() {
        showCreateDialog.value = true
        createForm.value = EnvelopeCreateFormState()
    }

    fun onDismissCreate() {
        showCreateDialog.value = false
        createForm.value = EnvelopeCreateFormState()
    }

    fun onCreateNameChanged(value: String) {
        createForm.update { it.copy(name = value, errorMessage = null) }
    }

    fun onCreateCategorySelected(index: Int) {
        createForm.update { it.copy(selectedCategoryIndex = index, errorMessage = null) }
    }

    fun onCreateAmountChanged(value: String) {
        createForm.update { it.copy(amountText = value, errorMessage = null) }
    }

    fun onConfirmCreate() {
        val state = (_uiState.value as? EnvelopesUiState.Content) ?: return
        val available = state.categories.filter { it.id !in state.usedCategoryIds }
        val category = available.getOrNull(createForm.value.selectedCategoryIndex) ?: return

        viewModelScope.launch {
            createForm.update { it.copy(isSaving = true, errorMessage = null) }
            when (val parsed = MoneyInputParser.parsePen(createForm.value.amountText)) {
                is DomainResult.Err -> {
                    createForm.update {
                        it.copy(isSaving = false, errorMessage = "Ingresa un monto válido")
                    }
                }

                is DomainResult.Ok -> {
                    createEnvelope(
                        name = createForm.value.name,
                        categoryId = category.id,
                        weeklyLimit = parsed.value,
                    )
                        .onSuccess {
                            showCreateDialog.value = false
                            createForm.value = EnvelopeCreateFormState()
                        }
                        .onFailure {
                            createForm.update {
                                it.copy(
                                    isSaving = false,
                                    errorMessage = "No pudimos crear el sobre. Revisa los datos.",
                                )
                            }
                        }
                }
            }
        }
    }

    fun onDeleteClick(budget: EnvelopeBudgetState) {
        deleteTarget.value = budget
    }

    fun onDismissDelete() {
        deleteTarget.value = null
    }

    fun onConfirmDelete() {
        val target = deleteTarget.value ?: return
        viewModelScope.launch {
            deleteEnvelope(target.envelopeId)
            deleteTarget.value = null
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
)
