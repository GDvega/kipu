package pe.kipu.feature.plan.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.kipu.core.domain.model.Commitment
import pe.kipu.core.domain.model.CommitmentType
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.Envelope
import pe.kipu.core.domain.model.EnvelopeBudgetState
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
import pe.kipu.core.domain.plan.GoalCurrency
import pe.kipu.core.domain.plan.currency
import pe.kipu.core.domain.repository.CommitmentRepository
import pe.kipu.core.domain.repository.FinancialPlanRepository
import pe.kipu.core.domain.repository.UserPreferencesRepository
import pe.kipu.core.domain.time.TimeProvider
import pe.kipu.core.domain.time.WeekRangeCalculator
import pe.kipu.core.domain.usecase.CalculateDailyAvailableUseCase
import pe.kipu.core.domain.usecase.CalculateGoalWeeklyContributionUseCase
import pe.kipu.core.domain.usecase.EstimateMonthlyIncomeUseCase
import pe.kipu.core.domain.usecase.ObserveEnvelopeBudgetsUseCase
import pe.kipu.core.domain.usecase.SaveFinancialPlanUseCase
import pe.kipu.core.domain.usecase.UpdateEnvelopeWeeklyLimitUseCase
import pe.kipu.core.domain.usecase.ValidateFinancialPlanUseCase
import pe.kipu.core.designsystem.component.formatPenAmountForDisplay
import pe.kipu.core.domain.util.MoneyInputParser

