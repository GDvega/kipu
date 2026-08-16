package pe.kipu.core.domain.plan

import java.util.Locale
import javax.inject.Inject
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.BudgetCycle
import pe.kipu.core.domain.model.Category
import pe.kipu.core.domain.model.Commitment
import pe.kipu.core.domain.model.CommitmentType
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.Envelope
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.FinancialPlan
import pe.kipu.core.domain.model.FinancialPlanValidationResult
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.usecase.ValidateFinancialPlanUseCase
import pe.kipu.core.domain.util.MoneyInputParser

data class PlanSetupPreparationInput(
    val estimatedMonthlyIncome: Money,
    val fixedExpenses: Money,
    val initialBalance: Money = Money.ZERO,
    val incomeProfile: IncomeProfile = IncomeProfile.FIXED,
    val payFrequency: PayFrequency = PayFrequency.MONTHLY,
    val budgetCycle: BudgetCycle = BudgetCycle.WEEKLY,
    val envelopeLimits: Map<String, String>,
    val antSpendingLimitText: String,
    val antSpendingAlertEnabled: Boolean = true,
    val antSpendingAlertPercent: Int = 80,
    val antSpendingTrackedCategoryIds: Set<EntityId> = emptySet(),
    val customEnvelopeLines: List<PlanWizardLineItem> = emptyList(),
    val goalSkipped: Boolean,
    val goalTitle: String,
    val goalTargetText: String,
    val goalCurrentText: String,
    val goalMonthsText: String,
    val goalCurrencyCode: String = GoalCurrency.PEN.code,
    val hasSocialDebt: Boolean = false,
    val socialDebtCounterparty: String = "",
    val socialDebtAmountText: String = "",
    val existingCategories: List<Category> = emptyList(),
    val existingEnvelopes: List<Envelope> = emptyList(),
    val existingCommitments: List<Commitment> = emptyList(),
)

sealed interface PlanSetupPreparationError {
    data class InvalidEnvelopeAmount(val envelopeId: String) : PlanSetupPreparationError
    data class BlankCustomEnvelopeName(val lineId: String) : PlanSetupPreparationError
    data class InvalidCustomEnvelopeAmount(val lineId: String) : PlanSetupPreparationError
    data object DuplicateCustomEnvelopeName : PlanSetupPreparationError
    data object DuplicateCustomEnvelopeIdentity : PlanSetupPreparationError
    data object InvalidGoal : PlanSetupPreparationError
    data object InvalidSocialDebt : PlanSetupPreparationError
    data object InvalidPreparedModel : PlanSetupPreparationError
    data class InvalidFinancialPlan(
        val validation: FinancialPlanValidationResult.Invalid,
    ) : PlanSetupPreparationError
}

sealed interface PlanSetupPreparationResult {
    data class Success(
        val setup: PlanSetup,
        val validation: FinancialPlanValidationResult,
    ) : PlanSetupPreparationResult

    data class Error(val reason: PlanSetupPreparationError) : PlanSetupPreparationResult
}

