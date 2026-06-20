package pe.kipu.feature.movements.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.kipu.core.domain.model.ConfirmMovementResult
import pe.kipu.core.domain.model.DuplicateResolution
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementDuplicatePair
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.repository.CategoryRepository
import pe.kipu.core.domain.repository.MovementRepository
import pe.kipu.core.domain.usecase.ConfirmPendingNotificationMovementUseCase
import pe.kipu.core.domain.usecase.DismissDuplicatePairUseCase
import pe.kipu.core.domain.usecase.DismissPendingNotificationMovementUseCase
import pe.kipu.core.domain.usecase.ObserveMovementDuplicatePairsUseCase
import pe.kipu.core.domain.usecase.ObservePendingNotificationMovementsUseCase
import pe.kipu.core.domain.usecase.ResolveDuplicateMovementUseCase
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.usecase.CreateManualMovementUseCase
import pe.kipu.core.domain.usecase.UpdateMovementCategoryUseCase
import pe.kipu.core.domain.util.MoneyInputParser
import pe.kipu.feature.movements.presentation.ManualMovementChannelOption
import pe.kipu.feature.movements.ui.ManualMovementFormState

@HiltViewModel
class MovementsViewModel @Inject constructor(
    movementRepository: MovementRepository,
    categoryRepository: CategoryRepository,
    observePendingNotificationMovements: ObservePendingNotificationMovementsUseCase,
    observeMovementDuplicatePairs: ObserveMovementDuplicatePairsUseCase,
    private val resolveDuplicateMovement: ResolveDuplicateMovementUseCase,
    private val dismissDuplicatePair: DismissDuplicatePairUseCase,
    private val confirmPendingNotificationMovement: ConfirmPendingNotificationMovementUseCase,
    private val dismissPendingNotificationMovement: DismissPendingNotificationMovementUseCase,
    private val updateMovementCategory: UpdateMovementCategoryUseCase,
    private val createManualMovement: CreateManualMovementUseCase,
) : ViewModel() {

    private val pendingResolution = MutableStateFlow<MovementDuplicatePair?>(null)
    private val pendingNotificationConfirm = MutableStateFlow<PendingNotificationConfirmState?>(null)
    private val selectedFilter = MutableStateFlow(MovementChannelFilter.ALL)
    private val categoryFilterId = MutableStateFlow<String?>(null)
    private val categoryChangeTarget = MutableStateFlow<Movement?>(null)

    private val _uiState = MutableStateFlow<MovementsUiState>(MovementsUiState.Loading)
    val uiState: StateFlow<MovementsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                combine(
                    movementRepository.observeMovements(),
                    categoryRepository.observeCategories(),
                    observePendingNotificationMovements(),
                    observeMovementDuplicatePairs(),
                    selectedFilter,
                ) { movements, categories, pendingNotifications, duplicatePairs, filter ->
                    MovementsData(
                        movements = movements.filter { it.status == MovementStatus.CONFIRMED },
                        categories = categories,
                        pendingNotificationIncomes = pendingNotifications,
                        duplicatePairs = duplicatePairs,
                        selectedFilter = filter,
                    )
                },
                categoryFilterId,
                pendingResolution,
                pendingNotificationConfirm,
                categoryChangeTarget,
            ) { data, categoryId, pending, pendingConfirm, changeTarget ->
                MovementsUiState.Content(
                    movements = data.movements,
                    categories = data.categories,
                    categoryNamesById = data.categories.associate { it.id to it.name },
                    selectedFilter = data.selectedFilter,
                    categoryFilterId = categoryId,
                    categoryFilterName = categoryId?.let { id -> data.categories.find { it.id == id }?.name },
                    pendingNotificationIncomes = data.pendingNotificationIncomes,
                    duplicatePairs = data.duplicatePairs,
                    pendingResolution = pending,
                    pendingNotificationConfirm = pendingConfirm,
                    categoryChangeTarget = changeTarget,
                    showAddOptionsDialog = currentContent()?.showAddOptionsDialog ?: false,
                    manualMovementForm = currentContent()?.manualMovementForm,
                )
            }
                .catch {
                    _uiState.value = MovementsUiState.Error("No pudimos cargar tus movimientos")
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun applyCategoryFilter(categoryId: String?) {
        categoryFilterId.update { categoryId }
    }

    fun clearCategoryFilter() {
        categoryFilterId.update { null }
    }

    fun onFilterSelected(filter: MovementChannelFilter) {
        selectedFilter.update { filter }
    }

    fun onChangeCategoryClick(movement: Movement) {
        categoryChangeTarget.value = movement
    }

    fun onDismissCategoryChange() {
        categoryChangeTarget.value = null
    }

    fun onCategorySelected(categoryId: String) {
        val movement = categoryChangeTarget.value ?: return
        viewModelScope.launch {
            updateMovementCategory(movement.id, categoryId)
            categoryChangeTarget.value = null
        }
    }

    fun onDuplicatePairClick(pair: MovementDuplicatePair) {
        pendingResolution.value = pair
    }

    fun onDismissDuplicateDialog() {
        pendingResolution.value = null
    }

    fun onResolveDuplicate(resolution: DuplicateResolution) {
        val pair = pendingResolution.value ?: return
        viewModelScope.launch {
            resolveDuplicateMovement(pair, resolution)
            if (resolution == DuplicateResolution.SAVE_AS_NEW) {
                dismissDuplicatePair(pair)
            }
            pendingResolution.value = null
        }
    }

    fun onConfirmPendingNotification(movementId: String) {
        viewModelScope.launch {
            when (val result = confirmPendingNotificationMovement(movementId)) {
                is ConfirmMovementResult.Saved -> {
                    pendingNotificationConfirm.value = null
                }

                is ConfirmMovementResult.DuplicatePending -> {
                    pendingNotificationConfirm.value = PendingNotificationConfirmState(
                        movementId = movementId,
                        duplicateMatches = result.matches,
                    )
                }

                ConfirmMovementResult.Cancelled -> Unit
            }
        }
    }

    fun onDismissPendingNotification(movementId: String) {
        viewModelScope.launch {
            dismissPendingNotificationMovement(movementId)
        }
    }

    fun onResolvePendingNotificationDuplicate(resolution: DuplicateResolution) {
        val state = pendingNotificationConfirm.value ?: return
        if (resolution == DuplicateResolution.CANCEL) {
            pendingNotificationConfirm.value = null
            return
        }
        viewModelScope.launch {
            confirmPendingNotificationMovement(state.movementId, resolution)
            pendingNotificationConfirm.value = null
        }
    }

    fun onAddMovementClick() {
        updateContent { it.copy(showAddOptionsDialog = true) }
    }

    fun onDismissAddOptions() {
        updateContent { it.copy(showAddOptionsDialog = false) }
    }

    fun onRegisterManualClicked(defaultChannel: PaymentChannel) {
        val defaultCategoryId = currentContent()?.categories?.firstOrNull()?.id
        updateContent {
            it.copy(
                showAddOptionsDialog = false,
                manualMovementForm = ManualMovementFormState(
                    channel = defaultChannel,
                    categoryId = defaultCategoryId,
                ),
            )
        }
    }

    fun onDismissManualMovement() {
        updateContent { it.copy(manualMovementForm = null) }
    }

    fun onManualMovementTypeSelected(type: MovementType) {
        updateManualForm { it.copy(movementType = type, errorMessage = null) }
    }

    fun onManualChannelSelected(option: ManualMovementChannelOption) {
        updateManualForm { it.copy(channel = option.channel, errorMessage = null) }
    }

    fun onManualAmountChanged(value: String) {
        updateManualForm { it.copy(amountText = value, errorMessage = null) }
    }

    fun onManualCategorySelected(categoryId: String) {
        updateManualForm { it.copy(categoryId = categoryId, errorMessage = null) }
    }

    fun onManualDescriptionChanged(value: String) {
        updateManualForm { it.copy(description = value) }
    }

    fun onManualCounterpartyChanged(value: String) {
        updateManualForm { it.copy(counterpartyName = value) }
    }

    fun onSaveManualMovement() {
        val content = currentContent() ?: return
        val form = content.manualMovementForm ?: return
        val categoryId = form.categoryId
        if (categoryId.isNullOrBlank()) {
            updateManualForm { it.copy(errorMessage = "Elige una categoría") }
            return
        }

        val amount = when (val parsed = MoneyInputParser.parsePen(form.amountText)) {
            is DomainResult.Ok -> parsed.value
            is DomainResult.Err -> {
                updateManualForm { it.copy(errorMessage = "Ingresa un monto válido") }
                return
            }
        }
        if (amount.isZero()) {
            updateManualForm { it.copy(errorMessage = "El monto debe ser mayor a cero") }
            return
        }

        viewModelScope.launch {
            updateManualForm { it.copy(isSaving = true, errorMessage = null) }
            createManualMovement(
                type = form.movementType,
                amount = amount,
                categoryId = categoryId,
                channel = form.channel,
                description = form.description,
                counterpartyName = form.counterpartyName,
            ).fold(
                onSuccess = {
                    updateContent { it.copy(manualMovementForm = null) }
                },
                onFailure = {
                    updateManualForm {
                        it.copy(
                            isSaving = false,
                            errorMessage = "No pudimos guardar el movimiento",
                        )
                    }
                },
            )
        }
    }

    private fun currentContent(): MovementsUiState.Content? = _uiState.value as? MovementsUiState.Content

    private fun updateContent(transform: (MovementsUiState.Content) -> MovementsUiState.Content) {
        val current = currentContent() ?: return
        _uiState.update { transform(current) }
    }

    private fun updateManualForm(transform: (ManualMovementFormState) -> ManualMovementFormState) {
        updateContent { content ->
            val form = content.manualMovementForm ?: return@updateContent content
            content.copy(manualMovementForm = transform(form))
        }
    }
}
