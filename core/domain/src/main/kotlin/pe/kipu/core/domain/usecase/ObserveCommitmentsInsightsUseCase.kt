package pe.kipu.core.domain.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import pe.kipu.core.domain.model.CommitmentsInsights
import pe.kipu.core.domain.repository.EnvelopeRepository
import pe.kipu.core.domain.repository.FinancialPlanRepository

class ObserveCommitmentsInsightsUseCase @Inject constructor(
    private val observeCommitmentSummaries: ObserveCommitmentSummariesUseCase,
    private val financialPlanRepository: FinancialPlanRepository,
    private val envelopeRepository: EnvelopeRepository,
    private val validateFinancialPlan: ValidateFinancialPlanUseCase,
) {

    operator fun invoke(): Flow<CommitmentsInsights> =
        combine(
            observeCommitmentSummaries(),
            financialPlanRepository.observePlans(),
            envelopeRepository.observeEnvelopes(),
        ) { summaries, plans, envelopes ->
            val planValidation = plans.firstOrNull()?.let { plan ->
                validateFinancialPlan(
                    plan = plan,
                    envelopes = envelopes,
                    commitments = summaries.map { summary -> summary.commitment },
                )
            }
            CommitmentsInsights(
                summaries = summaries,
                planValidation = planValidation,
            )
        }
}
