package pe.kipu.feature.plan.presentation

import javax.inject.Inject
import java.util.concurrent.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.flow.FlowFirstDefaults
import pe.kipu.core.domain.model.Category
import pe.kipu.core.domain.model.Commitment
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.Envelope
import pe.kipu.core.domain.model.EnvelopeBudgetState
import pe.kipu.core.domain.model.FinancialPlanValidationResult
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.plan.FixedExpenseBreakdownCalculator
import pe.kipu.core.domain.plan.CustomFixedExpenseSerializer
import pe.kipu.core.domain.plan.DefaultPlanEnvelopeIds
import pe.kipu.core.domain.plan.PlanSetupPreparationError
import pe.kipu.core.domain.plan.PlanSetupPreparationInput
import pe.kipu.core.domain.plan.PlanSetupPreparationResult
import pe.kipu.core.domain.plan.PlanEnvelopeTemplates
import pe.kipu.core.domain.plan.currency
import pe.kipu.core.domain.plan.defaultTitle
import pe.kipu.core.domain.plan.PreparePlanSetupUseCase
import pe.kipu.core.domain.repository.CategoryRepository
import pe.kipu.core.domain.repository.CommitmentRepository
import pe.kipu.core.domain.repository.EnvelopeRepository
import pe.kipu.core.domain.plan.PlanSetupRepository
import pe.kipu.core.domain.usecase.EstimateMonthlyIncomeUseCase
import pe.kipu.core.domain.util.MoneyInputParser