class PreparePlanSetupUseCase @Inject constructor(
    private val validateFinancialPlan: ValidateFinancialPlanUseCase,
) {
    operator fun invoke(input: PlanSetupPreparationInput): PlanSetupPreparationResult {
        val duplicateNames = input.customEnvelopeLines
            .map { normalizeName(it.label) }
            .filter { it.isNotEmpty() }
            .groupingBy { it }
            .eachCount()
            .any { (_, count) -> count > 1 }
        if (duplicateNames) {
            return PlanSetupPreparationResult.Error(PlanSetupPreparationError.DuplicateCustomEnvelopeName)
        }
        if (input.customEnvelopeLines.map { it.id }.distinct().size != input.customEnvelopeLines.size) {
            return PlanSetupPreparationResult.Error(PlanSetupPreparationError.DuplicateCustomEnvelopeIdentity)
        }

        val envelopesById = input.existingEnvelopes.associateBy { it.id }.toMutableMap()
        val submittedCustomEnvelopeIds = input.customEnvelopeLines
            .mapTo(mutableSetOf()) { line -> customEnvelopeId(line.id) }
        val envelopeIdsToDelete = input.existingEnvelopes
            .asSequence()
            .map { it.id }
            .filter(PlanEnvelopeTemplates::isWizardManagedCustomEnvelope)
            .filterNot(submittedCustomEnvelopeIds::contains)
            .toSet()
        envelopeIdsToDelete.forEach(envelopesById::remove)
        val wizardLimits = input.envelopeLimits.filterValues { it.isNotBlank() } +
            (PlanEnvelopeTemplates.ANT_SPENDING_ENVELOPE_ID to input.antSpendingLimitText)
        for ((envelopeId, amountText) in wizardLimits) {
            if (amountText.isBlank()) continue
            val limit = parsePositiveMoney(amountText)
                ?: return PlanSetupPreparationResult.Error(
                    PlanSetupPreparationError.InvalidEnvelopeAmount(envelopeId),
                )
            val existing = envelopesById[envelopeId]
            envelopesById[envelopeId] = existing?.copy(weeklyLimit = limit)
                ?: Envelope(
                    id = envelopeId,
                    name = wizardEnvelopeName(envelopeId),
                    weeklyLimit = limit,
                    categoryId = categoryIdForWizardEnvelope(envelopeId),
                )
        }


        val categoriesToSave = mutableListOf<Category>()
        val existingCategoriesByName = input.existingCategories.associateBy { normalizeName(it.name) }
        for (line in input.customEnvelopeLines) {
            val name = line.label.trim()
            if (name.isEmpty()) {
                return PlanSetupPreparationResult.Error(
                    PlanSetupPreparationError.BlankCustomEnvelopeName(line.id),
                )
            }
            val limit = parsePositiveMoney(line.amountText)
                ?: return PlanSetupPreparationResult.Error(
                    PlanSetupPreparationError.InvalidCustomEnvelopeAmount(line.id),
                )
            val category = line.categoryId?.let { categoryId ->
                input.existingCategories.firstOrNull { it.id == categoryId }
                    ?: Category(id = categoryId, name = name).also(categoriesToSave::add)
            } ?: existingCategoriesByName[normalizeName(name)]
                ?: Category(id = customCategoryId(line.id), name = name).also(categoriesToSave::add)
            val envelopeId = customEnvelopeId(line.id)
            val existingEnvelope = envelopesById[envelopeId]
                ?: input.existingEnvelopes.firstOrNull { it.id == line.id }
            envelopesById.remove(line.id)
            envelopesById[envelopeId] = existingEnvelope?.copy(
                id = envelopeId,
                name = name,
                weeklyLimit = limit,
                categoryId = category.id,
            ) ?: Envelope(
                id = envelopeId,
                name = name,
                weeklyLimit = limit,
                categoryId = category.id,
            )
        }

        val commitmentsToSave = input.existingCommitments.filterNot(::isWizardManagedCommitment).toMutableList()
        val commitmentIdsToSettle = mutableSetOf<String>()
        prepareGoal(input)?.let { goalResult ->
            when (goalResult) {
                is PreparedCommitment.Invalid -> {
                    return PlanSetupPreparationResult.Error(PlanSetupPreparationError.InvalidGoal)
                }
                is PreparedCommitment.Save -> commitmentsToSave += goalResult.commitment
                is PreparedCommitment.Settle -> commitmentIdsToSettle += goalResult.id
                PreparedCommitment.None -> Unit
            }
        }
        when (val debtResult = prepareSocialDebt(input)) {
            is PreparedCommitment.Invalid -> {
                return PlanSetupPreparationResult.Error(PlanSetupPreparationError.InvalidSocialDebt)
            }
            is PreparedCommitment.Save -> commitmentsToSave += debtResult.commitment
            is PreparedCommitment.Settle -> commitmentIdsToSettle += debtResult.id
            PreparedCommitment.None -> Unit
        }

        val envelopes = envelopesById.values.sortedBy { it.id }
        val plan = FinancialPlan(
            id = FinancialPlanIds.PRIMARY,
            estimatedMonthlyIncome = input.estimatedMonthlyIncome,
            fixedExpenses = input.fixedExpenses,
            initialBalance = input.initialBalance,
            envelopeIds = envelopes.map { it.id },
            incomeProfile = input.incomeProfile,
            payFrequency = input.payFrequency,
            budgetCycle = input.budgetCycle,
            antSpendingLimit = envelopesById[PlanEnvelopeTemplates.ANT_SPENDING_ENVELOPE_ID]
                ?.weeklyLimit,
            antSpendingAlertEnabled = input.antSpendingAlertEnabled,
            antSpendingAlertPercent = input.antSpendingAlertPercent,
            antSpendingTrackedCategoryIds = input.antSpendingTrackedCategoryIds,
        )
        val setup = PlanSetup(
            plan = plan,
            categories = categoriesToSave.toList(),
            envelopes = envelopes,
            envelopeIdsToDelete = envelopeIdsToDelete,
            commitmentsToSave = commitmentsToSave.toList(),
            commitmentIdsToSettle = commitmentIdsToSettle.toSet(),
        )
        if (!setup.isStructurallyValid()) {
            return PlanSetupPreparationResult.Error(PlanSetupPreparationError.InvalidPreparedModel)
        }
        val validation = validateFinancialPlan(plan, envelopes, commitmentsToSave)
        if (validation is FinancialPlanValidationResult.Invalid) {
            return PlanSetupPreparationResult.Error(
                PlanSetupPreparationError.InvalidFinancialPlan(validation),
            )
        }
        return PlanSetupPreparationResult.Success(setup = setup, validation = validation)
    }

    private fun prepareGoal(input: PlanSetupPreparationInput): PreparedCommitment? {
        val existing = input.existingCommitments.firstOrNull {
            it.id == CommitmentIds.EMERGENCY_FUND && it.type == CommitmentType.SAVINGS_GOAL
        }
        if (input.goalSkipped) {
            return if (existing != null && !existing.isSettled) {
                PreparedCommitment.Settle(existing.id)
            } else {
                PreparedCommitment.None
            }
        }
        val target = parsePositiveMoney(input.goalTargetText) ?: return PreparedCommitment.Invalid
        val current = parseOptionalMoney(input.goalCurrentText) ?: return PreparedCommitment.Invalid
        val months = input.goalMonthsText.toIntOrNull()?.takeIf { it > 0 }
            ?: return PreparedCommitment.Invalid
        return PreparedCommitment.Save(
            Commitment(
                id = existing?.id ?: CommitmentIds.EMERGENCY_FUND,
                type = CommitmentType.SAVINGS_GOAL,
                title = input.goalTitle.trim().ifEmpty { GoalType.EMERGENCY.defaultTitle() },
                targetAmount = target,
                currentAmount = current,
                isSettled = false,
                currencyCode = input.goalCurrencyCode,
                savingsHorizonMonths = months,
            ),
        )
    }

    private fun prepareSocialDebt(input: PlanSetupPreparationInput): PreparedCommitment {
        val existing = input.existingCommitments.firstOrNull {
            it.type == CommitmentType.SOCIAL_DEBT && it.id != CommitmentIds.DEMO_SOCIAL_DEBT
        }
        if (!input.hasSocialDebt) {
            return if (existing != null && !existing.isSettled) {
                PreparedCommitment.Settle(existing.id)
            } else {
                PreparedCommitment.None
            }
        }
        val counterparty = input.socialDebtCounterparty.trim()
        val amount = parsePositiveMoney(input.socialDebtAmountText)
        if (counterparty.isEmpty() || amount == null) return PreparedCommitment.Invalid
        return PreparedCommitment.Save(
            Commitment(
                id = existing?.id ?: CommitmentIds.PRIMARY_SOCIAL_DEBT,
                type = CommitmentType.SOCIAL_DEBT,
                title = "Deuda con $counterparty",
                currentAmount = amount,
                counterpartyName = counterparty,
                isSettled = false,
            ),
        )
    }

    private fun PlanSetup.isStructurallyValid(): Boolean =
        plan.validate() is DomainResult.Ok &&
            categories.all { it.validate() is DomainResult.Ok } &&
            envelopes.all { it.validate() is DomainResult.Ok } &&
            commitmentsToSave.all { it.validate() is DomainResult.Ok }

    private fun parsePositiveMoney(text: String): Money? = when (val parsed = MoneyInputParser.parsePen(text)) {
        is DomainResult.Ok -> parsed.value.takeUnless { it.isZero() }
        is DomainResult.Err -> null
    }

    private fun parseOptionalMoney(text: String): Money? {
        if (text.isBlank()) return Money.ZERO
        return when (val parsed = MoneyInputParser.parsePen(text)) {
            is DomainResult.Ok -> parsed.value
            is DomainResult.Err -> null
        }
    }

    private fun wizardEnvelopeName(envelopeId: String): String =
        PlanEnvelopeTemplates.WIZARD_ENVELOPES.firstOrNull { it.envelopeId == envelopeId }?.name
            ?: "Gastos hormiga"

    private fun categoryIdForWizardEnvelope(envelopeId: String): String = when (envelopeId) {
        DefaultPlanEnvelopeIds.FOOD -> CategoryIds.FOOD
        DefaultPlanEnvelopeIds.TRANSPORT -> CategoryIds.TRANSPORT
        else -> CategoryIds.OTHER
    }

    private fun customCategoryId(lineId: String): String = "category-plan-${stableLineId(lineId)}"

    private fun customEnvelopeId(lineId: String): String =
        if (lineId.startsWith("envelope-")) lineId
        else PlanEnvelopeTemplates.CUSTOM_ENVELOPE_PREFIX + stableLineId(lineId)

    private fun stableLineId(lineId: String): String = lineId.trim().ifEmpty { "invalid" }

    private fun normalizeName(value: String): String = value.trim().lowercase(Locale.ROOT)

    private fun isWizardManagedCommitment(commitment: Commitment): Boolean =
        commitment.id == CommitmentIds.EMERGENCY_FUND ||
            commitment.id == CommitmentIds.PRIMARY_SOCIAL_DEBT ||
            commitment.type == CommitmentType.SOCIAL_DEBT

    private sealed interface PreparedCommitment {
        data class Save(val commitment: Commitment) : PreparedCommitment
        data class Settle(val id: String) : PreparedCommitment
        data object None : PreparedCommitment
        data object Invalid : PreparedCommitment
    }
}
