package pe.kipu.core.domain.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import pe.kipu.core.domain.model.CommitmentsInsights
import pe.kipu.core.domain.repository.EnvelopeRepository
import pe.kipu.core.domain.repository.FinancialPlanRepository
import pe.kipu.core.domain.repository.MovementRepository

class ObserveCommitmentsInsightsUseCase @Inject constructor(
    private val observeCommitmentSummaries: ObserveCommitmentSummariesUseCase,
    private val financialPlanRepository: FinancialPlanRepository,
    private val envelopeRepository: EnvelopeRepository,
    private val movementRepository: MovementRepository,
    private val validateFinancialPlan: ValidateFinancialPlanUseCase,
) {

    operator fun invoke(): Flow<CommitmentsInsights> =
        combine(
            observeCommitmentSummaries(),
            financialPlanRepository.observePlans(),
            envelopeRepository.observeEnvelopes(),
            movementRepository.observeMovements(),
        ) { summaries, plans, envelopes, movements ->
            val linkedIncomeByCommitmentId = summaries.associate { summary ->
                summary.commitment.id to CommitmentLinkedIncomeCalculator.sumLinkedIncome(
                    summary.commitment.id,
                    movements,
                )
            }
            val planValidation = plans.firstOrNull()?.let { plan ->
                validateFinancialPlan(
                    plan = plan,
                    envelopes = envelopes,
                    commitments = summaries.map { summary -> summary.commitment },
                    linkedIncomeByCommitmentId = linkedIncomeByCommitmentId,
                )
            }
            CommitmentsInsights(
                summaries = summaries,
                planValidation = planValidation,
            )
        }
}
