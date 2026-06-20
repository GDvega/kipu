package pe.kipu.core.domain.usecase

import javax.inject.Inject
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.repository.EnvelopeRepository

class UpdateEnvelopeWeeklyLimitUseCase @Inject constructor(
    private val envelopeRepository: EnvelopeRepository,
) {
    suspend operator fun invoke(
        envelopeId: EntityId,
        weeklyLimit: Money,
    ): Result<Unit> {
        val envelope = envelopeRepository.getById(envelopeId)
            ?: return Result.failure(IllegalArgumentException("Envelope not found"))

        val updated = envelope.copy(weeklyLimit = weeklyLimit)
        return when (val validation = updated.validate()) {
            is DomainResult.Err -> Result.failure(IllegalArgumentException("Invalid envelope limit"))
            is DomainResult.Ok -> envelopeRepository.save(updated)
        }
    }
}
