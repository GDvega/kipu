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
import pe.kipu.core.domain.model.UserPreferences
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
import pe.kipu.core.domain.repository.UserPreferencesRepository
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
    private val commitmentRepository: CommitmentRepository,
    private val categoryRepository: CategoryRepository,
    private val envelopeRepository: EnvelopeRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val observeEnvelopeBudgets: ObserveEnvelopeBudgetsUseCase,
    private val saveFinancialPlan: SaveFinancialPlanUseCase,
    private val saveCommitment: SaveCommitmentUseCase,
    private val validateFinancialPlan: ValidateFinancialPlanUseCase,
    private val calculateCycleAvailable: CalculateCycleAvailableUseCase,
    private val estimateMonthlyIncome: EstimateMonthlyIncomeUseCase,
    private val createCategory: CreateCategoryUseCase,
    private val createEnvelope: pe.kipu.core.domain.usecase.CreateEnvelopeUseCase,
    private val calculateGoalWeeklyContribution: CalculateGoalWeeklyContributionUseCase,
    private val cycleRangeCalculator: CycleRangeCalculator,
    private val timeProvider: TimeProvider,
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
            val preferences = userPreferencesRepository.observePreferences()
                .firstWithTimeout(default = UserPreferences())
            val existingPlan = financialPlanRepository.getById(FinancialPlanIds.PRIMARY)
            val allBudgets = observeEnvelopeBudgets().firstWithTimeout(default = emptyList())
            val wizardBudgets = allBudgets.filter { it.envelopeId in wizardEnvelopeIds }

            val incomeDefaults = PlanWizardStateLoader.incomeDefaults(existingPlan)
            val incomeProfileDefaults = PlanWizardStateLoader.incomeProfileDefaults(existingPlan)
            val fixedDefaults = PlanWizardStateLoader.fixedExpenseDefaults(existingPlan)
            val defaultLimits = buildDefaultEnvelopeLimits(wizardBudgets, preferences)
            val emergency = commitmentRepository.getById(CommitmentIds.EMERGENCY_FUND)
            val goalDefaults = PlanWizardStateLoader.goalDefaults(emergency)
            val socialDebt = commitmentRepository.observeCommitments()
                .firstWithTimeout(default = emptyList())
                .firstOrNull {
                    it.type == CommitmentType.SOCIAL_DEBT &&
                        it.id != CommitmentIds.DEMO_SOCIAL_DEBT
                }
            val antLimitFromPrefs = preferences.antSpendingWeeklyLimitCents?.let { cents ->
                BigDecimal.valueOf(cents).movePointLeft(2).stripTrailingZeros().toPlainString()
            }
            val categories = categoryRepository.observeCategories()
                .firstWithTimeout(default = emptyList())

            val fixedExpenseFields = FixedExpenseFields(
                skipFixedExpenses = fixedDefaults.skipFixedExpenses,
                educationText = fixedDefaults.educationText,
                rentText = fixedDefaults.rentText,
                utilitiesText = fixedDefaults.utilitiesText,
                phoneText = fixedDefaults.phoneText,
                debtsText = fixedDefaults.debtsText,
            )

            val initialAntCategories = preferences.antSpendingTrackedCategories
                .filter { it.startsWith("category-") }
                .toSet()

            _uiState.value = PlanWizardUiState.Content(
                step = startStep,
                isEditingExistingPlan = PlanWizardStateLoader.hasExistingPlan(existingPlan),
                incomeProfile = incomeProfileDefaults.incomeProfile,
                payFrequency = incomeProfileDefaults.payFrequency,
                budgetCycle = existingPlan?.budgetCycle ?: pe.kipu.core.domain.model.BudgetCycle.WEEKLY,
                fixedBaseText = incomeDefaults.fixedBaseText,
                initialBalanceText = incomeDefaults.initialBalanceText,
                approximateIncomeText = incomeDefaults.approximateIncomeText,
                lowWeekText = "",
                normalWeekText = "",
                goodWeekText = "",
                skipFixedExpenses = fixedExpenseFields.skipFixedExpenses,
                educationText = fixedExpenseFields.educationText,
                rentText = fixedExpenseFields.rentText,
                utilitiesText = fixedExpenseFields.utilitiesText,
                phoneText = fixedExpenseFields.phoneText,
                debtsText = fixedExpenseFields.debtsText,
                envelopeLimits = defaultLimits,
                antSpendingLimitText = antLimitFromPrefs
                    ?: defaultLimits[PlanEnvelopeTemplates.ANT_SPENDING_ENVELOPE_ID]
                    ?: "",
                antSpendingCategories = initialAntCategories,
                antSpendingAlertEnabled = preferences.antSpendingAlertEnabled,
                categories = categories,
                goalName = goalDefaults.goalName,
                goalTargetText = goalDefaults.goalTargetText,
                goalCurrentText = goalDefaults.goalCurrentText,
                goalSkipped = goalDefaults.goalSkipped,
                hasSocialDebt = socialDebt != null && !socialDebt.isSettled,
                socialDebtCounterparty = socialDebt?.counterpartyName.orEmpty(),
                socialDebtAmountText = socialDebt?.currentAmount?.amount?.stripTrailingZeros()?.toPlainString().orEmpty(),
                budgets = wizardBudgets,
            )
            viewModelScope.launch {
                runCatching {
                    val migratedAntCategories = migrateAntSpendingCategoryIds(
                        stored = preferences.antSpendingTrackedCategories,
                    )
                    if (migratedAntCategories != initialAntCategories) {
                        updateContent { it.copy(antSpendingCategories = migratedAntCategories) }
                    }
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

    fun onLowWeekChanged(value: String) = updateContent { it.copy(lowWeekText = value, errorMessage = null) }
    fun onNormalWeekChanged(value: String) = updateContent { it.copy(normalWeekText = value, errorMessage = null) }
    fun onGoodWeekChanged(value: String) = updateContent { it.copy(goodWeekText = value, errorMessage = null) }
    fun onApproximateIncomeChanged(value: String) {
        updateContent { it.copy(approximateIncomeText = value, errorMessage = null) }
        scheduleSummaryRefresh()
    }

    fun onBudgetCycleSelected(cycle: pe.kipu.core.domain.model.BudgetCycle) {
        updateContent { it.copy(budgetCycle = cycle) }
        scheduleSummaryRefresh()
    }

    fun onEducationChanged(value: String) {
        updateContent { it.copy(educationText = value, errorMessage = null) }
        scheduleSummaryRefresh()
    }
    fun onRentChanged(value: String) {
        updateContent { it.copy(rentText = value, errorMessage = null) }
        scheduleSummaryRefresh()
    }
    fun onUtilitiesChanged(value: String) {
        updateContent { it.copy(utilitiesText = value, errorMessage = null) }
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
                skipFixedExpenses = true,
                educationText = "",
                rentText = "",
                utilitiesText = "",
                phoneText = "",
                debtsText = "",
                customExpenseLines = emptyList(),
                errorMessage = null,
            )
        }
        scheduleSummaryRefresh()
        onContinue()
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
            it.copy(
                goalType = type,
                goalName = type.defaultTitle(),
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
                if (!validateIncome(content)) return
                updateContent { it.copy(step = PlanWizardStep.FixedExpenses, errorMessage = null) }
            }

            PlanWizardStep.FixedExpenses -> {
                if (!content.skipFixedExpenses && !validateFixedExpenses(content)) return
                viewModelScope.launch {
                    syncCustomExpenseCategories()
                    updateContent { it.copy(step = PlanWizardStep.Envelopes, errorMessage = null) }
                }
            }

            PlanWizardStep.Envelopes -> {
                if (!validateEnvelopeLimits(content)) return
                updateContent { it.copy(step = PlanWizardStep.AntSpending, errorMessage = null) }
            }

            PlanWizardStep.AntSpending -> {
                if (!validateAntSpending(content)) return
                updateContent { it.copy(step = PlanWizardStep.Goal, errorMessage = null) }
                refreshGoalSuggestion()
                scheduleSummaryRefresh()
            }

            PlanWizardStep.Goal -> {
                if (!content.goalSkipped && !validateGoal(content)) return
                if (content.hasSocialDebt && !validateSocialDebt(content)) return
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
            if (persistPlan()) {
                onFinished()
            }
        }
    }

    private suspend fun persistPlan(): Boolean {
        val content = currentContent() ?: return false
        if (!validateIncome(content)) return false
        if (!content.skipFixedExpenses && !validateFixedExpenses(content)) return false
        if (!validateEnvelopeLimits(content)) return false
        if (!validateAntSpending(content)) return false

        refreshSummaryPreview()
        val refreshed = currentContent() ?: return false
        if (refreshed.validation is FinancialPlanValidationResult.Invalid) {
            val deficit = refreshed.validation.deficit
            updateContent {
                it.copy(
                    errorMessage = "Tu plan no cuadra. Ajusta montos antes de guardar. " +
                        "Faltan ${formatPenAmountForDisplay(deficit.amount)} para el mes.",
                )
            }
            return false
        }

        val income = parseMonthlyIncome(refreshed) ?: return false
        val fixedExpenses = parseFixedExpenses(refreshed) ?: return false
        val initialBalance = when (val parsed = MoneyInputParser.parsePen(refreshed.initialBalanceText)) {
            is DomainResult.Ok -> parsed.value
            is DomainResult.Err -> Money.ZERO
        }

        updateContent { it.copy(isSaving = true, errorMessage = null) }

        val wizardLimits = refreshed.envelopeLimits + mapOf(
            PlanEnvelopeTemplates.ANT_SPENDING_ENVELOPE_ID to refreshed.antSpendingLimitText,
        )

        for ((envelopeId, limitText) in wizardLimits) {
            when (val limit = MoneyInputParser.parsePen(limitText)) {
                is DomainResult.Err -> {
                    updateContent { it.copy(isSaving = false, errorMessage = "Revisa los límites de sobres") }
                    return false
                }

                is DomainResult.Ok -> {
                    if (!saveWizardEnvelopeLimit(envelopeId, limit.value)) return false
                }
            }
        }

        for (line in refreshed.customEnvelopeLines) {
            val limit = when (val parsed = MoneyInputParser.parsePen(line.amountText)) {
                is DomainResult.Err -> {
                    updateContent { it.copy(isSaving = false, errorMessage = "Revisa los límites de sobres") }
                    return false
                }

                is DomainResult.Ok -> parsed.value
            }
            val category = createCategory(line.label).getOrNull()
            if (category != null) {
                createEnvelope(
                    name = line.label,
                    categoryId = category.id,
                    weeklyLimit = limit,
                ).onFailure {
                    updateContent { it.copy(isSaving = false, errorMessage = "No pudimos guardar tus sobres") }
                    return false
                }
            }
        }

        if (!refreshed.goalSkipped) {
            if (!saveGoal(refreshed)) return false
        }

        if (!saveSocialDebt(refreshed)) return false

        if (!saveAntSpendingPreferences(refreshed)) return false

        val result = saveFinancialPlan(
            planId = FinancialPlanIds.PRIMARY,
            estimatedMonthlyIncome = income,
            fixedExpenses = fixedExpenses,
            initialBalance = initialBalance,
            incomeProfile = refreshed.incomeProfile,
            payFrequency = refreshed.payFrequency,
            budgetCycle = refreshed.budgetCycle,
        )

        return result.fold(
            onSuccess = { saveResult ->
                refreshSummaryPreview(saveResult.validation)
                updateContent { it.copy(isSaving = false, validation = saveResult.validation) }
                true
            },
            onFailure = {
                updateContent { it.copy(isSaving = false, errorMessage = "No pudimos guardar tu plan") }
                false
            },
        )
    }

    private suspend fun saveGoal(content: PlanWizardUiState.Content): Boolean {
        val target = parseGoalAmount(content.goalTargetText)
        if (target == null || target.isZero()) {
            updateContent { it.copy(isSaving = false, errorMessage = "Revisa el monto de tu meta") }
            return false
        }

        val current = parseGoalAmount(content.goalCurrentText) ?: Money.ZERO

        val months = content.goalMonthsText.toIntOrNull() ?: 5
        val existing = commitmentRepository.getById(CommitmentIds.EMERGENCY_FUND)

        return saveCommitment(
            existingId = existing?.id ?: CommitmentIds.EMERGENCY_FUND,
            type = CommitmentType.SAVINGS_GOAL,
            title = content.goalName.ifBlank { content.goalType.defaultTitle() },
            targetAmount = target,
            currentAmount = current,
            savingsHorizonMonths = months,
        ).fold(
            onSuccess = { true },
            onFailure = {
                updateContent { it.copy(isSaving = false, errorMessage = "No pudimos guardar tu meta") }
                false
            },
        )
    }

    private suspend fun saveSocialDebt(content: PlanWizardUiState.Content): Boolean {
        if (!content.hasSocialDebt) {
            val existing = commitmentRepository.getById(CommitmentIds.PRIMARY_SOCIAL_DEBT)
                ?: commitmentRepository.observeCommitments().firstWithTimeout(emptyList())
                    .firstOrNull { it.type == CommitmentType.SOCIAL_DEBT }
            if (existing != null) {
                return commitmentRepository.save(existing.copy(isSettled = true)).fold(
                    onSuccess = { true },
                    onFailure = {
                        updateContent { it.copy(isSaving = false, errorMessage = "No pudimos actualizar tu deuda social") }
                        false
                    },
                )
            }
            return true
        }

        val amount = when (val parsed = MoneyInputParser.parsePen(content.socialDebtAmountText)) {
            is DomainResult.Ok -> parsed.value
            is DomainResult.Err -> {
                updateContent { it.copy(isSaving = false, errorMessage = "Revisa el monto de tu deuda social") }
                return false
            }
        }
        if (amount.isZero()) {
            updateContent { it.copy(isSaving = false, errorMessage = "Ingresa el monto que debes") }
            return false
        }
        val counterparty = content.socialDebtCounterparty.trim()
        if (counterparty.isEmpty()) {
            updateContent { it.copy(isSaving = false, errorMessage = "Ingresa a quién le debes") }
            return false
        }

        val existing = commitmentRepository.getById(CommitmentIds.PRIMARY_SOCIAL_DEBT)
            ?: commitmentRepository.observeCommitments().firstWithTimeout(emptyList())
                .firstOrNull { it.type == CommitmentType.SOCIAL_DEBT }

        return saveCommitment(
            existingId = existing?.id ?: CommitmentIds.PRIMARY_SOCIAL_DEBT,
            type = CommitmentType.SOCIAL_DEBT,
            title = "Deuda con $counterparty",
            currentAmount = amount,
            counterpartyName = counterparty,
        ).fold(
            onSuccess = { true },
            onFailure = {
                updateContent { it.copy(isSaving = false, errorMessage = "No pudimos guardar tu deuda social") }
                false
            },
        )
    }

    private suspend fun saveAntSpendingPreferences(content: PlanWizardUiState.Content): Boolean {
        val limitCents = when (val parsed = MoneyInputParser.parsePen(content.antSpendingLimitText)) {
            is DomainResult.Ok -> parsed.value.amount.movePointRight(2).longValueExact()
            is DomainResult.Err -> null
        }
        return userPreferencesRepository.updatePreferences { prefs ->
            prefs.copy(
                antSpendingWeeklyLimitCents = limitCents,
                antSpendingAlertEnabled = content.antSpendingAlertEnabled,
                antSpendingAlertPercent = 80,
                antSpendingTrackedCategories = content.antSpendingCategories,
            )
        }.fold(
            onSuccess = { true },
            onFailure = {
                updateContent { it.copy(isSaving = false, errorMessage = "No pudimos guardar tus preferencias") }
                false
            },
        )
    }

    private fun parseGoalAmount(text: String): Money? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return Money.ZERO
        return when (val result = MoneyInputParser.parsePen(trimmed)) {
            is DomainResult.Ok -> result.value
            is DomainResult.Err -> null
        }
    }

    private fun validateIncome(content: PlanWizardUiState.Content): Boolean {
        val income = parseMonthlyIncome(content)
        if (income == null || income.isZero()) {
            updateContent { it.copy(errorMessage = "Ingresa un monto de ingreso válido") }
            return false
        }
        return true
    }

    private fun validateFixedExpenses(content: PlanWizardUiState.Content): Boolean {
        val fixed = parseFixedExpenses(content)
        if (fixed == null) {
            updateContent { it.copy(errorMessage = "Revisa el desglose de gastos fijos") }
            return false
        }
        return true
    }

    private fun validateEnvelopeLimits(content: PlanWizardUiState.Content): Boolean =
        validateLimits(content.envelopeLimits, content.budgets)

    private fun validateAntSpending(content: PlanWizardUiState.Content): Boolean {
        when (val parsed = MoneyInputParser.parsePen(content.antSpendingLimitText)) {
            is DomainResult.Err -> {
                updateContent { it.copy(errorMessage = "Ingresa un límite válido para gastos hormiga") }
                return false
            }

            is DomainResult.Ok -> {
                if (parsed.value.isZero()) {
                    updateContent { it.copy(errorMessage = "El límite de gastos hormiga debe ser mayor a cero") }
                    return false
                }
            }
        }
        return true
    }

    private fun validateSocialDebt(content: PlanWizardUiState.Content): Boolean {
        val counterparty = content.socialDebtCounterparty.trim()
        if (counterparty.isEmpty()) {
            updateContent { it.copy(errorMessage = "Ingresa a quién le debes") }
            return false
        }
        when (val parsed = MoneyInputParser.parsePen(content.socialDebtAmountText)) {
            is DomainResult.Err -> {
                updateContent { it.copy(errorMessage = "Revisa el monto de tu deuda social") }
                return false
            }

            is DomainResult.Ok -> {
                if (parsed.value.isZero()) {
                    updateContent { it.copy(errorMessage = "Ingresa el monto que debes") }
                    return false
                }
            }
        }
        return true
    }

    private fun validateGoal(content: PlanWizardUiState.Content): Boolean {
        val target = parseGoalAmount(content.goalTargetText)
        if (target == null || target.isZero()) {
            updateContent { it.copy(errorMessage = "Ingresa cuánto necesitas para tu meta") }
            return false
        }
        return true
    }

    private fun validateLimits(
        limits: Map<String, String>,
        budgets: List<EnvelopeBudgetState>,
    ): Boolean {
        for ((envelopeId, limitText) in limits) {
            val name = budgets.find { it.envelopeId == envelopeId }?.name
                ?: PlanEnvelopeTemplates.WIZARD_ENVELOPES.find { it.envelopeId == envelopeId }?.name
                ?: "sobre"
            when (val parsed = MoneyInputParser.parsePen(limitText)) {
                is DomainResult.Err -> {
                    updateContent { it.copy(errorMessage = "Revisa el límite de $name") }
                    return false
                }

                is DomainResult.Ok -> {
                    if (parsed.value.isZero()) {
                        updateContent { it.copy(errorMessage = "El límite de $name debe ser mayor a cero") }
                        return false
                    }
                }
            }
        }
        return true
    }

    private fun parseMonthlyIncome(content: PlanWizardUiState.Content): Money? =
        when (
            val result = estimateMonthlyIncome.estimate(
                profile = content.incomeProfile,
                fixedBaseText = content.fixedBaseText,
                frequency = content.payFrequency,
                secondQuincenaText = content.secondQuincenaText,
                extraIncomeText = content.extraIncomeText,
                additionalIncomeLines = content.additionalIncomeLines,
                lowWeekText = content.lowWeekText,
                normalWeekText = content.normalWeekText,
                goodWeekText = content.goodWeekText,
                approximateText = content.approximateIncomeText,
            )
        ) {
            is DomainResult.Ok -> result.value
            is DomainResult.Err -> null
        }

    private fun parseFixedExpenses(content: PlanWizardUiState.Content): Money? {
        if (content.skipFixedExpenses) return Money.ZERO
        return when (
            val result = FixedExpenseBreakdownCalculator.sumAll(
                presetParts = listOf(
                    content.educationText,
                    content.rentText,
                    content.utilitiesText,
                    content.phoneText,
                    content.debtsText,
                ),
                customLines = content.customExpenseLines,
            )
        ) {
            is DomainResult.Ok -> result.value
            is DomainResult.Err -> null
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
        val income = parseMonthlyIncome(content)
        val fixedExpenses = parseFixedExpenses(content)

        val planBreakdown = if (income != null && fixedExpenses != null) {
            val envelopes = previewBudgets.map { it.toEnvelope() }
            val commitments = buildPreviewCommitments(content)
            val plan = FinancialPlan(
                id = FinancialPlanIds.PRIMARY,
                estimatedMonthlyIncome = income,
                fixedExpenses = fixedExpenses,
                envelopeIds = emptyList(),
            )
            validateFinancialPlan.analyze(plan, envelopes, commitments)
        } else {
            null
        }

        val previewValidation = planBreakdown?.validation ?: validation
        val monthlyEnvelope = planBreakdown?.monthlyEnvelopeReserve
            ?: multiplyWeeklyToMonthly(
                previewBudgets.fold(Money.ZERO) { acc, budget -> acc + budget.weeklyLimit },
            )
        val monthlyExtra = planBreakdown?.monthlySurplus

        val referenceInstant = timeProvider.now()
        val cycleRange = cycleRangeCalculator.currentCycleRange(pe.kipu.core.domain.model.BudgetCycle.WEEKLY, referenceInstant)
        val daily = calculateCycleAvailable(previewBudgets, referenceInstant, cycleRange, pe.kipu.core.domain.model.BudgetCycle.WEEKLY)

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
        val antBudget = content.budgets.find { it.envelopeId == PlanEnvelopeTemplates.ANT_SPENDING_ENVELOPE_ID }
            ?: EnvelopeBudgetState(
                envelopeId = PlanEnvelopeTemplates.ANT_SPENDING_ENVELOPE_ID,
                name = "Gastos hormiga",
                categoryId = "category-other",
                weeklyLimit = Money.ZERO,
                spentAmount = Money.ZERO,
                remainingAmount = Money.ZERO,
                percentUsed = 0,
                status = EnvelopeBudgetStatus.OK,
            )

        val storedBudgets = content.budgets.map { budget ->
            val limitText = when (budget.envelopeId) {
                PlanEnvelopeTemplates.ANT_SPENDING_ENVELOPE_ID -> antLimit
                else -> content.envelopeLimits[budget.envelopeId] ?: return@map budget
            }
            applyLimit(budget, limitText)
        }

        val templateBudgets = PlanEnvelopeTemplates.WIZARD_ENVELOPES
            .filterNot { template -> storedBudgets.any { it.envelopeId == template.envelopeId } }
            .mapNotNull { template ->
                val limitText = content.envelopeLimits[template.envelopeId] ?: return@mapNotNull null
                buildTemplateBudgetState(
                    envelopeId = template.envelopeId,
                    name = template.name,
                    categoryId = categoryIdForWizardEnvelope(template.envelopeId),
                    limitText = limitText,
                )
            }

        val antPreview = applyLimit(antBudget, antLimit)

        val customBudgets = content.customEnvelopeLines.map { line ->
            val limit = when (val parsed = MoneyInputParser.parsePen(line.amountText)) {
                is DomainResult.Ok -> parsed.value
                is DomainResult.Err -> Money.ZERO
            }
            EnvelopeBudgetState(
                envelopeId = line.id,
                name = line.label,
                categoryId = line.categoryId ?: "category-other",
                weeklyLimit = limit,
                spentAmount = Money.ZERO,
                remainingAmount = limit,
                percentUsed = 0,
                status = EnvelopeBudgetStatus.OK,
            )
        }

        val envelopeBudgets = storedBudgets + templateBudgets
        val allPreviews = if (envelopeBudgets.any { it.envelopeId == antPreview.envelopeId }) {
            envelopeBudgets
        } else {
            envelopeBudgets + antPreview
        }

        return allPreviews + customBudgets
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

    private suspend fun saveWizardEnvelopeLimit(envelopeId: String, limit: Money): Boolean {
        val existing = envelopeRepository.getById(envelopeId)
        val envelope = existing?.copy(weeklyLimit = limit)
            ?: Envelope(
                id = envelopeId,
                name = wizardEnvelopeName(envelopeId),
                weeklyLimit = limit,
                categoryId = categoryIdForWizardEnvelope(envelopeId),
            )

        return envelopeRepository.save(envelope).fold(
            onSuccess = { true },
            onFailure = {
                updateContent { content ->
                    content.copy(isSaving = false, errorMessage = "No pudimos guardar tus sobres")
                }
                false
            },
        )
    }

    private fun wizardEnvelopeName(envelopeId: String): String =
        PlanEnvelopeTemplates.WIZARD_ENVELOPES.firstOrNull { it.envelopeId == envelopeId }?.name
            ?: "Gastos hormiga"

    private fun categoryIdForWizardEnvelope(envelopeId: String): String = when (envelopeId) {
        pe.kipu.core.domain.plan.DefaultPlanEnvelopeIds.FOOD -> CategoryIds.FOOD
        pe.kipu.core.domain.plan.DefaultPlanEnvelopeIds.TRANSPORT -> CategoryIds.TRANSPORT
        else -> CategoryIds.OTHER
    }

    private fun buildDefaultEnvelopeLimits(
        budgets: List<EnvelopeBudgetState>,
        preferences: UserPreferences,
    ): Map<String, String> {
        val defaults = mutableMapOf<String, String>()
        PlanEnvelopeTemplates.WIZARD_ENVELOPES.forEach { template ->
            val existing = budgets.find { it.envelopeId == template.envelopeId }
            if (existing != null) {
                defaults[template.envelopeId] = existing.weeklyLimit.amount.stripTrailingZeros().toPlainString()
            }
        }
        val antFromBudget = budgets.find { it.envelopeId == PlanEnvelopeTemplates.ANT_SPENDING_ENVELOPE_ID }
            ?.weeklyLimit?.amount
        val antFromPrefs = preferences.antSpendingWeeklyLimitCents?.let { cents ->
            BigDecimal.valueOf(cents).movePointLeft(2)
        }
        val antAmount = antFromPrefs ?: antFromBudget
        if (antAmount != null) {
            defaults[PlanEnvelopeTemplates.ANT_SPENDING_ENVELOPE_ID] = antAmount.stripTrailingZeros().toPlainString()
        }
        return defaults
    }

    private fun multiplyWeeklyToMonthly(weekly: Money): Money {
        val product = weekly.amount.multiply(BigDecimal.valueOf(4))
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
        val educationText: String,
        val rentText: String,
        val utilitiesText: String,
        val phoneText: String,
        val debtsText: String,
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
        val current = currentContent() ?: return
        _uiState.update { transform(current) }
    }

    private fun newWizardLine(label: String = ""): PlanWizardLineItem =
        PlanWizardLineItem(
            id = "line-${timeProvider.now().toEpochMilli()}",
            label = label,
            amountText = "",
        )

    private suspend fun migrateAntSpendingCategoryIds(stored: Set<String>): Set<String> {
        if (stored.isEmpty()) return emptySet()
        val migrated = mutableSetOf<String>()
        var needsMigration = false
        for (value in stored) {
            if (value.startsWith("category-")) {
                migrated.add(value)
            } else {
                needsMigration = true
                createCategory(value).onSuccess { migrated.add(it.id) }
            }
        }
        if (needsMigration) {
            userPreferencesRepository.updatePreferences { prefs ->
                prefs.copy(antSpendingTrackedCategories = migrated)
            }
        }
        return migrated
    }

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
