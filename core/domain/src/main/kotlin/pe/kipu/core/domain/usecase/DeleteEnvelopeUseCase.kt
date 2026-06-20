package pe.kipu.core.domain.usecase

import javax.inject.Inject
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.plan.FinancialPlanIds
import pe.kipu.core.domain.repository.EnvelopeRepository
import pe.kipu.core.domain.repository.FinancialPlanRepository

class DeleteEnvelopeUseCase @Inject constructor(
    private val envelopeRepository: EnvelopeRepository,
    private val financialPlanRepository: FinancialPlanRepository,
) {
    suspend operator fun invoke(envelopeId: EntityId): Result<Unit> {
        if (envelopeRepository.getById(envelopeId) == null) {
            return Result.failure(IllegalArgumentException("Envelope not found"))
        }

        envelopeRepository.delete(envelopeId).getOrElse { return Result.failure(it) }

        val plan = financialPlanRepository.getById(FinancialPlanIds.PRIMARY)
        if (plan != null && envelopeId in plan.envelopeIds) {
            financialPlanRepository.save(
                plan.copy(envelopeIds = plan.envelopeIds.filterNot { it == envelopeId }),
            ).getOrElse { return Result.failure(it) }
        }

        return Result.success(Unit)
    }
}
