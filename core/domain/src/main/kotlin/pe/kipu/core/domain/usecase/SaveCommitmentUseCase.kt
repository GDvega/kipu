package pe.kipu.core.domain.usecase

import javax.inject.Inject
import pe.kipu.core.domain.model.Commitment
import pe.kipu.core.domain.model.CommitmentType
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.plan.GoalCurrency
import pe.kipu.core.domain.repository.CommitmentRepository
import pe.kipu.core.domain.time.TimeProvider

class SaveCommitmentUseCase @Inject constructor(
    private val commitmentRepository: CommitmentRepository,
    private val timeProvider: TimeProvider,
) {
    suspend operator fun invoke(
        existingId: EntityId? = null,
        type: CommitmentType,
        title: String,
        targetAmount: Money? = null,
        currentAmount: Money? = null,
        counterpartyName: String? = null,
    ): Result<Unit> {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) {
            return Result.failure(IllegalArgumentException("Title is required"))
        }

        val id = existingId ?: "commitment-${timeProvider.now().toEpochMilli()}"
        val existing = existingId?.let { commitmentRepository.getById(it) }

        val commitment = Commitment(
            id = id,
            type = type,
            title = trimmedTitle,
            targetAmount = targetAmount,
            currentAmount = currentAmount,
            counterpartyName = counterpartyName?.trim()?.takeIf { it.isNotEmpty() },
            isSettled = existing?.isSettled ?: false,
            currencyCode = existing?.currencyCode ?: GoalCurrency.PEN.code,
        )

        when (val validation = commitment.validate()) {
            is DomainResult.Err -> return Result.failure(IllegalArgumentException(validation.error.message))
            is DomainResult.Ok -> Unit
        }

        return commitmentRepository.save(commitment)
    }
}
