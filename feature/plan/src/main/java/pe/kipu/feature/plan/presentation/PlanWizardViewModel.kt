package pe.kipu.feature.plan.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.flow.firstWithTimeout
import pe.kipu.core.domain.model.Commitment
import pe.kipu.core.domain.model.CommitmentType
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.Envelope
import pe.kipu.core.domain.model.EnvelopeBudgetState
import pe.kipu.core.domain.model.EnvelopeBudgetStatus
import pe.kipu.core.domain.model.FinancialPlan
import pe.kipu.core.domain.model.FinancialPlanValidationResult
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.plan.CommitmentIds
import pe.kipu.core.domain.plan.FixedExpenseBreakdownCalculator
import pe.kipu.core.domain.plan.GoalType
import pe.kipu.core.domain.plan.IncomeProfile
import pe.kipu.core.domain.plan.PayFrequency
import pe.kipu.core.domain.plan.PeruPlanDefaults
import pe.kipu.core.domain.plan.PlanEnvelopeTemplates
import pe.kipu.core.domain.plan.defaultTitle
import pe.kipu.core.domain.plan.FinancialPlanIds
import pe.kipu.core.domain.plan.PlanWizardStateLoader
import pe.kipu.core.domain.plan.currency
import pe.kipu.core.domain.plan.PlanWizardLineItem
import pe.kipu.core.domain.repository.CategoryRepository
import pe.kipu.core.domain.repository.CommitmentRepository
import pe.kipu.core.domain.repository.EnvelopeRepository
import pe.kipu.core.domain.repository.FinancialPlanRepository
import pe.kipu.core.domain.time.TimeProvider
import pe.kipu.core.domain.time.CycleRangeCalculator
import pe.kipu.core.domain.usecase.CreateCategoryUseCase
import pe.kipu.core.domain.usecase.CalculateCycleAvailableUseCase
import pe.kipu.core.domain.usecase.CalculateGoalWeeklyContributionUseCase
import pe.kipu.core.domain.usecase.EstimateMonthlyIncomeUseCase
import pe.kipu.core.domain.usecase.ObserveEnvelopeBudgetsUseCase
import pe.kipu.core.domain.usecase.SaveCommitmentUseCase
import pe.kipu.core.domain.usecase.SaveFinancialPlanUseCase
import pe.kipu.core.domain.usecase.ValidateFinancialPlanUseCase
import pe.kipu.core.designsystem.component.formatPenAmountForDisplay
import pe.kipu.core.domain.util.MoneyInputParser

