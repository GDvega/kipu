package pe.kipu.core.domain.usecase

import javax.inject.Inject
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.plan.FinancialPlanIds
import pe.kipu.core.domain.repository.EnvelopeRepository
import pe.kipu.core.domain.repository.EnvelopePlanRepository
import pe.kipu.core.domain.repository.FinancialPlanRepository

class DeleteEnvelopeUseCase @Inject constructor(
    private val envelopeRepository: EnvelopeRepository,
    private val financialPlanRepository: FinancialPlanRepository,
    private val envelopePlanRepository: EnvelopePlanRepository,
) {
    suspend operator fun invoke(envelopeId: EntityId): Result<Unit> {
        if (envelopeRepository.getById(envelopeId) == null) {
            return Result.failure(IllegalArgumentException("Envelope not found"))
        }

        val plan = financialPlanRepository.getById(FinancialPlanIds.PRIMARY)
        val updatedPlan = plan?.let { current ->
            if (envelopeId !in current.envelopeIds) current
            else current.copy(envelopeIds = current.envelopeIds.filterNot { it == envelopeId })
        }
        return envelopePlanRepository.deleteEnvelopeWithPlan(envelopeId, updatedPlan)
    }
}
