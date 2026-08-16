package pe.kipu.core.domain.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.first
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.FinancialPlan
import pe.kipu.core.domain.model.FinancialPlanValidationResult
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.plan.IncomeProfile
import pe.kipu.core.domain.plan.PayFrequency
import pe.kipu.core.domain.repository.CommitmentRepository
import pe.kipu.core.domain.repository.EnvelopeRepository
import pe.kipu.core.domain.repository.FinancialPlanRepository

data class SaveFinancialPlanResult(
    val validation: FinancialPlanValidationResult,
)

class SaveFinancialPlanUseCase @Inject constructor(
    private val financialPlanRepository: FinancialPlanRepository,
    private val envelopeRepository: EnvelopeRepository,
    private val commitmentRepository: CommitmentRepository,
    private val validateFinancialPlan: ValidateFinancialPlanUseCase,
) {
    suspend operator fun invoke(
        planId: String,
        estimatedMonthlyIncome: Money,
        fixedExpenses: Money,
        initialBalance: Money = Money.ZERO,
        incomeProfile: IncomeProfile = IncomeProfile.FIXED,
        payFrequency: PayFrequency = PayFrequency.MONTHLY,
        budgetCycle: pe.kipu.core.domain.model.BudgetCycle = pe.kipu.core.domain.model.BudgetCycle.WEEKLY,
    ): Result<SaveFinancialPlanResult> {
        val existing = financialPlanRepository.getById(planId)
        val envelopeIds = existing?.envelopeIds
            ?: envelopeRepository.observeEnvelopes().first().map { it.id }

        val plan = FinancialPlan(
            id = planId,
            estimatedMonthlyIncome = estimatedMonthlyIncome,
            fixedExpenses = fixedExpenses,
            initialBalance = initialBalance,
            envelopeIds = envelopeIds,
            incomeProfile = incomeProfile,
            payFrequency = payFrequency,
            budgetCycle = budgetCycle,
            antSpendingLimit = existing?.antSpendingLimit,
            antSpendingAlertEnabled = existing?.antSpendingAlertEnabled ?: true,
            antSpendingAlertPercent = existing?.antSpendingAlertPercent ?: 80,
            antSpendingTrackedCategoryIds = existing?.antSpendingTrackedCategoryIds.orEmpty(),
        )

        when (val structural = plan.validate()) {
            is DomainResult.Err -> return Result.failure(IllegalArgumentException("Invalid plan"))
            is DomainResult.Ok -> Unit
        }

        val envelopes = envelopeRepository.observeEnvelopes().first()
        val commitments = commitmentRepository.observeCommitments().first()
        val validation = validateFinancialPlan(plan, envelopes, commitments)

        if (validation is FinancialPlanValidationResult.Invalid) {
            return Result.failure(InvalidFinancialPlanException(validation))
        }

        return financialPlanRepository.save(plan).map {
            SaveFinancialPlanResult(validation = validation)
        }
    }
}

class InvalidFinancialPlanException(
    val validation: FinancialPlanValidationResult.Invalid,
) : IllegalStateException("Financial plan does not balance for the month")