internal fun EstimateMonthlyIncomeUseCase.estimateMonthlyIncome(
    content: PlanWizardUiState.Content,
): DomainResult<Money> = estimate(
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

class PlanWizardSaver @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val commitmentRepository: CommitmentRepository,
    private val envelopeRepository: EnvelopeRepository,
    private val planSetupRepository: PlanSetupRepository,
    private val estimateMonthlyIncome: EstimateMonthlyIncomeUseCase,
    private val preparePlanSetup: PreparePlanSetupUseCase,
) {
    sealed interface Result {
        data class Success(val validation: FinancialPlanValidationResult) : Result
        data class Error(val message: String) : Result
    }

    suspend fun save(content: PlanWizardUiState.Content): Result {
        val income = parseMonthlyIncome(content)
        if (income == null || income.isZero()) {
            return Result.Error("Ingresa un monto de ingreso válido")
        }

        val fixedExpenses = parseFixedExpenses(content)
        if (!content.skipFixedExpenses && fixedExpenses == null) {
            return Result.Error("Revisa el desglose de gastos fijos")
        }

        val reserveMonthlyContribution = parseReserveMonthlyContribution(content)
            ?: return Result.Error("Revisa el aporte mensual para imprevistos")

        val limitsValidation = validateLimits(content.envelopeLimits, content.budgets)
        if (limitsValidation != null) {
            return Result.Error(limitsValidation)
        }

        if (!validateAntSpending(content)) {
            return Result.Error("Revisa el límite de gastos hormiga")
        }

        if (!content.goalSkipped && content.goalTargetText.isNotBlank()) {
            val target = parseGoalAmount(content.goalTargetText)
            if (target == null || target.isZero()) return Result.Error("Revisa el monto de tu meta")
        }

        if (content.hasSocialDebt) {
            validateSocialDebt(content)?.let { return Result.Error(it) }
        }

        val initialBalance = when (val parsed = MoneyInputParser.parsePen(content.initialBalanceText)) {
            is DomainResult.Ok -> parsed.value
            is DomainResult.Err -> Money.ZERO
        }

        val existingData = try {
            loadExistingData()
        } catch (_: TimeoutCancellationException) {
            return Result.Error("No pudimos leer tus datos actuales. Intenta nuevamente.")
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            return Result.Error("No pudimos leer tus datos actuales. Intenta nuevamente.")
        }

        val preparation = preparePlanSetup(
            PlanSetupPreparationInput(
                estimatedMonthlyIncome = income,
                fixedExpenses = fixedExpenses ?: Money.ZERO,
                initialBalance = initialBalance,
                reserveMonthlyContribution = reserveMonthlyContribution,
                incomeProfile = content.incomeProfile,
                payFrequency = content.payFrequency,
                budgetCycle = content.budgetCycle,
                envelopeLimits = content.envelopeLimits,
                antSpendingLimitText = content.antSpendingLimitText,
                antSpendingAlertEnabled = content.antSpendingAlertEnabled,
                antSpendingAlertPercent = 80,
                antSpendingTrackedCategoryIds = content.antSpendingCategories,
                customEnvelopeLines = content.customEnvelopeLines,
                goalSkipped = content.goalSkipped,
                goalTitle = content.goalName.ifBlank { content.goalType.defaultTitle() },
                goalTargetText = content.goalTargetText,
                goalCurrentText = content.goalCurrentText,
                goalMonthsText = content.goalMonthsText,
                goalCurrencyCode = content.goalType.currency().code,
                hasSocialDebt = content.hasSocialDebt,
                socialDebtCounterparty = content.socialDebtCounterparty,
                socialDebtAmountText = content.socialDebtAmountText,
                existingCategories = existingData.categories,
                existingEnvelopes = existingData.envelopes,
                existingCommitments = existingData.commitments,
                electricityExpenses = parseOptionalBreakdownMoney(content.electricityText),
                waterExpenses = parseOptionalBreakdownMoney(content.waterText),
                internetExpenses = parseOptionalBreakdownMoney(content.internetText),
                rentExpenses = parseOptionalBreakdownMoney(content.rentText),
                phoneExpenses = parseOptionalBreakdownMoney(content.phoneText),
                debtsExpenses = parseOptionalBreakdownMoney(content.debtsText),
                educationExpenses = parseOptionalBreakdownMoney(content.educationText),
                customFixedExpensesJson = CustomFixedExpenseSerializer.serialize(content.customExpenseLines),
            ),
        )
        if (preparation is PlanSetupPreparationResult.Error) {
            return Result.Error(preparationErrorMessage(preparation.reason))
        }
        preparation as PlanSetupPreparationResult.Success

        val saveResult = planSetupRepository.save(preparation.setup)
        return if (saveResult.isSuccess) {
            Result.Success(preparation.validation)
        } else {
            Result.Error("No pudimos guardar tu plan. No se aplicó ningún cambio parcial.")
        }
    }

    private suspend fun loadExistingData(): ExistingPlanData =
        withTimeout(FlowFirstDefaults.TIMEOUT_MS) {
            coroutineScope {
                val categories = async { categoryRepository.observeCategories().first() }
                val envelopes = async { envelopeRepository.observeEnvelopes().first() }
                val commitments = async { commitmentRepository.observeCommitments().first() }
                ExistingPlanData(
                    categories = categories.await(),
                    envelopes = envelopes.await(),
                    commitments = commitments.await(),
                )
            }
        }

    private data class ExistingPlanData(
        val categories: List<Category>,
        val envelopes: List<Envelope>,
        val commitments: List<Commitment>,
    )

    private fun preparationErrorMessage(reason: PlanSetupPreparationError): String = when (reason) {
        is PlanSetupPreparationError.InvalidEnvelopeAmount,
        is PlanSetupPreparationError.InvalidCustomEnvelopeAmount,
        -> "Revisa los límites de sobres"

        is PlanSetupPreparationError.BlankCustomEnvelopeName ->
            "Ponle un nombre a cada sobre personalizado"

        PlanSetupPreparationError.DuplicateCustomEnvelopeName ->
            "No repitas el nombre de un sobre personalizado"

        PlanSetupPreparationError.DuplicateCustomEnvelopeIdentity ->
            "No pudimos identificar tus sobres personalizados"

        PlanSetupPreparationError.InvalidGoal -> "Revisa los datos de tu meta"
        PlanSetupPreparationError.InvalidSocialDebt -> "Revisa los datos de tu deuda social"
        PlanSetupPreparationError.InvalidPreparedModel -> "No pudimos preparar un plan válido"
        is PlanSetupPreparationError.InvalidFinancialPlan ->
            "Tu plan no cuadra. Ajusta montos antes de guardar. " +
                "Faltan ${reason.validation.deficit.amount} para el mes."
    }

    fun categoryIdForWizardEnvelope(envelopeId: String): String = when (envelopeId) {
        DefaultPlanEnvelopeIds.FOOD -> CategoryIds.FOOD
        DefaultPlanEnvelopeIds.TRANSPORT -> CategoryIds.TRANSPORT
        else -> CategoryIds.OTHER
    }

    fun parseGoalAmount(text: String): Money? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return Money.ZERO
        return when (val result = MoneyInputParser.parsePen(trimmed)) {
            is DomainResult.Ok -> result.value
            is DomainResult.Err -> null
        }
    }

    fun validateLimits(
        limits: Map<String, String>,
        budgets: List<EnvelopeBudgetState>,
    ): String? {
        for ((envelopeId, limitText) in limits) {
            if (limitText.isBlank()) continue
            val name = budgets.find { it.envelopeId == envelopeId }?.name
                ?: PlanEnvelopeTemplates.WIZARD_ENVELOPES.find { it.envelopeId == envelopeId }?.name
                ?: "sobre"
            when (val parsed = MoneyInputParser.parsePen(limitText)) {
                is DomainResult.Err -> return "Revisa el límite de $name"
                is DomainResult.Ok -> {
                    if (parsed.value.isZero()) return "El límite de $name debe ser mayor a cero"
                }
            }
        }
        return null
    }

    fun validateAntSpending(content: PlanWizardUiState.Content): Boolean {
        when (val parsed = MoneyInputParser.parsePen(content.antSpendingLimitText)) {
            is DomainResult.Err -> return false
            is DomainResult.Ok -> {
                if (parsed.value.isZero()) return false
            }
        }
        return true
    }

    fun validateSocialDebt(content: PlanWizardUiState.Content): String? {
        val counterparty = content.socialDebtCounterparty.trim()
        if (counterparty.isEmpty()) return "Ingresa a quién le debes"

        val amount = when (val parsed = MoneyInputParser.parsePen(content.socialDebtAmountText)) {
            is DomainResult.Ok -> parsed.value
            is DomainResult.Err -> return "Revisa el monto de tu deuda social"
        }
        if (amount.isZero()) return "Ingresa el monto que debes"
        return null
    }

    fun parseMonthlyIncome(content: PlanWizardUiState.Content): Money? =
        when (val result = estimateMonthlyIncome.estimateMonthlyIncome(content)) {
            is DomainResult.Ok -> result.value
            is DomainResult.Err -> null
        }

    fun parseFixedExpenses(content: PlanWizardUiState.Content): Money? {
        if (content.skipFixedExpenses) return Money.ZERO
        return when (
            val result = FixedExpenseBreakdownCalculator.sumPresetParts(
                electricityText = content.electricityText,
                waterText = content.waterText,
                internetText = content.internetText,
                rentText = content.rentText,
                phoneText = content.phoneText,
                debtsText = content.debtsText,
                educationText = content.educationText,
                customLines = content.customExpenseLines,
            )
        ) {
            is DomainResult.Ok -> result.value
            is DomainResult.Err -> null
        }
    }

    fun parseReserveMonthlyContribution(content: PlanWizardUiState.Content): Money? {
        val text = content.reserveMonthlyContributionText.trim()
        if (text.isEmpty()) return Money.ZERO
        return when (val parsed = MoneyInputParser.parsePen(text)) {
            is DomainResult.Ok -> parsed.value
            is DomainResult.Err -> null
        }
    }

    private fun parseOptionalBreakdownMoney(text: String): Money? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        return when (val parsed = MoneyInputParser.parsePen(trimmed)) {
            is DomainResult.Ok -> parsed.value.takeUnless { it.isZero() }
            is DomainResult.Err -> null
        }
    }
}
