package pe.kipu.feature.home.presentation

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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.receipt.MonthlyServiceReceipt
import pe.kipu.core.domain.repository.CategoryRepository
import pe.kipu.core.domain.repository.CommitmentRepository
import pe.kipu.core.domain.repository.EnvelopeRepository
import pe.kipu.core.domain.usecase.CreateManualMovementUseCase
import pe.kipu.core.domain.usecase.ContributeMonthlyReserveUseCase
import pe.kipu.core.domain.usecase.MarkServiceReceiptPaidUseCase
import pe.kipu.core.domain.usecase.AnalyzeVoiceIntentUseCase
import pe.kipu.core.domain.usecase.ObserveHomeInsightsUseCase
import pe.kipu.core.domain.usecase.ObserveMonthlyServiceReceiptsUseCase
import pe.kipu.core.domain.usecase.PrepareUnexpectedExpenseUseCase
import pe.kipu.core.domain.usecase.RegisterUnexpectedExpenseUseCase
import pe.kipu.core.domain.usecase.UnmarkServiceReceiptPaidUseCase
import pe.kipu.core.domain.usecase.UpdateDailyAvailableWidgetUseCase
import pe.kipu.core.domain.voice.VoiceFinancialIntent

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val observeHomeInsights: ObserveHomeInsightsUseCase,
    private val categoryRepository: CategoryRepository,
    private val envelopeRepository: EnvelopeRepository,
    private val updateDailyAvailableWidget: UpdateDailyAvailableWidgetUseCase,
    private val observeMonthlyServiceReceipts: ObserveMonthlyServiceReceiptsUseCase,
    private val markServiceReceiptPaid: MarkServiceReceiptPaidUseCase,
    private val unmarkServiceReceiptPaid: UnmarkServiceReceiptPaidUseCase,
    private val createManualMovement: CreateManualMovementUseCase,
    private val commitmentRepository: CommitmentRepository,
    private val analyzeVoiceIntent: AnalyzeVoiceIntentUseCase,
    private val contributeMonthlyReserve: ContributeMonthlyReserveUseCase,
    private val prepareUnexpectedExpense: PrepareUnexpectedExpenseUseCase,
    private val registerUnexpectedExpense: RegisterUnexpectedExpenseUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _parsedVoiceIntent = MutableStateFlow<VoiceFinancialIntent?>(null)
    val parsedVoiceIntent: StateFlow<VoiceFinancialIntent?> = _parsedVoiceIntent.asStateFlow()
    private val _isAnalyzingVoice = MutableStateFlow(false)
    val isAnalyzingVoice: StateFlow<Boolean> = _isAnalyzingVoice.asStateFlow()
    private val _isSavingVoice = MutableStateFlow(false)
    val isSavingVoice: StateFlow<Boolean> = _isSavingVoice.asStateFlow()
    private val _voiceSaveError = MutableStateFlow<String?>(null)
    val voiceSaveError: StateFlow<String?> = _voiceSaveError.asStateFlow()
    private val _voiceUnexpectedExpense = MutableStateFlow<VoiceUnexpectedExpenseState?>(null)
    val voiceUnexpectedExpense: StateFlow<VoiceUnexpectedExpenseState?> =
        _voiceUnexpectedExpense.asStateFlow()
    private val _isContributingReserve = MutableStateFlow(false)
    val isContributingReserve: StateFlow<Boolean> = _isContributingReserve.asStateFlow()
    private val _reserveContributionError = MutableStateFlow<String?>(null)
    val reserveContributionError: StateFlow<String?> = _reserveContributionError.asStateFlow()

    private val reloadRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init {
        viewModelScope.launch {
            reloadRequests
                .onStart { emit(Unit) }
                .flatMapLatest { observeHomeState() }
                .collect { state ->
                    _uiState.value = state
                    if (state is HomeUiState.Content) {
                        updateDailyAvailableWidget(state.insights)
                    }
                }
        }
    }

    fun retryLoad() {
        reloadRequests.tryEmit(Unit)
    }

    fun contributeMonthlyReserve() {
        if (_isContributingReserve.value) return
        val insights = (_uiState.value as? HomeUiState.Content)?.insights ?: return
        val target = insights.financialPlan
            ?.reserveMonthlyContribution
            ?.takeUnless { it.isZero() }
            ?: return
        if ((insights.availableBalance?.availableBalance ?: return) < target.amount) {
            _reserveContributionError.value = "Tu saldo disponible aún no alcanza para este aporte."
            return
        }
        _isContributingReserve.value = true
        _reserveContributionError.value = null
        viewModelScope.launch {
            try {
                contributeMonthlyReserve(target).getOrThrow()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _reserveContributionError.value = "No pudimos registrar el aporte. Intenta nuevamente."
            } finally {
                _isContributingReserve.value = false
            }
        }
    }

    fun markReceiptPaid(
        receipt: MonthlyServiceReceipt,
        actualAmount: pe.kipu.core.domain.model.Money,
        channel: PaymentChannel = PaymentChannel.CASH,
    ) {
        viewModelScope.launch {
            runCatching {
                markServiceReceiptPaid(receipt = receipt, actualAmount = actualAmount, channel = channel)
            }
        }
    }

    fun unmarkReceiptPaid(receipt: MonthlyServiceReceipt) {
        viewModelScope.launch {
            runCatching {
                unmarkServiceReceiptPaid(receipt = receipt)
            }
        }
    }

    fun onVoiceTranscriptionReceived(rawText: String) {
        if (_isAnalyzingVoice.value) return
        viewModelScope.launch {
            _isAnalyzingVoice.value = true
            try {
                _parsedVoiceIntent.value = analyzeVoiceIntent(rawText)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _parsedVoiceIntent.value = VoiceFinancialIntent.Unknown(rawText)
            } finally {
                _isAnalyzingVoice.value = false
            }
        }
    }

    fun clearParsedVoiceIntent() {
        _parsedVoiceIntent.value = null
        _voiceUnexpectedExpense.value = null
        _voiceSaveError.value = null
    }

    fun saveVoiceIntent(
        intent: VoiceFinancialIntent,
        isUnexpectedExpense: Boolean = false,
        onSaved: () -> Unit = {},
    ) {
        if (_isSavingVoice.value) return
        _isSavingVoice.value = true
        viewModelScope.launch {
            _voiceSaveError.value = null
            try {
                if (isUnexpectedExpense && intent is VoiceFinancialIntent.Expense) {
                    _voiceUnexpectedExpense.value = VoiceUnexpectedExpenseState(
                        intent = intent,
                        preview = prepareUnexpectedExpense(intent.amount),
                    )
                } else {
                    persistVoiceIntent(intent)
                    _parsedVoiceIntent.value = null
                    onSaved()
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _voiceSaveError.value = "No pudimos guardar el movimiento. Intenta nuevamente."
            } finally {
                _isSavingVoice.value = false
            }
        }
    }

    fun onVoiceUnexpectedAdjustmentToggled(envelopeId: String) {
        _voiceUnexpectedExpense.value = _voiceUnexpectedExpense.value?.let { state ->
            if (state.isSaving) state else state.copy(
                selectedEnvelopeIds = if (envelopeId in state.selectedEnvelopeIds) {
                    state.selectedEnvelopeIds - envelopeId
                } else {
                    state.selectedEnvelopeIds + envelopeId
                },
                errorMessage = null,
            )
        }
    }

    fun dismissVoiceUnexpectedExpense() {
        if (_voiceUnexpectedExpense.value?.isSaving == true) return
        _voiceUnexpectedExpense.value = null
    }

    fun confirmVoiceUnexpectedExpense(
        applyAdjustments: Boolean,
        onSaved: () -> Unit = {},
    ) {
        val state = _voiceUnexpectedExpense.value ?: return
        if (state.isSaving) return
        _voiceUnexpectedExpense.value = state.copy(isSaving = true, errorMessage = null)
        viewModelScope.launch {
            try {
                registerUnexpectedExpense(
                    amount = state.intent.amount,
                    categoryId = state.intent.categoryId,
                    channel = state.intent.channel,
                    description = state.intent.description,
                    counterpartyName = state.intent.counterpartyName,
                    envelopeId = state.intent.envelopeId,
                    reserveAmount = state.preview.coverage.fromReserve,
                    recoveryPlan = state.selectedRecoveryPlan.takeIf { applyAdjustments },
                ).getOrThrow()
                _voiceUnexpectedExpense.value = null
                _parsedVoiceIntent.value = null
                onSaved()
            } catch (cancellation: CancellationException) {
                _voiceUnexpectedExpense.value = state.copy(isSaving = false)
                throw cancellation
            } catch (_: Exception) {
                _voiceUnexpectedExpense.value = state.copy(
                    isSaving = false,
                    errorMessage = "No pudimos guardar la compra ni sus ajustes",
                )
            }
        }
    }

    private suspend fun persistVoiceIntent(intent: VoiceFinancialIntent) {
        when (intent) {
            is VoiceFinancialIntent.Expense -> {
                val matched = (_uiState.value as? HomeUiState.Content)?.monthlyReceipts?.find {
                    it.key.identifier == intent.matchedServiceKey?.identifier && !it.isPaid
                }
                if (matched != null) {
                    markServiceReceiptPaid(
                        receipt = matched,
                        actualAmount = intent.amount,
                        channel = intent.channel,
                        envelopeId = intent.envelopeId ?: uniqueEnvelopeIdForCategory(CategoryIds.SERVICES),
                    )
                } else {
                    createManualMovement(
                        type = MovementType.EXPENSE,
                        amount = intent.amount,
                        categoryId = intent.categoryId,
                        channel = intent.channel,
                        description = intent.description,
                        counterpartyName = intent.counterpartyName,
                        envelopeId = intent.envelopeId,
                    ).getOrThrow()
                }
            }

            is VoiceFinancialIntent.Income -> {
                createManualMovement(
                    type = MovementType.INCOME,
                    amount = intent.amount,
                    categoryId = intent.categoryId,
                    channel = intent.channel,
                    description = intent.description,
                    counterpartyName = intent.counterpartyName,
                ).getOrThrow()
            }

            is VoiceFinancialIntent.GoalContribution -> {
                val commitments = commitmentRepository.observeCommitments().firstOrNull().orEmpty()
                val matchedGoal = commitments.find { it.title.contains(intent.goalQuery, ignoreCase = true) }
                createManualMovement(
                    type = MovementType.EXPENSE,
                    amount = intent.amount,
                    categoryId = CategoryIds.OTHER,
                    channel = PaymentChannel.CASH,
                    description = intent.description,
                    commitmentId = matchedGoal?.id,
                ).getOrThrow()
            }

            is VoiceFinancialIntent.ServiceReceiptPayment -> {
                val matched = (_uiState.value as? HomeUiState.Content)?.monthlyReceipts?.find {
                    it.key.identifier == intent.serviceKey.identifier && !it.isPaid
                }
                if (matched != null) {
                    markServiceReceiptPaid(
                        receipt = matched,
                        actualAmount = intent.amount,
                        channel = PaymentChannel.CASH,
                        envelopeId = uniqueEnvelopeIdForCategory(CategoryIds.SERVICES),
                    )
                } else {
                    val amount = requireNotNull(intent.amount) { "El pago necesita un monto" }
                    createManualMovement(
                        type = MovementType.EXPENSE,
                        amount = amount,
                        categoryId = CategoryIds.SERVICES,
                        channel = PaymentChannel.CASH,
                        description = intent.description,
                        envelopeId = uniqueEnvelopeIdForCategory(CategoryIds.SERVICES),
                    ).getOrThrow()
                }
            }

            is VoiceFinancialIntent.Unknown -> error("No se puede guardar un comando sin reconocer")
        }
    }

    private fun uniqueEnvelopeIdForCategory(categoryId: String): String? =
        (_uiState.value as? HomeUiState.Content)
            ?.envelopes
            ?.filter { it.categoryId == categoryId }
            ?.singleOrNull()
            ?.id

    private fun observeHomeState(): Flow<HomeUiState> =
        combine(
            observeHomeInsights(),
            categoryRepository.observeCategories(),
            observeMonthlyServiceReceipts(),
            envelopeRepository.observeEnvelopes(),
        ) { insights, categories, receipts, envelopes ->
            HomeUiState.Content(
                insights = insights,
                categoryNamesById = categories.associate { category ->
                    category.id to category.name
                },
                userCategories = categories
                    .filter { !CategoryIds.isBuiltIn(it.id) }
                    .sortedBy { it.name },
                envelopes = envelopes,
                monthlyReceipts = receipts,
            )
        }
            .map<HomeUiState.Content, HomeUiState> { it }
            .catch {
                emit(HomeUiState.Error("No pudimos cargar tu resumen"))
            }
}
