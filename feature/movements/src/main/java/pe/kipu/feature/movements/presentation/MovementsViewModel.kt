package pe.kipu.feature.movements.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.kipu.core.domain.model.ConfirmMovementResult
import pe.kipu.core.domain.model.DuplicateResolution
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementDuplicatePair
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.repository.CategoryRepository
import pe.kipu.core.domain.repository.EnvelopeRepository
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
import pe.kipu.core.domain.usecase.DeleteMovementUseCase
import pe.kipu.core.domain.usecase.LinkMovementToCommitmentUseCase
import pe.kipu.core.domain.usecase.ObserveMovementAuditLogsUseCase
import pe.kipu.core.domain.usecase.ObserveSavingsGoalCommitmentsUseCase
import pe.kipu.core.domain.usecase.PrepareUnexpectedExpenseUseCase
import pe.kipu.core.domain.usecase.RegisterUnexpectedExpenseUseCase
import pe.kipu.core.domain.usecase.UpdateMovementCategoryUseCase
import pe.kipu.core.domain.usecase.UpdateMovementUseCase
import pe.kipu.core.domain.util.MoneyInputParser
import pe.kipu.feature.movements.presentation.ManualMovementChannelOption
import pe.kipu.feature.movements.ui.EditMovementFormState
import pe.kipu.feature.movements.ui.ManualMovementFormState

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MovementsViewModel @Inject constructor(
    private val movementRepository: MovementRepository,
    private val categoryRepository: CategoryRepository,
    private val envelopeRepository: EnvelopeRepository,
    private val observePendingNotificationMovements: ObservePendingNotificationMovementsUseCase,
    private val observeMovementDuplicatePairs: ObserveMovementDuplicatePairsUseCase,
    private val observeSavingsGoalCommitments: ObserveSavingsGoalCommitmentsUseCase,
    private val observeMovementAuditLogs: ObserveMovementAuditLogsUseCase,
    private val resolveDuplicateMovement: ResolveDuplicateMovementUseCase,
    private val dismissDuplicatePair: DismissDuplicatePairUseCase,
    private val confirmPendingNotificationMovement: ConfirmPendingNotificationMovementUseCase,
    private val dismissPendingNotificationMovement: DismissPendingNotificationMovementUseCase,
    private val updateMovementCategory: UpdateMovementCategoryUseCase,
    private val linkMovementToCommitment: LinkMovementToCommitmentUseCase,
    private val createManualMovement: CreateManualMovementUseCase,
    private val updateMovement: UpdateMovementUseCase,
    private val deleteMovement: DeleteMovementUseCase,
    private val prepareUnexpectedExpense: PrepareUnexpectedExpenseUseCase? = null,
    private val registerUnexpectedExpense: RegisterUnexpectedExpenseUseCase? = null,
) : ViewModel() {

    private val pendingResolution = MutableStateFlow<MovementDuplicatePair?>(null)
    private val pendingNotificationConfirm = MutableStateFlow<PendingNotificationConfirmState?>(null)
    private val selectedTab = MutableStateFlow(MovementsTab.ACTIVE)
    private val selectedFilter = MutableStateFlow(MovementChannelFilter.ALL)
    private val selectedAuditFilter = MutableStateFlow(MovementAuditFilter.ALL)
    private val categoryFilterId = MutableStateFlow<String?>(null)
    private val categoryChangeTarget = MutableStateFlow<Movement?>(null)
    private val goalLinkTarget = MutableStateFlow<Movement?>(null)
    private val manualMovementForm = MutableStateFlow<ManualMovementFormState?>(null)
    private val unexpectedExpenseConfirmation =
        MutableStateFlow<UnexpectedExpenseConfirmationState?>(null)
    private val editMovementForm = MutableStateFlow<EditMovementFormState?>(null)
    private val movementToDelete = MutableStateFlow<Movement?>(null)
    private val isActionInProgress = MutableStateFlow(false)
    private val reloadRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    
    private val _events = MutableSharedFlow<MovementsEvent>()
    val events = _events.asSharedFlow()

    private val _uiState = MutableStateFlow<MovementsUiState>(MovementsUiState.Loading)
    val uiState: StateFlow<MovementsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            reloadRequests
                .onStart { emit(Unit) }
                .flatMapLatest { observeMovementsState() }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun retryLoad() {
        reloadRequests.tryEmit(Unit)
    }

    fun clearStalePendingNotificationConfirm() {
        pendingNotificationConfirm.value = null
    }

    private fun observeMovementsState(): kotlinx.coroutines.flow.Flow<MovementsUiState> {
        val baseDataFlow = combine(
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
        }.combine(envelopeRepository.observeEnvelopes()) { data, envelopes ->
            data.copy(envelopes = envelopes)
        }

        val dataFlow = combine(
            baseDataFlow,
            observeSavingsGoalCommitments(),
            observeMovementAuditLogs(),
            selectedTab,
            selectedAuditFilter,
        ) { data, savingsGoals, auditLogs, tab, auditFilter ->
            data.copy(savingsGoals = savingsGoals, auditLogs = auditLogs) to (tab to auditFilter)
        }

        val dialogsFlow = combine(
            categoryFilterId,
            pendingResolution,
            pendingNotificationConfirm,
            categoryChangeTarget,
            goalLinkTarget,
        ) { catId, pending, pendingConfirm, changeTarget, linkTarget ->
            DialogsState(catId, pending, pendingConfirm, changeTarget, linkTarget)
        }

        val formsFlow = combine(
            manualMovementForm,
            unexpectedExpenseConfirmation,
            editMovementForm,
            movementToDelete,
            isActionInProgress,
        ) { manualForm, unexpectedConfirmation, editForm, toDelete, inProgress ->
            FormsState(manualForm, unexpectedConfirmation, editForm, toDelete, inProgress)
        }

        return combine(
            dataFlow,
            dialogsFlow,
            formsFlow,
        ) { (data, tabAndFilter), dialogs, forms ->
            MovementsUiState.Content(
                movements = data.movements,
                categories = data.categories,
                envelopes = data.envelopes,
                categoryNamesById = data.categories.associate { it.id to it.name },
                selectedTab = tabAndFilter.first,
                selectedFilter = data.selectedFilter,
                categoryFilterId = dialogs.categoryFilterId,
                categoryFilterName = dialogs.categoryFilterId?.let { id -> data.categories.find { it.id == id }?.name },
                auditLogs = data.auditLogs,
                selectedAuditFilter = tabAndFilter.second,
                pendingNotificationIncomes = data.pendingNotificationIncomes,
                duplicatePairs = data.duplicatePairs,
                pendingResolution = dialogs.pendingResolution,
                pendingNotificationConfirm = dialogs.pendingNotificationConfirm,
                categoryChangeTarget = dialogs.categoryChangeTarget,
                goalLinkTarget = dialogs.goalLinkTarget,
                savingsGoals = data.savingsGoals,
                manualMovementForm = forms.manualMovementForm,
                unexpectedExpenseConfirmation = forms.unexpectedExpenseConfirmation,
                editMovementForm = forms.editMovementForm,
                movementToDelete = forms.movementToDelete,
                isActionInProgress = forms.isActionInProgress,
            ) as MovementsUiState
        }.onStart { emit(MovementsUiState.Loading) }
            .catch {
                emit(MovementsUiState.Error("No pudimos cargar tus movimientos"))
            }
    }

    fun onTabSelected(tab: MovementsTab) {
        selectedTab.update { tab }
    }

    fun onAuditFilterSelected(filter: MovementAuditFilter) {
        selectedAuditFilter.update { filter }
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
        if (isActionInProgress.value) return
        categoryChangeTarget.value = null
    }

    fun onCategorySelected(categoryId: String) {
        val movement = categoryChangeTarget.value ?: return
        if (!beginAction()) return
        viewModelScope.launch {
            try {
                updateMovementCategory(movement.id, categoryId)
                    .onSuccess {
                        if (categoryChangeTarget.value?.id == movement.id) {
                            categoryChangeTarget.value = null
                        }
                    }
                    .onFailure { error ->
                        if (error is CancellationException) throw error
                        _events.emit(MovementsEvent.ShowSnackbar("No pudimos cambiar la categoría"))
                    }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _events.emit(MovementsEvent.ShowSnackbar("No pudimos cambiar la categoría"))
            } finally {
                endAction()
            }
        }
    }

    fun onLinkGoalClick(movement: Movement) {
        goalLinkTarget.value = movement
    }

    fun onDismissGoalLink() {
        if (isActionInProgress.value) return
        goalLinkTarget.value = null
    }

    fun onGoalSelected(commitmentId: String?) {
        val movement = goalLinkTarget.value ?: return
        if (!beginAction()) return
        viewModelScope.launch {
            try {
                linkMovementToCommitment(movement.id, commitmentId)
                    .onSuccess {
                        if (goalLinkTarget.value?.id == movement.id) {
                            goalLinkTarget.value = null
                        }
                    }
                    .onFailure { error ->
                        if (error is CancellationException) throw error
                        _events.emit(MovementsEvent.ShowSnackbar("No pudimos vincular la meta"))
                    }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _events.emit(MovementsEvent.ShowSnackbar("No pudimos vincular la meta"))
            } finally {
                endAction()
            }
        }
    }

    fun onDuplicatePairClick(pair: MovementDuplicatePair) {
        pendingResolution.value = pair
    }

    fun onDismissDuplicateDialog() {
        if (isActionInProgress.value) return
        pendingResolution.value = null
    }

    fun onResolveDuplicate(resolution: DuplicateResolution) {
        val pair = pendingResolution.value ?: return
        if (isActionInProgress.value) return
        if (resolution == DuplicateResolution.CANCEL) {
            pendingResolution.value = null
            return
        }
        if (!beginAction()) return
        viewModelScope.launch {
            try {
                val resolveResult = resolveDuplicateMovement(pair, resolution)
                val result = if (
                    resolveResult.isSuccess && resolution == DuplicateResolution.SAVE_AS_NEW
                ) {
                    dismissDuplicatePair(pair)
                } else {
                    resolveResult
                }
                result
                    .onSuccess {
                        if (pendingResolution.value == pair) {
                            pendingResolution.value = null
                        }
                    }
                    .onFailure { error ->
                        if (error is CancellationException) throw error
                        _events.emit(MovementsEvent.ShowSnackbar("No pudimos resolver el duplicado"))
                    }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _events.emit(MovementsEvent.ShowSnackbar("No pudimos resolver el duplicado"))
            } finally {
                endAction()
            }
        }
    }

    fun onConfirmPendingNotification(movementId: String) {
        if (!beginAction()) return
        viewModelScope.launch {
            try {
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

                    ConfirmMovementResult.Cancelled -> {
                        _events.emit(MovementsEvent.ShowSnackbar("No pudimos confirmar el movimiento"))
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _events.emit(MovementsEvent.ShowSnackbar("No pudimos confirmar el movimiento"))
            } finally {
                endAction()
            }
        }
    }

    fun onDismissPendingNotification(movementId: String) {
        if (!beginAction()) return
        viewModelScope.launch {
            try {
                if (!dismissPendingNotificationMovement(movementId)) {
                    _events.emit(MovementsEvent.ShowSnackbar("No pudimos descartar el movimiento"))
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _events.emit(MovementsEvent.ShowSnackbar("No pudimos descartar el movimiento"))
            } finally {
                endAction()
            }
        }
    }

    fun onResolvePendingNotificationDuplicate(resolution: DuplicateResolution) {
        val state = pendingNotificationConfirm.value ?: return
        if (isActionInProgress.value) return
        if (resolution == DuplicateResolution.CANCEL) {
            pendingNotificationConfirm.value = null
            return
        }
        if (!beginAction()) return
        viewModelScope.launch {
            try {
                when (val result = confirmPendingNotificationMovement(state.movementId, resolution)) {
                    is ConfirmMovementResult.Saved -> pendingNotificationConfirm.value = null
                    ConfirmMovementResult.Cancelled -> {
                        if (resolution == DuplicateResolution.MERGE) {
                            pendingNotificationConfirm.value = null
                        } else {
                            _events.emit(MovementsEvent.ShowSnackbar("No pudimos resolver el duplicado"))
                        }
                    }

                    is ConfirmMovementResult.DuplicatePending -> {
                        pendingNotificationConfirm.value = state.copy(
                            duplicateMatches = result.matches,
                        )
                        _events.emit(MovementsEvent.ShowSnackbar("No pudimos resolver el duplicado"))
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _events.emit(MovementsEvent.ShowSnackbar("No pudimos resolver el duplicado"))
            } finally {
                endAction()
            }
        }
    }

    fun onAddMovementClick() {
        onRegisterManualClicked(PaymentChannel.CASH)
    }

    fun onRegisterManualClicked(defaultChannel: PaymentChannel = PaymentChannel.CASH) {
        val defaultCategoryId = currentContent()?.categories?.firstOrNull()?.id
        manualMovementForm.value = ManualMovementFormState(
            channel = defaultChannel,
            categoryId = defaultCategoryId,
        )
    }

    fun onDismissManualMovement() {
        if (manualMovementForm.value?.isSaving == true) return
        manualMovementForm.value = null
    }

    fun onManualMovementTypeSelected(type: MovementType) {
        if (manualMovementForm.value?.isSaving != false) return
        manualMovementForm.update {
            it?.copy(
                movementType = type,
                envelopeId = if (type == MovementType.EXPENSE) it.envelopeId else null,
                isUnexpectedExpense = type == MovementType.EXPENSE && it.isUnexpectedExpense,
                errorMessage = null,
            )
        }
    }

    fun onManualChannelSelected(option: ManualMovementChannelOption) {
        if (manualMovementForm.value?.isSaving != false) return
        manualMovementForm.update { it?.copy(channel = option.channel, errorMessage = null) }
    }

    fun onManualAmountChanged(value: String) {
        if (manualMovementForm.value?.isSaving != false) return
        manualMovementForm.update {
            it?.copy(
                amountText = value,
                amountErrorMessage = ManualMovementAmountValidator.errorMessage(value),
                errorMessage = null,
            )
        }
    }

    fun onManualCategorySelected(categoryId: String) {
        if (manualMovementForm.value?.isSaving != false) return
        manualMovementForm.update { it?.copy(categoryId = categoryId, errorMessage = null) }
    }

    fun onManualEnvelopeSelected(envelopeId: String?) {
        if (manualMovementForm.value?.isSaving != false) return
        val envelope = currentContent()?.envelopes?.find { it.id == envelopeId }
        manualMovementForm.update {
            it?.copy(
                envelopeId = envelope?.id,
                categoryId = envelope?.categoryId ?: it.categoryId,
                errorMessage = null,
            )
        }
    }

    fun onUnexpectedExpenseChanged(isUnexpected: Boolean) {
        if (manualMovementForm.value?.isSaving != false) return
        manualMovementForm.update {
            it?.copy(
                isUnexpectedExpense = it.movementType == MovementType.EXPENSE && isUnexpected,
                errorMessage = null,
            )
        }
    }

    fun onManualDescriptionChanged(value: String) {
        if (manualMovementForm.value?.isSaving != false) return
        manualMovementForm.update { it?.copy(description = value) }
    }

    fun onManualCounterpartyChanged(value: String) {
        if (manualMovementForm.value?.isSaving != false) return
        manualMovementForm.update { it?.copy(counterpartyName = value) }
    }

    fun onSaveManualMovement() {
        val form = manualMovementForm.value ?: return
        if (form.isSaving) return
        val categoryId = form.categoryId
        if (categoryId.isNullOrBlank()) {
            manualMovementForm.update { it?.copy(errorMessage = "Elige una categoría") }
            return
        }

        val amountError = ManualMovementAmountValidator.errorMessage(form.amountText)
        if (amountError != null) {
            manualMovementForm.update { it?.copy(amountErrorMessage = amountError) }
            return
        }

        val amount = when (val parsed = MoneyInputParser.parsePen(form.amountText)) {
            is DomainResult.Ok -> parsed.value
            is DomainResult.Err -> {
                manualMovementForm.update { it?.copy(amountErrorMessage = ManualMovementAmountValidator.INVALID_AMOUNT_MESSAGE) }
                return
            }
        }
        if (amount.isZero()) {
            manualMovementForm.update { it?.copy(amountErrorMessage = ManualMovementAmountValidator.ZERO_AMOUNT_MESSAGE) }
            return
        }

        if (form.isUnexpectedExpense) {
            prepareUnexpectedExpense(form, amount)
            return
        }

        manualMovementForm.value = form.copy(isSaving = true, errorMessage = null)
        viewModelScope.launch {
            try {
                createManualMovement(
                    type = form.movementType,
                    amount = amount,
                    categoryId = categoryId,
                    channel = form.channel,
                    description = form.description,
                    counterpartyName = form.counterpartyName,
                    envelopeId = form.envelopeId,
                ).fold(
                    onSuccess = {
                        manualMovementForm.value = null
                    },
                    onFailure = { error ->
                        if (error is CancellationException) throw error
                        showManualSaveError()
                    },
                )
            } catch (error: CancellationException) {
                manualMovementForm.update { it?.copy(isSaving = false) }
                throw error
            } catch (_: Exception) {
                showManualSaveError()
            }
        }
    }

    private fun prepareUnexpectedExpense(form: ManualMovementFormState, amount: pe.kipu.core.domain.model.Money) {
        val prepare = prepareUnexpectedExpense
        if (prepare == null || registerUnexpectedExpense == null) {
            manualMovementForm.update {
                it?.copy(errorMessage = "No pudimos preparar el gasto imprevisto")
            }
            return
        }
        manualMovementForm.value = form.copy(isSaving = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val preview = prepare(amount)
                manualMovementForm.value = null
                unexpectedExpenseConfirmation.value = UnexpectedExpenseConfirmationState(
                    form = form.copy(isSaving = false),
                    preview = preview,
                )
            } catch (cancellation: CancellationException) {
                manualMovementForm.update { it?.copy(isSaving = false) }
                throw cancellation
            } catch (_: Exception) {
                manualMovementForm.value = form.copy(
                    isSaving = false,
                    errorMessage = "No pudimos calcular la cobertura de esta compra",
                )
            }
        }
    }

    fun onUnexpectedAdjustmentToggled(envelopeId: String) {
        unexpectedExpenseConfirmation.update { state ->
            if (state == null || state.isSaving) return@update state
            state.copy(
                selectedEnvelopeIds = if (envelopeId in state.selectedEnvelopeIds) {
                    state.selectedEnvelopeIds - envelopeId
                } else {
                    state.selectedEnvelopeIds + envelopeId
                },
                errorMessage = null,
            )
        }
    }

    fun onDismissUnexpectedExpenseConfirmation() {
        val state = unexpectedExpenseConfirmation.value ?: return
        if (state.isSaving) return
        unexpectedExpenseConfirmation.value = null
        manualMovementForm.value = state.form.copy(isSaving = false, errorMessage = null)
    }

    fun onConfirmUnexpectedExpense(applyAdjustments: Boolean) {
        val state = unexpectedExpenseConfirmation.value ?: return
        if (state.isSaving) return
        val register = registerUnexpectedExpense ?: return
        val categoryId = state.form.categoryId ?: return
        val amount = when (val parsed = MoneyInputParser.parsePen(state.form.amountText)) {
            is DomainResult.Ok -> parsed.value
            is DomainResult.Err -> return
        }
        unexpectedExpenseConfirmation.value = state.copy(isSaving = true, errorMessage = null)
        viewModelScope.launch {
            try {
                register(
                    amount = amount,
                    categoryId = categoryId,
                    channel = state.form.channel,
                    description = state.form.description,
                    counterpartyName = state.form.counterpartyName,
                    envelopeId = state.form.envelopeId,
                    reserveAmount = state.preview.coverage.fromReserve,
                    recoveryPlan = state.selectedRecoveryPlan.takeIf { applyAdjustments },
                ).getOrThrow()
                unexpectedExpenseConfirmation.value = null
            } catch (cancellation: CancellationException) {
                unexpectedExpenseConfirmation.update { it?.copy(isSaving = false) }
                throw cancellation
            } catch (_: Exception) {
                unexpectedExpenseConfirmation.update {
                    it?.copy(
                        isSaving = false,
                        errorMessage = "No pudimos guardar la compra ni sus ajustes",
                    )
                }
            }
        }
    }

    private fun showManualSaveError() {
        manualMovementForm.update {
            it?.copy(
                isSaving = false,
                errorMessage = "No pudimos guardar el movimiento",
            )
        }
    }

    fun onEditMovementClick(movement: Movement) {
        if (isActionInProgress.value) return
        editMovementForm.value = EditMovementFormState.fromMovement(movement)
    }

    fun onDismissEditMovement() {
        if (editMovementForm.value?.isSaving == true) return
        editMovementForm.value = null
    }

    fun onEditMovementTypeSelected(type: MovementType) {
        if (editMovementForm.value?.isSaving != false) return
        editMovementForm.update { it?.copy(movementType = type, errorMessage = null) }
    }

    fun onEditChannelSelected(option: ManualMovementChannelOption) {
        if (editMovementForm.value?.isSaving != false) return
        editMovementForm.update { it?.copy(channel = option.channel, errorMessage = null) }
    }

    fun onEditAmountChanged(value: String) {
        if (editMovementForm.value?.isSaving != false) return
        editMovementForm.update {
            it?.copy(
                amountText = value,
                amountErrorMessage = ManualMovementAmountValidator.errorMessage(value),
                errorMessage = null,
            )
        }
    }

    fun onEditCategorySelected(categoryId: String) {
        if (editMovementForm.value?.isSaving != false) return
        editMovementForm.update { it?.copy(categoryId = categoryId, errorMessage = null) }
    }

    fun onEditDescriptionChanged(value: String) {
        if (editMovementForm.value?.isSaving != false) return
        editMovementForm.update { it?.copy(description = value) }
    }

    fun onEditCounterpartyChanged(value: String) {
        if (editMovementForm.value?.isSaving != false) return
        editMovementForm.update { it?.copy(counterpartyName = value) }
    }

    fun onSaveEditedMovement() {
        val form = editMovementForm.value ?: return
        if (form.isSaving) return
        val categoryId = form.categoryId
        if (categoryId.isNullOrBlank()) {
            editMovementForm.update { it?.copy(errorMessage = "Elige una categoría") }
            return
        }

        val amountError = ManualMovementAmountValidator.errorMessage(form.amountText)
        if (amountError != null) {
            editMovementForm.update { it?.copy(amountErrorMessage = amountError) }
            return
        }

        val amount = when (val parsed = MoneyInputParser.parsePen(form.amountText)) {
            is DomainResult.Ok -> parsed.value
            is DomainResult.Err -> {
                editMovementForm.update { it?.copy(amountErrorMessage = ManualMovementAmountValidator.INVALID_AMOUNT_MESSAGE) }
                return
            }
        }
        if (amount.isZero()) {
            editMovementForm.update { it?.copy(amountErrorMessage = ManualMovementAmountValidator.ZERO_AMOUNT_MESSAGE) }
            return
        }

        editMovementForm.value = form.copy(isSaving = true, errorMessage = null)
        viewModelScope.launch {
            try {
                updateMovement(
                    movementId = form.movementId,
                    type = form.movementType,
                    amount = amount,
                    categoryId = categoryId,
                    channel = form.channel,
                    description = form.description,
                    counterpartyName = form.counterpartyName,
                ).fold(
                    onSuccess = {
                        editMovementForm.value = null
                    },
                    onFailure = { error ->
                        if (error is CancellationException) throw error
                        editMovementForm.update {
                            it?.copy(isSaving = false, errorMessage = "No pudimos guardar los cambios")
                        }
                    },
                )
            } catch (error: CancellationException) {
                editMovementForm.update { it?.copy(isSaving = false) }
                throw error
            } catch (_: Exception) {
                editMovementForm.update {
                    it?.copy(isSaving = false, errorMessage = "No pudimos guardar los cambios")
                }
            }
        }
    }

    fun onDeleteMovementClick(movement: Movement) {
        if (isActionInProgress.value) return
        movementToDelete.value = movement
    }

    fun onDismissDeleteMovement() {
        if (isActionInProgress.value) return
        movementToDelete.value = null
    }

    fun onConfirmDeleteMovement() {
        val movement = movementToDelete.value ?: return
        if (!beginAction()) return
        viewModelScope.launch {
            try {
                deleteMovement(movement.id).fold(
                    onSuccess = {
                        if (movementToDelete.value?.id == movement.id) {
                            movementToDelete.value = null
                        }
                    },
                    onFailure = { error ->
                        if (error is CancellationException) throw error
                        _events.emit(MovementsEvent.ShowSnackbar("No pudimos eliminar el movimiento"))
                    },
                )
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _events.emit(MovementsEvent.ShowSnackbar("No pudimos eliminar el movimiento"))
            } finally {
                endAction()
            }
        }
    }

    private fun beginAction(): Boolean {
        if (isActionInProgress.value) return false
        isActionInProgress.value = true
        return true
    }

    private fun endAction() {
        isActionInProgress.value = false
    }

    private fun currentContent(): MovementsUiState.Content? = _uiState.value as? MovementsUiState.Content
}

private data class DialogsState(
    val categoryFilterId: String?,
    val pendingResolution: MovementDuplicatePair?,
    val pendingNotificationConfirm: PendingNotificationConfirmState?,
    val categoryChangeTarget: Movement?,
    val goalLinkTarget: Movement?,
)

private data class FormsState(
    val manualMovementForm: ManualMovementFormState?,
    val unexpectedExpenseConfirmation: UnexpectedExpenseConfirmationState?,
    val editMovementForm: EditMovementFormState?,
    val movementToDelete: Movement?,
    val isActionInProgress: Boolean,
)
