package pe.kipu.core.domain.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.first
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.Envelope
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.plan.FinancialPlanIds
import pe.kipu.core.domain.repository.CategoryRepository
import pe.kipu.core.domain.repository.EnvelopePlanRepository
import pe.kipu.core.domain.repository.EnvelopeRepository
import pe.kipu.core.domain.repository.FinancialPlanRepository
import pe.kipu.core.domain.time.TimeProvider

class CreateEnvelopeUseCase @Inject constructor(
    private val envelopeRepository: EnvelopeRepository,
    private val categoryRepository: CategoryRepository,
    private val financialPlanRepository: FinancialPlanRepository,
    private val envelopePlanRepository: EnvelopePlanRepository,
    private val timeProvider: TimeProvider,
) {
    suspend operator fun invoke(
        name: String,
        categoryId: EntityId,
        weeklyLimit: Money,
    ): Result<Unit> {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Envelope name is required"))
        }

        if (categoryRepository.getById(categoryId) == null) {
            return Result.failure(IllegalArgumentException("Category not found"))
        }

        val existingEnvelopes = envelopeRepository.observeEnvelopes().first()
        if (existingEnvelopes.any { it.categoryId == categoryId }) {
            return Result.failure(IllegalArgumentException("This category already has an envelope"))
        }

        val envelope = Envelope(
            id = "envelope-${timeProvider.now().toEpochMilli()}",
            name = trimmedName,
            weeklyLimit = weeklyLimit,
            categoryId = categoryId,
        )

        when (val validation = envelope.validate()) {
            is DomainResult.Err -> return Result.failure(IllegalArgumentException(validation.error.message))
            is DomainResult.Ok -> Unit
        }

        val plan = financialPlanRepository.getById(FinancialPlanIds.PRIMARY)
        val updatedPlan = plan?.let { current ->
            if (envelope.id in current.envelopeIds) current
            else current.copy(envelopeIds = current.envelopeIds + envelope.id)
        }
        return envelopePlanRepository.saveEnvelopeWithPlan(envelope, updatedPlan)
    }
}