@HiltViewModel
class PlanWizardViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val financialPlanRepository: FinancialPlanRepository,
    private val commitmentRepository: CommitmentRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val observeEnvelopeBudgets: ObserveEnvelopeBudgetsUseCase,
    private val saveFinancialPlan: SaveFinancialPlanUseCase,
    private val updateEnvelopeWeeklyLimit: UpdateEnvelopeWeeklyLimitUseCase,
    private val validateFinancialPlan: ValidateFinancialPlanUseCase,
    private val calculateDailyAvailable: CalculateDailyAvailableUseCase,
    private val estimateMonthlyIncome: EstimateMonthlyIncomeUseCase,
    private val calculateGoalWeeklyContribution: CalculateGoalWeeklyContributionUseCase,
    private val weekRangeCalculator: WeekRangeCalculator,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val startStep = planWizardStepFromRoute(savedStateHandle.get<String>("startStep"))

    private val wizardEnvelopeIds = buildSet {
        PlanEnvelopeTemplates.WIZARD_ENVELOPES.forEach { add(it.envelopeId) }
        add(PlanEnvelopeTemplates.ANT_SPENDING_ENVELOPE_ID)
    }

    private val _uiState = MutableStateFlow<PlanWizardUiState>(PlanWizardUiState.Loading)
    val uiState: StateFlow<PlanWizardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val preferences = userPreferencesRepository.observePreferences().first()
            val allBudgets = observeEnvelopeBudgets().first()
            val wizardBudgets = allBudgets.filter { it.envelopeId in wizardEnvelopeIds }

            val defaultLimits = buildDefaultEnvelopeLimits(wizardBudgets, preferences)
            val emergency = commitmentRepository.getById(CommitmentIds.EMERGENCY_FUND)
            val socialDebt = commitmentRepository.observeCommitments().first()
                .firstOrNull { it.type == CommitmentType.SOCIAL_DEBT }
            val antLimitFromPrefs = preferences.antSpendingWeeklyLimitCents?.let { cents ->
                BigDecimal.valueOf(cents).movePointLeft(2).stripTrailingZeros().toPlainString()
            }

            _uiState.value = PlanWizardUiState.Content(
                step = startStep,
                incomeProfile = IncomeProfile.FIXED,
                fixedBaseText = "1500",
                approximateIncomeText = "1500",
                lowWeekText = "250",
                normalWeekText = "400",
                goodWeekText = "650",
                educationText = PeruPlanDefaults.SEED_EDUCATION_MONTHLY.stripTrailingZeros().toPlainString(),
                rentText = PeruPlanDefaults.SEED_RENT_MONTHLY.stripTrailingZeros().toPlainString(),
                utilitiesText = PeruPlanDefaults.SEED_UTILITIES_MONTHLY.stripTrailingZeros().toPlainString(),
                phoneText = PeruPlanDefaults.SEED_PHONE_MONTHLY.stripTrailingZeros().toPlainString(),
                debtsText = PeruPlanDefaults.SEED_DEBTS_MONTHLY.stripTrailingZeros().toPlainString(),
                envelopeLimits = defaultLimits,
                antSpendingLimitText = antLimitFromPrefs ?: defaultLimits[PlanEnvelopeTemplates.ANT_SPENDING_ENVELOPE_ID] ?: "35",
                antSpendingCategories = preferences.antSpendingTrackedCategories,
                antSpendingAlertEnabled = preferences.antSpendingAlertEnabled,
                goalName = emergency?.title ?: GoalType.EMERGENCY.defaultTitle(),
                goalTargetText = emergency?.targetAmount?.amount?.stripTrailingZeros()?.toPlainString() ?: "1000",
                goalCurrentText = emergency?.currentAmount?.amount?.stripTrailingZeros()?.toPlainString() ?: "150",
                hasSocialDebt = socialDebt != null && !socialDebt.isSettled,
                socialDebtCounterparty = socialDebt?.counterpartyName.orEmpty(),
                socialDebtAmountText = socialDebt?.currentAmount?.amount?.stripTrailingZeros()?.toPlainString().orEmpty(),
                budgets = wizardBudgets,
            )
            refreshGoalSuggestion()
            refreshSummaryPreview()
        }
    }

    fun onIncomeProfileSelected(profile: IncomeProfile) {
        updateContent { it.copy(incomeProfile = profile, errorMessage = null) }
    }

    fun onFixedBaseChanged(value: String) = updateContent { it.copy(fixedBaseText = value, errorMessage = null) }
    fun onPayFrequencySelected(frequency: PayFrequency) =
        updateContent { it.copy(payFrequency = frequency, errorMessage = null) }

    fun onExtraIncomeChanged(value: String) = updateContent { it.copy(extraIncomeText = value, errorMessage = null) }
    fun onLowWeekChanged(value: String) = updateContent { it.copy(lowWeekText = value, errorMessage = null) }
    fun onNormalWeekChanged(value: String) = updateContent { it.copy(normalWeekText = value, errorMessage = null) }
    fun onGoodWeekChanged(value: String) = updateContent { it.copy(goodWeekText = value, errorMessage = null) }
    fun onApproximateIncomeChanged(value: String) =
        updateContent { it.copy(approximateIncomeText = value, errorMessage = null) }

    fun onEducationChanged(value: String) = updateContent { it.copy(educationText = value, errorMessage = null) }
    fun onRentChanged(value: String) = updateContent { it.copy(rentText = value, errorMessage = null) }
    fun onUtilitiesChanged(value: String) = updateContent { it.copy(utilitiesText = value, errorMessage = null) }
    fun onPhoneChanged(value: String) = updateContent { it.copy(phoneText = value, errorMessage = null) }
    fun onDebtsChanged(value: String) = updateContent { it.copy(debtsText = value, errorMessage = null) }

    fun onSkipFixedExpenses() {
        updateContent {
            it.copy(
                skipFixedExpenses = true,
                educationText = "",
                rentText = "",
                utilitiesText = "",
                phoneText = "",
                debtsText = "",
                errorMessage = null,
            )
        }
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
    }

    fun onEnvelopeLimitChanged(envelopeId: String, value: String) {
        updateContent {
            it.copy(
                envelopeLimits = it.envelopeLimits + (envelopeId to value),
                errorMessage = null,
            )
        }
    }

    fun onCustomizeEnvelope(envelopeId: String?) {
        updateContent { it.copy(customizingEnvelopeId = envelopeId) }
    }

    fun onAntSpendingLimitChanged(value: String) {
        updateContent { it.copy(antSpendingLimitText = value, errorMessage = null) }
    }

    fun onAntSpendingPresetSelected(amount: BigDecimal) {
        updateContent {
            it.copy(antSpendingLimitText = amount.stripTrailingZeros().toPlainString(), errorMessage = null)
        }
    }

    fun onAntCategoryToggled(category: String) {
        updateContent { content ->
            val updated = if (category in content.antSpendingCategories) {
                content.antSpendingCategories - category
            } else {
                content.antSpendingCategories + category
            }
            content.copy(antSpendingCategories = updated)
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
    }

    fun onGoalCurrentChanged(value: String) {
        updateContent { it.copy(goalCurrentText = value, errorMessage = null) }
        refreshGoalSuggestion()
    }

    fun onGoalMonthsSelected(months: Int) {
        updateContent { it.copy(goalMonths = months, errorMessage = null) }
        refreshGoalSuggestion()
    }

    fun onSkipGoal() {
        updateContent { it.copy(goalSkipped = true, errorMessage = null) }
        viewModelScope.launch {
            refreshSummaryPreview()
            updateContent { it.copy(step = PlanWizardStep.Summary) }
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
            val text = it.approximateIncomeText.ifBlank { "1500" }
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
                updateContent { it.copy(step = PlanWizardStep.Envelopes, errorMessage = null) }
            }

            PlanWizardStep.Envelopes -> {
                if (!validateEnvelopeLimits(content)) return
                updateContent { it.copy(step = PlanWizardStep.AntSpending, errorMessage = null) }
            }

            PlanWizardStep.AntSpending -> {
                if (!validateAntSpending(content)) return
                updateContent { it.copy(step = PlanWizardStep.Goal, errorMessage = null) }
                refreshGoalSuggestion()
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
            val deficit = (refreshed.validation as FinancialPlanValidationResult.Invalid).deficit
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

        updateContent { it.copy(isSaving = true, errorMessage = null) }

        val allLimits = refreshed.envelopeLimits + mapOf(
            PlanEnvelopeTemplates.ANT_SPENDING_ENVELOPE_ID to refreshed.antSpendingLimitText,
        )

        for ((envelopeId, limitText) in allLimits) {
            when (val limit = MoneyInputParser.parsePen(limitText)) {
                is DomainResult.Err -> {
                    updateContent { it.copy(isSaving = false, errorMessage = "Revisa los límites de sobres") }
                    return false
                }

                is DomainResult.Ok -> {
                    updateEnvelopeWeeklyLimit(envelopeId, limit.value)
                        .onFailure {
                            updateContent { it.copy(isSaving = false, errorMessage = "No pudimos guardar tus sobres") }
                            return false
                        }
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
        val currency = content.goalType.currency()
        val target = parseGoalAmount(content.goalTargetText, currency)
        if (target == null || target.isZero()) {
            updateContent { it.copy(isSaving = false, errorMessage = "Revisa el monto de tu meta") }
            return false
        }

        val current = parseGoalAmount(content.goalCurrentText, currency) ?: Money.ZERO

        val existing = commitmentRepository.getById(CommitmentIds.EMERGENCY_FUND)
        val commitment = Commitment(
            id = CommitmentIds.EMERGENCY_FUND,
            type = CommitmentType.SAVINGS_GOAL,
            title = content.goalName.ifBlank { content.goalType.defaultTitle() },
            targetAmount = target,
            currentAmount = current,
            dueDate = null,
            counterpartyName = null,
            isSettled = false,
            currencyCode = currency.code,
        ).let { candidate ->
            existing?.copy(
                title = candidate.title,
                targetAmount = candidate.targetAmount,
                currentAmount = candidate.currentAmount,
                currencyCode = candidate.currencyCode,
            ) ?: candidate
        }

        return commitmentRepository.save(commitment).fold(
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
                ?: commitmentRepository.observeCommitments().first()
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
            ?: commitmentRepository.observeCommitments().first()
                .firstOrNull { it.type == CommitmentType.SOCIAL_DEBT }

        val commitment = Commitment(
            id = existing?.id ?: CommitmentIds.PRIMARY_SOCIAL_DEBT,
            type = CommitmentType.SOCIAL_DEBT,
            title = "Deuda con $counterparty",
            targetAmount = null,
            currentAmount = amount,
            dueDate = null,
            counterpartyName = counterparty,
            isSettled = false,
        )

        return commitmentRepository.save(commitment).fold(
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

    private fun parseGoalAmount(text: String, @Suppress("UNUSED_PARAMETER") currency: GoalCurrency): Money? {
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
        val currency = content.goalType.currency()
        val target = parseGoalAmount(content.goalTargetText, currency)
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
                extraIncomeText = content.extraIncomeText,
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
            val result = FixedExpenseBreakdownCalculator.sumParts(
                content.educationText,
                content.rentText,
                content.utilitiesText,
                content.phoneText,
                content.debtsText,
            )
        ) {
            is DomainResult.Ok -> result.value
            is DomainResult.Err -> null
        }
    }

    private fun refreshGoalSuggestion() {
        val content = currentContent() ?: return
        val weekly = when (
            val result = calculateGoalWeeklyContribution.invoke(
                content.goalTargetText,
                content.goalCurrentText,
                content.goalMonths,
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

        val previewValidation = if (income != null && fixedExpenses != null) {
            val envelopes = previewBudgets.map { it.toEnvelope() }
            val commitments = buildPreviewCommitments(content)
            val plan = FinancialPlan(
                id = FinancialPlanIds.PRIMARY,
                estimatedMonthlyIncome = income,
                fixedExpenses = fixedExpenses,
                envelopeIds = emptyList(),
            )
            validateFinancialPlan(plan, envelopes, commitments)
        } else {
            validation
        }

        val weeklyTotal = previewBudgets.fold(Money.ZERO) { acc, budget -> acc + budget.weeklyLimit }
        val monthlyEnvelope = multiplyWeeklyToMonthly(weeklyTotal)
        val monthlyExtra = if (income != null && fixedExpenses != null) {
            income.minus(fixedExpenses).let { diff ->
                when (diff) {
                    is DomainResult.Ok -> diff.value.minus(monthlyEnvelope).let { extra ->
                        when (extra) {
                            is DomainResult.Ok -> extra.value
                            is DomainResult.Err -> Money.ZERO
                        }
                    }

                    is DomainResult.Err -> Money.ZERO
                }
            }
        } else {
            null
        }

        val referenceInstant = timeProvider.now()
        val weekRange = weekRangeCalculator.currentWeekRange(referenceInstant)
        val daily = calculateDailyAvailable(previewBudgets, referenceInstant, weekRange)

        updateContent {
            it.copy(
                previewBudgets = previewBudgets,
                dailyAvailable = daily,
                validation = previewValidation ?: it.validation,
                monthlyEnvelopeTotal = monthlyEnvelope,
                monthlyExtraAvailable = monthlyExtra,
            )
        }
    }

    private suspend fun buildPreviewCommitments(content: PlanWizardUiState.Content): List<Commitment> {
        val stored = commitmentRepository.observeCommitments().first()
            .filterNot { it.type == CommitmentType.SOCIAL_DEBT || it.id == CommitmentIds.EMERGENCY_FUND }

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
                status = pe.kipu.core.domain.model.EnvelopeBudgetStatus.OK,
            )

        val envelopeBudgets = content.budgets.map { budget ->
            val limitText = content.envelopeLimits[budget.envelopeId] ?: return@map budget
            applyLimit(budget, limitText)
        }

        val antPreview = applyLimit(antBudget, antLimit)
        return if (envelopeBudgets.any { it.envelopeId == antPreview.envelopeId }) {
            envelopeBudgets
        } else {
            envelopeBudgets + antPreview
        }
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

    private fun buildDefaultEnvelopeLimits(
        budgets: List<EnvelopeBudgetState>,
        preferences: pe.kipu.core.domain.model.UserPreferences,
    ): Map<String, String> {
        val defaults = mutableMapOf<String, String>()
        PlanEnvelopeTemplates.WIZARD_ENVELOPES.forEach { template ->
            val existing = budgets.find { it.envelopeId == template.envelopeId }
            val amount = existing?.weeklyLimit?.amount
                ?: PlanEnvelopeTemplates.defaultWeeklyLimit(template)
            defaults[template.envelopeId] = amount.stripTrailingZeros().toPlainString()
        }
        val antFromBudget = budgets.find { it.envelopeId == PlanEnvelopeTemplates.ANT_SPENDING_ENVELOPE_ID }
            ?.weeklyLimit?.amount
        val antFromPrefs = preferences.antSpendingWeeklyLimitCents?.let { cents ->
            BigDecimal.valueOf(cents).movePointLeft(2)
        }
        val antAmount = antFromPrefs ?: antFromBudget ?: PlanEnvelopeTemplates.ANT_SPENDING_PRESETS[1]
        defaults[PlanEnvelopeTemplates.ANT_SPENDING_ENVELOPE_ID] = antAmount.stripTrailingZeros().toPlainString()
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

    private fun currentContent(): PlanWizardUiState.Content? = _uiState.value as? PlanWizardUiState.Content

    private fun updateContent(transform: (PlanWizardUiState.Content) -> PlanWizardUiState.Content) {
        val current = currentContent() ?: return
        _uiState.update { transform(current) }
    }
}