@HiltViewModel
class PlanWizardViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val financialPlanRepository: FinancialPlanRepository,
    private val commitmentRepository: pe.kipu.core.domain.repository.CommitmentRepository,
    private val categoryRepository: CategoryRepository,
    private val observeEnvelopeBudgets: ObserveEnvelopeBudgetsUseCase,
    private val validateFinancialPlan: ValidateFinancialPlanUseCase,
    private val calculateCycleAvailable: CalculateCycleAvailableUseCase,
    private val createCategory: pe.kipu.core.domain.usecase.CreateCategoryUseCase,
    private val calculateGoalWeeklyContribution: pe.kipu.core.domain.usecase.CalculateGoalWeeklyContributionUseCase,
    private val cycleRangeCalculator: CycleRangeCalculator,
    private val timeProvider: TimeProvider,
    private val planWizardSaver: PlanWizardSaver,
    private val fixedExpenseReminderScheduler: pe.kipu.core.domain.notification.FixedExpenseReminderScheduler,
) : ViewModel() {


    private val startStep = planWizardStepFromRoute(savedStateHandle.get<String>("startStep"))

    private val wizardEnvelopeIds = buildSet {
        PlanEnvelopeTemplates.WIZARD_ENVELOPES.forEach { add(it.envelopeId) }
        add(PlanEnvelopeTemplates.ANT_SPENDING_ENVELOPE_ID)
    }

    private val _uiState = MutableStateFlow<PlanWizardUiState>(PlanWizardUiState.Loading)
    val uiState: StateFlow<PlanWizardUiState> = _uiState.asStateFlow()

    private val reloadRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init {
        viewModelScope.launch {
            reloadRequests
                .onStart { emit(Unit) }
                .collect { loadInitialState() }
        }
    }

    fun retryLoad() {
        reloadRequests.tryEmit(Unit)
    }

    private suspend fun loadInitialState() {
        _uiState.value = PlanWizardUiState.Loading
        try {
            val existingPlan = financialPlanRepository.getById(FinancialPlanIds.PRIMARY)
            val allBudgets = observeEnvelopeBudgets().firstWithTimeout(default = emptyList())
            val wizardBudgets = allBudgets.filter { it.envelopeId in wizardEnvelopeIds }
            val customEnvelopeLines = PlanWizardStateLoader.customEnvelopeDefaults(existingPlan, allBudgets)

            val incomeDefaults = PlanWizardStateLoader.incomeDefaults(existingPlan)
            val incomeProfileDefaults = PlanWizardStateLoader.incomeProfileDefaults(existingPlan)
            val fixedDefaults = PlanWizardStateLoader.fixedExpenseDefaults(existingPlan)
            val defaultLimits = buildDefaultEnvelopeLimits(wizardBudgets, existingPlan)
            val emergency = commitmentRepository.getById(CommitmentIds.EMERGENCY_FUND)
            val goalDefaults = PlanWizardStateLoader.goalDefaults(emergency)
            val socialDebt = commitmentRepository.observeCommitments()
                .firstWithTimeout(default = emptyList())
                .firstOrNull {
                    it.type == CommitmentType.SOCIAL_DEBT &&
                        it.id != CommitmentIds.DEMO_SOCIAL_DEBT
                }
            val antLimitFromPlan = existingPlan?.antSpendingLimit
                ?.amount
                ?.stripTrailingZeros()
                ?.toPlainString()
            val categories = categoryRepository.observeCategories()
                .firstWithTimeout(default = emptyList())

            val fixedExpenseFields = FixedExpenseFields(
                skipFixedExpenses = fixedDefaults.skipFixedExpenses,
                electricityText = fixedDefaults.electricityText,
                waterText = fixedDefaults.waterText,
                internetText = fixedDefaults.internetText,
                rentText = fixedDefaults.rentText,
                phoneText = fixedDefaults.phoneText,
                debtsText = fixedDefaults.debtsText,
                educationText = fixedDefaults.educationText,
            )

            val initialAntCategories = existingPlan?.antSpendingTrackedCategoryIds
                .orEmpty()
                .filter { it.startsWith("category-") }
                .toSet()

            _uiState.value = PlanWizardUiState.Content(
                step = startStep,
                isEditingExistingPlan = PlanWizardStateLoader.hasExistingPlan(existingPlan),
                incomeProfile = incomeProfileDefaults.incomeProfile,
                payFrequency = incomeProfileDefaults.payFrequency,
                budgetCycle = existingPlan?.budgetCycle ?: pe.kipu.core.domain.model.BudgetCycle.WEEKLY,
                fixedBaseText = incomeDefaults.fixedBaseText,
                secondQuincenaText = incomeDefaults.secondQuincenaText,
                extraIncomeText = incomeDefaults.extraIncomeText,
                initialBalanceText = incomeDefaults.initialBalanceText,
                approximateIncomeText = incomeDefaults.approximateIncomeText,
                lowWeekText = incomeDefaults.lowWeekText,
                normalWeekText = incomeDefaults.normalWeekText,
                goodWeekText = incomeDefaults.goodWeekText,
                skipFixedExpenses = fixedExpenseFields.skipFixedExpenses,
                electricityText = fixedExpenseFields.electricityText,
                waterText = fixedExpenseFields.waterText,
                internetText = fixedExpenseFields.internetText,
                rentText = fixedExpenseFields.rentText,
                phoneText = fixedExpenseFields.phoneText,
                debtsText = fixedExpenseFields.debtsText,
                educationText = fixedExpenseFields.educationText,
                envelopeLimits = defaultLimits,
                antSpendingLimitText = antLimitFromPlan
                    ?: defaultLimits[PlanEnvelopeTemplates.ANT_SPENDING_ENVELOPE_ID].orEmpty(),
                antSpendingCategories = initialAntCategories,
                antSpendingAlertEnabled = existingPlan?.antSpendingAlertEnabled ?: true,
                categories = categories,
                goalType = goalDefaults.goalType,
                goalName = goalDefaults.goalName,
                goalTargetText = goalDefaults.goalTargetText,
                goalCurrentText = goalDefaults.goalCurrentText,
                goalMonthsText = goalDefaults.goalMonthsText,
                goalSkipped = goalDefaults.goalSkipped,
                hasSocialDebt = socialDebt != null && !socialDebt.isSettled,
                socialDebtCounterparty = socialDebt?.counterpartyName.orEmpty(),
                socialDebtAmountText = socialDebt?.currentAmount?.amount?.stripTrailingZeros()?.toPlainString().orEmpty(),
                budgets = wizardBudgets,
                customEnvelopeLines = customEnvelopeLines,
            )

            viewModelScope.launch {
                runCatching {
                    val refreshedCategories = categoryRepository.observeCategories()
                        .firstWithTimeout(default = categories)
                    if (refreshedCategories != categories) {
                        updateContent { it.copy(categories = refreshedCategories) }
                    }
                    refreshGoalSuggestion()
                    refreshSummaryPreview()
                }
            }
        } catch (_: Exception) {
            _uiState.value = PlanWizardUiState.Error("No pudimos cargar tu plan")
        }
    }

    fun onIncomeProfileSelected(profile: IncomeProfile) {
        updateContent { it.copy(incomeProfile = profile, errorMessage = null) }
        scheduleSummaryRefresh()
    }

    fun onFixedBaseChanged(value: String) = updateContent { it.copy(fixedBaseText = value, errorMessage = null) }.also { scheduleSummaryRefresh() }
    fun onInitialBalanceChanged(value: String) = updateContent { it.copy(initialBalanceText = value, errorMessage = null) }
    fun onSecondQuincenaChanged(value: String) =
        updateContent { it.copy(secondQuincenaText = value, errorMessage = null) }.also { scheduleSummaryRefresh() }
    fun onPayFrequencySelected(frequency: PayFrequency) =
        updateContent { it.copy(payFrequency = frequency, errorMessage = null) }.also { scheduleSummaryRefresh() }

    fun onAddIncomeLine() {
        updateContent {
            it.copy(
                additionalIncomeLines = it.additionalIncomeLines + newWizardLine(),
                errorMessage = null,
            )
        }
    }

    fun onRemoveIncomeLine(lineId: String) {
        updateContent {
            it.copy(
                additionalIncomeLines = it.additionalIncomeLines.filterNot { line -> line.id == lineId },
                errorMessage = null,
            )
        }
        scheduleSummaryRefresh()
    }

    fun onIncomeLineChanged(lineId: String, label: String, amountText: String) {
        updateContent {
            it.copy(
                additionalIncomeLines = it.additionalIncomeLines.map { line ->
                    if (line.id == lineId) line.copy(label = label, amountText = amountText) else line
                },
                errorMessage = null,
            )
        }
        scheduleSummaryRefresh()
    }

    fun onLowWeekChanged(value: String) =
        updateContent { it.copy(lowWeekText = value, errorMessage = null) }.also { scheduleSummaryRefresh() }
    fun onNormalWeekChanged(value: String) =
        updateContent { it.copy(normalWeekText = value, errorMessage = null) }.also { scheduleSummaryRefresh() }
    fun onGoodWeekChanged(value: String) =
        updateContent { it.copy(goodWeekText = value, errorMessage = null) }.also { scheduleSummaryRefresh() }
    fun onApproximateIncomeChanged(value: String) {
        updateContent { it.copy(approximateIncomeText = value, errorMessage = null) }
        scheduleSummaryRefresh()
    }

    fun onBudgetCycleSelected(cycle: pe.kipu.core.domain.model.BudgetCycle) {
        updateContent { it.copy(budgetCycle = cycle) }
        scheduleSummaryRefresh()
    }

    fun onElectricityChanged(value: String) {
        updateContent { it.copy(electricityText = value, errorMessage = null) }
        scheduleSummaryRefresh()
    }
    fun onWaterChanged(value: String) {
        updateContent { it.copy(waterText = value, errorMessage = null) }
        scheduleSummaryRefresh()
    }
    fun onInternetChanged(value: String) {
        updateContent { it.copy(internetText = value, errorMessage = null) }
        scheduleSummaryRefresh()
    }
    fun onRentChanged(value: String) {
        updateContent { it.copy(rentText = value, errorMessage = null) }
        scheduleSummaryRefresh()
    }
    fun onPhoneChanged(value: String) {
        updateContent { it.copy(phoneText = value, errorMessage = null) }
        scheduleSummaryRefresh()
    }
    fun onDebtsChanged(value: String) {
        updateContent { it.copy(debtsText = value, errorMessage = null) }
        scheduleSummaryRefresh()
    }
    fun onEducationChanged(value: String) {
        updateContent { it.copy(educationText = value, errorMessage = null) }
        scheduleSummaryRefresh()
    }

    fun onAddCustomExpenseLine() {
        updateContent {
            it.copy(
                customExpenseLines = it.customExpenseLines + newWizardLine(),
                errorMessage = null,
            )
        }
    }

    fun onRemoveCustomExpenseLine(lineId: String) {
        updateContent {
            it.copy(
                customExpenseLines = it.customExpenseLines.filterNot { line -> line.id == lineId },
                errorMessage = null,
            )
        }
        scheduleSummaryRefresh()
    }

    fun onCustomExpenseLineChanged(lineId: String, label: String, amountText: String) {
        updateContent {
            it.copy(
                customExpenseLines = it.customExpenseLines.map { line ->
                    if (line.id == lineId) line.copy(label = label, amountText = amountText) else line
                },
                errorMessage = null,
            )
        }
        scheduleSummaryRefresh()
    }

    fun onQuickExpenseSelected(label: String) {
        val content = currentContent() ?: return
        if (content.customExpenseLines.any { it.label.equals(label, ignoreCase = true) }) return
        viewModelScope.launch {
            val line = newWizardLine(label = label)
            updateContent {
                it.copy(
                    customExpenseLines = it.customExpenseLines + line,
                    errorMessage = null,
                )
            }
            ensureCategoryForExpenseLine(line.id, label)
            scheduleSummaryRefresh()
        }
    }

    fun onSkipFixedExpenses() {
        updateContent {
            it.copy(
                step = PlanWizardStep.Envelopes,
                skipFixedExpenses = true,
                electricityText = "",
                waterText = "",
                internetText = "",
                rentText = "",
                phoneText = "",
                debtsText = "",
                educationText = "",
                customExpenseLines = emptyList(),
                errorMessage = null,
            )
        }
        scheduleSummaryRefresh()
    }


    fun onEnvelopePresetSelected(envelopeId: String, amount: BigDecimal) {
        updateContent {
            it.copy(
                envelopeLimits = it.envelopeLimits + (envelopeId to amount.stripTrailingZeros().toPlainString()),
                customizingEnvelopeId = null,
                errorMessage = null,
            )
        }
        scheduleSummaryRefresh()
    }

    fun onEnvelopeLimitChanged(envelopeId: String, value: String) {
        updateContent {
            it.copy(
                envelopeLimits = it.envelopeLimits + (envelopeId to value),
                errorMessage = null,
            )
        }
        scheduleSummaryRefresh()
    }

    fun onCustomizeEnvelope(envelopeId: String?) {
        updateContent { it.copy(customizingEnvelopeId = envelopeId) }
    }

    fun onAntSpendingLimitChanged(value: String) {
        updateContent {
            it.copy(
                antSpendingLimitText = value,
                envelopeLimits = it.envelopeLimits + (PlanEnvelopeTemplates.ANT_SPENDING_ENVELOPE_ID to value),
                errorMessage = null,
            )
        }
        scheduleSummaryRefresh()
    }

    fun onAntSpendingPresetSelected(amount: BigDecimal) {
        val text = amount.stripTrailingZeros().toPlainString()
        updateContent {
            it.copy(
                antSpendingLimitText = text,
                envelopeLimits = it.envelopeLimits + (PlanEnvelopeTemplates.ANT_SPENDING_ENVELOPE_ID to text),
                errorMessage = null,
            )
        }
        scheduleSummaryRefresh()
    }

    fun onAntCategoryToggled(categoryId: String) {
        updateContent { content ->
            val updated = if (categoryId in content.antSpendingCategories) {
                content.antSpendingCategories - categoryId
            } else {
                content.antSpendingCategories + categoryId
            }
            content.copy(antSpendingCategories = updated)
        }
    }

    fun onPendingAntCategoryNameChanged(value: String) {
        updateContent { it.copy(pendingAntCategoryName = value, errorMessage = null) }
    }

    fun onAddAntCategory() {
        val name = currentContent()?.pendingAntCategoryName?.trim().orEmpty()
        if (name.isEmpty()) return
        viewModelScope.launch {
            createAndSelectAntCategory(name)
            updateContent { it.copy(pendingAntCategoryName = "", errorMessage = null) }
        }
    }

    fun onQuickAntCategorySelected(name: String) {
        viewModelScope.launch {
            createAndSelectAntCategory(name)
        }
    }

    fun onAntSpendingAlertToggled(enabled: Boolean) {
        updateContent { it.copy(antSpendingAlertEnabled = enabled, errorMessage = null) }
    }

    fun onGoalTypeSelected(type: GoalType) {
        updateContent {
            val name = if (type == GoalType.CUSTOM) {
                if (it.goalName.isBlank() || it.goalName in GoalType.entries.map { g -> g.defaultTitle() }) "" else it.goalName
            } else {
                type.defaultTitle()
            }
            it.copy(
                goalType = type,
                goalName = name,
                goalSkipped = false,
                errorMessage = null,
            )
        }
        refreshGoalSuggestion()
    }


    fun onGoalNameChanged(value: String) = updateContent { it.copy(goalName = value, errorMessage = null) }
    fun onGoalTargetChanged(value: String) {
        updateContent { it.copy(goalTargetText = value, errorMessage = null) }
        refreshGoalSuggestion()
        scheduleSummaryRefresh()
    }

    fun onGoalCurrentChanged(value: String) {
        updateContent { it.copy(goalCurrentText = value, errorMessage = null) }
        refreshGoalSuggestion()
        scheduleSummaryRefresh()
    }

    fun onGoalMonthsChanged(value: String) {
        updateContent { it.copy(goalMonthsText = value, errorMessage = null) }
        refreshGoalSuggestion()
    }

    fun onSkipGoal() {
        updateContent { it.copy(goalSkipped = true, errorMessage = null) }
        viewModelScope.launch {
            refreshSummaryPreview()
            updateContent { it.copy(step = PlanWizardStep.Summary, errorMessage = null) }
        }
    }

    fun onSocialDebtToggled(enabled: Boolean) {
        updateContent {
            it.copy(
                hasSocialDebt = enabled,
                socialDebtCounterparty = if (enabled) it.socialDebtCounterparty else "",
                socialDebtAmountText = if (enabled) it.socialDebtAmountText else "",
                errorMessage = null,
            )
        }
        viewModelScope.launch { refreshSummaryPreview() }
    }

    fun onSocialDebtCounterpartyChanged(value: String) {
        updateContent { it.copy(socialDebtCounterparty = value, errorMessage = null) }
        viewModelScope.launch { refreshSummaryPreview() }
    }

    fun onSocialDebtAmountChanged(value: String) {
        updateContent { it.copy(socialDebtAmountText = value, errorMessage = null) }
        viewModelScope.launch { refreshSummaryPreview() }
    }

    fun onSkipApproximate() {
        updateContent {
            val text = it.approximateIncomeText.ifBlank {
                PeruPlanDefaults.SEED_MONTHLY_FIXED.stripTrailingZeros().toPlainString()
            }
            it.copy(approximateIncomeText = text, errorMessage = null)
        }
        scheduleSummaryRefresh()
        onContinue()
    }

    fun onBack() {
        updateContent { content ->
            content.copy(
                step = when (content.step) {
                    PlanWizardStep.Income -> PlanWizardStep.Income
                    PlanWizardStep.FixedExpenses -> PlanWizardStep.Income
                    PlanWizardStep.Envelopes -> PlanWizardStep.FixedExpenses
                    PlanWizardStep.AntSpending -> PlanWizardStep.Envelopes
                    PlanWizardStep.Goal -> PlanWizardStep.AntSpending
                    PlanWizardStep.Summary -> PlanWizardStep.Goal
                },
                errorMessage = null,
            )
        }
    }

    fun onNavigateToStep(step: PlanWizardStep) {
        updateContent { it.copy(step = step, errorMessage = null) }
        if (step == PlanWizardStep.Summary) {
            scheduleSummaryRefresh()
        }
    }

    fun onContinue() {
        val content = currentContent() ?: return
        when (content.step) {
            PlanWizardStep.Income -> {
                val income = planWizardSaver.parseMonthlyIncome(content)
                if (income == null || income.isZero()) {
                    updateContent { it.copy(errorMessage = "Ingresa un monto de ingreso válido") }
                    return
                }
                updateContent { it.copy(step = PlanWizardStep.FixedExpenses, errorMessage = null) }
            }

            PlanWizardStep.FixedExpenses -> {
                if (!content.skipFixedExpenses) {
                    val fixed = planWizardSaver.parseFixedExpenses(content)
                    if (fixed == null) {
                        updateContent { it.copy(errorMessage = "Revisa el desglose de gastos fijos") }
                        return
                    }
                }
                viewModelScope.launch {
                    syncCustomExpenseCategories()
                    updateContent { it.copy(step = PlanWizardStep.Envelopes, errorMessage = null) }
                }
            }

            PlanWizardStep.Envelopes -> {
                val limitsValidation = planWizardSaver.validateLimits(content.envelopeLimits, content.budgets)
                if (limitsValidation != null) {
                    updateContent { it.copy(errorMessage = limitsValidation) }
                    return
                }
                val defaultAntLimit = if (content.antSpendingLimitText.isBlank()) {
                    PlanEnvelopeTemplates.antSpendingPresetsForCycle(content.budgetCycle)[1].stripTrailingZeros().toPlainString()
                } else content.antSpendingLimitText
                updateContent {
                    it.copy(
                        step = PlanWizardStep.AntSpending,
                        antSpendingLimitText = defaultAntLimit,
                        envelopeLimits = it.envelopeLimits + (PlanEnvelopeTemplates.ANT_SPENDING_ENVELOPE_ID to defaultAntLimit),
                        errorMessage = null,
                    )
                }
                scheduleSummaryRefresh()
            }

            PlanWizardStep.AntSpending -> {
                if (!planWizardSaver.validateAntSpending(content)) {
                    updateContent { it.copy(errorMessage = "Ingresa un límite válido para gastos hormiga") }
                    return
                }
                updateContent { it.copy(step = PlanWizardStep.Goal, errorMessage = null) }
                refreshGoalSuggestion()
                scheduleSummaryRefresh()
            }

            PlanWizardStep.Goal -> {
                if (!content.goalSkipped) {
                    val target = planWizardSaver.parseGoalAmount(content.goalTargetText)
                    if (target == null || target.isZero()) {
                        updateContent { it.copy(errorMessage = "Ingresa cuánto necesitas para tu meta") }
                        return
                    }
                }
                if (content.hasSocialDebt) {
                    val socialDebtResult = planWizardSaver.validateSocialDebt(content)
                    if (socialDebtResult != null) {
                        updateContent { it.copy(errorMessage = socialDebtResult) }
                        return
                    }
                }
                viewModelScope.launch {
                    refreshSummaryPreview()
                    updateContent { it.copy(step = PlanWizardStep.Summary, errorMessage = null) }
                }
            }

            PlanWizardStep.Summary -> Unit
        }
    }

    fun onFinish(onFinished: () -> Unit) {
        viewModelScope.launch {
            updateContent { it.copy(isSaving = true, errorMessage = null) }
            refreshSummaryPreview()
            val content = currentContent() ?: return@launch
            val saveResult = planWizardSaver.save(content)
            when (saveResult) {
                is PlanWizardSaver.Result.Success -> {
                    val fixedItems = mutableListOf<String>()
                    if (content.electricityText.isNotBlank()) fixedItems.add("Luz")
                    if (content.waterText.isNotBlank()) fixedItems.add("Agua")
                    if (content.internetText.isNotBlank()) fixedItems.add("Internet")
                    if (content.rentText.isNotBlank()) fixedItems.add("Alquiler")
                    if (content.phoneText.isNotBlank()) fixedItems.add("Celular")
                    if (content.debtsText.isNotBlank()) fixedItems.add("Deudas")
                    if (content.educationText.isNotBlank()) fixedItems.add("Educación")
                    content.customExpenseLines.forEach { line ->
                        if (line.amountText.isNotBlank()) fixedItems.add(line.label.ifBlank { "Gasto fijo" })
                    }

                    if (fixedItems.isNotEmpty() && !content.skipFixedExpenses) {
                        val itemsSummary = "Tienes pagos obligatorios este ciclo: ${fixedItems.joinToString(", ")}"
                        fixedExpenseReminderScheduler.schedulePaymentReminders(
                            itemsSummary = itemsSummary,
                            isBiweekly = content.payFrequency == PayFrequency.BIWEEKLY,
                        )
                    } else {
                        fixedExpenseReminderScheduler.cancelReminders()
                    }

                    refreshSummaryPreview(saveResult.validation)
                    updateContent { it.copy(isSaving = false, validation = saveResult.validation) }
                    onFinished()
                }
                is PlanWizardSaver.Result.Error -> {
                    updateContent { it.copy(isSaving = false, errorMessage = saveResult.message) }
                }
            }
        }
    }


    private fun refreshGoalSuggestion() {
        val content = currentContent() ?: return
        val months = content.goalMonthsText.toIntOrNull() ?: 5
        val weekly = when (
            val result = calculateGoalWeeklyContribution.invoke(
                content.goalTargetText,
                content.goalCurrentText,
                months,
            )
        ) {
            is DomainResult.Ok -> result.value
            is DomainResult.Err -> null
        }
        updateContent { it.copy(suggestedGoalWeekly = weekly) }
    }

    private suspend fun refreshSummaryPreview(validation: FinancialPlanValidationResult? = null) {
        val content = currentContent() ?: return
        val previewBudgets = buildPreviewBudgets(content)
        val income = planWizardSaver.parseMonthlyIncome(content)
        val fixedExpenses = planWizardSaver.parseFixedExpenses(content)

        val planBreakdown = if (income != null && fixedExpenses != null) {
            val envelopes = previewBudgets.map { it.toEnvelope() }
            val commitments = buildPreviewCommitments(content)
            val plan = FinancialPlan(
                id = FinancialPlanIds.PRIMARY,
                estimatedMonthlyIncome = income,
                fixedExpenses = fixedExpenses,
                envelopeIds = emptyList(),
                budgetCycle = content.budgetCycle,
            )
            validateFinancialPlan.analyze(plan, envelopes, commitments)
        } else {
            null
        }

        val previewValidation = planBreakdown?.validation ?: validation
        val monthlyEnvelope = planBreakdown?.monthlyEnvelopeReserve
            ?: projectCycleTotalToMonthly(
                previewBudgets.fold(Money.ZERO) { acc, budget -> acc + budget.weeklyLimit },
                content.budgetCycle,
            )
        val monthlyExtra = planBreakdown?.monthlySurplus

        val referenceInstant = timeProvider.now()
        val cycleRange = cycleRangeCalculator.currentCycleRange(content.budgetCycle, referenceInstant)
        val daily = calculateCycleAvailable(previewBudgets, referenceInstant, cycleRange, content.budgetCycle)

        updateContent {
            it.copy(
                previewBudgets = previewBudgets,
                cycleAvailable = daily,
                validation = previewValidation,
                monthlyEnvelopeTotal = monthlyEnvelope,
                monthlyExtraAvailable = monthlyExtra,
            )
        }
    }

    private suspend fun buildPreviewCommitments(content: PlanWizardUiState.Content): List<Commitment> {
        val stored = commitmentRepository.observeCommitments().firstWithTimeout(emptyList())
            .filterNot {
                it.type == CommitmentType.SOCIAL_DEBT ||
                    it.id == CommitmentIds.EMERGENCY_FUND ||
                    it.id == CommitmentIds.DEMO_SOCIAL_DEBT
            }

        val goalPreview = if (content.goalSkipped) {
            emptyList()
        } else {
            val target = MoneyInputParser.parsePen(content.goalTargetText)
            val current = MoneyInputParser.parsePen(content.goalCurrentText)
            listOf(
                Commitment(
                    id = CommitmentIds.EMERGENCY_FUND,
                    type = CommitmentType.SAVINGS_GOAL,
                    title = content.goalName.ifBlank { content.goalType.defaultTitle() },
                    targetAmount = (target as? DomainResult.Ok)?.value,
                    currentAmount = (current as? DomainResult.Ok)?.value ?: Money.ZERO,
                    dueDate = null,
                    counterpartyName = null,
                    isSettled = false,
                    currencyCode = content.goalType.currency().code,
                    savingsHorizonMonths = content.goalMonthsText.toIntOrNull() ?: 5,
                ),
            )
        }

        val socialDebtPreview = if (content.hasSocialDebt) {
            val amount = MoneyInputParser.parsePen(content.socialDebtAmountText)
            listOf(
                Commitment(
                    id = CommitmentIds.PRIMARY_SOCIAL_DEBT,
                    type = CommitmentType.SOCIAL_DEBT,
                    title = content.socialDebtCounterparty.ifBlank { "Deuda social" },
                    targetAmount = null,
                    currentAmount = (amount as? DomainResult.Ok)?.value ?: Money.ZERO,
                    dueDate = null,
                    counterpartyName = content.socialDebtCounterparty.takeIf { it.isNotBlank() },
                    isSettled = false,
                ),
            )
        } else {
            emptyList()
        }

        return stored + goalPreview + socialDebtPreview
    }

    private fun buildPreviewBudgets(content: PlanWizardUiState.Content): List<EnvelopeBudgetState> {
        val antLimit = content.antSpendingLimitText
        val antBudget = if (antLimit.isNotBlank()) {
            val parsedLimit = when (val parsed = MoneyInputParser.parsePen(antLimit)) {
                is DomainResult.Ok -> parsed.value
                is DomainResult.Err -> Money.ZERO
            }
            EnvelopeBudgetState(
                envelopeId = PlanEnvelopeTemplates.ANT_SPENDING_ENVELOPE_ID,
                name = "Gastos hormiga",
                categoryId = "category-other",
                weeklyLimit = parsedLimit,
                spentAmount = Money.ZERO,
                remainingAmount = parsedLimit,
                percentUsed = 0,
                status = EnvelopeBudgetStatus.OK,
            )
        } else null

        val storedBudgets = content.budgets.mapNotNull { budget ->
            val limitText = when (budget.envelopeId) {
                PlanEnvelopeTemplates.ANT_SPENDING_ENVELOPE_ID -> null
                else -> content.envelopeLimits[budget.envelopeId]
            }
            if (limitText.isNullOrBlank()) null else applyLimit(budget, limitText)
        }

        val templateBudgets = PlanEnvelopeTemplates.WIZARD_ENVELOPES
            .filterNot { template -> storedBudgets.any { it.envelopeId == template.envelopeId } }
            .mapNotNull { template ->
                val limitText = content.envelopeLimits[template.envelopeId]
                if (limitText.isNullOrBlank()) return@mapNotNull null
                buildTemplateBudgetState(
                    envelopeId = template.envelopeId,
                    name = template.name,
                    categoryId = planWizardSaver.categoryIdForWizardEnvelope(template.envelopeId),
                    limitText = limitText,
                )
            }

        val customBudgets = content.customEnvelopeLines.mapNotNull { line ->
            if (line.amountText.isBlank()) return@mapNotNull null
            val limit = when (val parsed = MoneyInputParser.parsePen(line.amountText)) {
                is DomainResult.Ok -> parsed.value
                is DomainResult.Err -> Money.ZERO
            }
            EnvelopeBudgetState(
                envelopeId = line.id,
                name = line.label.ifBlank { "Personalizado" },
                categoryId = line.categoryId ?: "category-other",
                weeklyLimit = limit,
                spentAmount = Money.ZERO,
                remainingAmount = limit,
                percentUsed = 0,
                status = EnvelopeBudgetStatus.OK,
            )
        }

        val nonAntBudgets = storedBudgets + templateBudgets
        return (listOfNotNull(antBudget) + nonAntBudgets + customBudgets)
    }

    private fun applyLimit(budget: EnvelopeBudgetState, limitText: String): EnvelopeBudgetState =
        when (val parsed = MoneyInputParser.parsePen(limitText)) {
            is DomainResult.Ok -> {
                val limit = parsed.value
                val remaining = budget.spentAmount.let { spent ->
                    when (val diff = limit.minus(spent)) {
                        is DomainResult.Ok -> diff.value
                        is DomainResult.Err -> Money.ZERO
                    }
                }
                budget.copy(weeklyLimit = limit, remainingAmount = remaining)
            }

            is DomainResult.Err -> budget
        }

    private fun buildTemplateBudgetState(
        envelopeId: String,
        name: String,
        categoryId: String,
        limitText: String,
    ): EnvelopeBudgetState? {
        val limit = when (val parsed = MoneyInputParser.parsePen(limitText)) {
            is DomainResult.Ok -> parsed.value
            is DomainResult.Err -> return null
        }
        return EnvelopeBudgetState(
            envelopeId = envelopeId,
            name = name,
            categoryId = categoryId,
            weeklyLimit = limit,
            spentAmount = Money.ZERO,
            remainingAmount = limit,
            percentUsed = 0,
            status = EnvelopeBudgetStatus.OK,
        )
    }


    private fun buildDefaultEnvelopeLimits(
        budgets: List<EnvelopeBudgetState>,
        plan: FinancialPlan?,
    ): Map<String, String> {
        val defaults = mutableMapOf<String, String>()
        if (plan == null) {
            return defaults
        }
        PlanEnvelopeTemplates.WIZARD_ENVELOPES.forEach { template ->
            val existing = budgets.find { it.envelopeId == template.envelopeId }
            if (existing != null) {
                defaults[template.envelopeId] = existing.weeklyLimit.amount.stripTrailingZeros().toPlainString()
            }
        }
        val antFromBudget = budgets.find { it.envelopeId == PlanEnvelopeTemplates.ANT_SPENDING_ENVELOPE_ID }
            ?.weeklyLimit?.amount
        val antAmount = plan.antSpendingLimit?.amount ?: antFromBudget
        if (antAmount != null) {
            defaults[PlanEnvelopeTemplates.ANT_SPENDING_ENVELOPE_ID] = antAmount.stripTrailingZeros().toPlainString()
        }
        return defaults
    }

    private fun projectCycleTotalToMonthly(
        cycleTotal: Money,
        cycle: pe.kipu.core.domain.model.BudgetCycle,
    ): Money {
        val factor = when (cycle) {
            pe.kipu.core.domain.model.BudgetCycle.DAILY -> 30L
            pe.kipu.core.domain.model.BudgetCycle.WEEKLY -> 4L
            pe.kipu.core.domain.model.BudgetCycle.MONTHLY -> 1L
        }
        if (factor == 1L) return cycleTotal
        val product = cycleTotal.amount.multiply(BigDecimal.valueOf(factor))
        return when (val result = Money.of(product)) {
            is DomainResult.Ok -> result.value
            is DomainResult.Err -> Money.ZERO
        }
    }

    private fun EnvelopeBudgetState.toEnvelope(): Envelope = Envelope(
        id = envelopeId,
        name = name,
        weeklyLimit = weeklyLimit,
        categoryId = categoryId,
    )

    private fun scheduleSummaryRefresh() {
        viewModelScope.launch { refreshSummaryPreview() }
    }

    private fun currentContent(): PlanWizardUiState.Content? = _uiState.value as? PlanWizardUiState.Content

    private data class FixedExpenseFields(
        val skipFixedExpenses: Boolean,
        val electricityText: String,
        val waterText: String,
        val internetText: String,
        val rentText: String,
        val phoneText: String,
        val debtsText: String,
        val educationText: String,
    )


    fun onAddCustomEnvelopeLine() {
        updateContent {
            it.copy(
                customEnvelopeLines = it.customEnvelopeLines + newWizardLine(),
                errorMessage = null,
            )
        }
    }

    fun onRemoveCustomEnvelopeLine(lineId: String) {
        updateContent {
            it.copy(
                customEnvelopeLines = it.customEnvelopeLines.filterNot { line -> line.id == lineId },
                errorMessage = null,
            )
        }
        scheduleSummaryRefresh()
    }

    fun onCustomEnvelopeLineChanged(lineId: String, label: String, amountText: String) {
        updateContent {
            it.copy(
                customEnvelopeLines = it.customEnvelopeLines.map { line ->
                    if (line.id == lineId) line.copy(label = label, amountText = amountText) else line
                },
                errorMessage = null,
            )
        }
        scheduleSummaryRefresh()
    }

    private fun updateContent(transform: (PlanWizardUiState.Content) -> PlanWizardUiState.Content) {
        _uiState.update { current ->
            (current as? PlanWizardUiState.Content)?.let(transform) ?: current
        }
    }

    private fun newWizardLine(label: String = ""): PlanWizardLineItem =
        PlanWizardLineItem(
            id = "line-${timeProvider.now().toEpochMilli()}",
            label = label,
            amountText = "",
        )

    private suspend fun ensureCategoryForExpenseLine(lineId: String, name: String) {
        createCategory(name).onSuccess { category ->
            val categories = categoryRepository.observeCategories()
                .firstWithTimeout(currentContent()?.categories.orEmpty())
            updateContent { content ->
                content.copy(
                    categories = categories,
                    customExpenseLines = content.customExpenseLines.map { line ->
                        if (line.id == lineId) {
                            line.copy(categoryId = category.id, label = category.name)
                        } else {
                            line
                        }
                    },
                )
            }
        }
    }

    private suspend fun syncCustomExpenseCategories() {
        val content = currentContent() ?: return
        for (line in content.customExpenseLines) {
            val name = line.label.trim()
            if (name.isNotEmpty() && line.categoryId == null) {
                ensureCategoryForExpenseLine(line.id, name)
            }
        }
    }

    private suspend fun createAndSelectAntCategory(name: String) {
        createCategory(name).onSuccess { category ->
            val categories = categoryRepository.observeCategories()
                .firstWithTimeout(currentContent()?.categories.orEmpty())
            updateContent { content ->
                content.copy(
                    categories = categories,
                    antSpendingCategories = content.antSpendingCategories + category.id,
                )
            }
        }.onFailure {
            updateContent { it.copy(errorMessage = "No pudimos crear la categoría") }
        }
    }
}
