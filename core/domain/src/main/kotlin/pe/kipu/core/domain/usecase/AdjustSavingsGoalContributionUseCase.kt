package pe.kipu.core.domain.usecase

import javax.inject.Inject
import pe.kipu.core.domain.model.CommitmentType
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.repository.CommitmentRepository
import pe.kipu.core.domain.repository.DirectLocalTransactionRunner
import pe.kipu.core.domain.repository.LocalTransactionRunner

/** Adjusts declared savings, not external cash flow or already linked income. */
class AdjustSavingsGoalContributionUseCase @Inject constructor(
    private val commitmentRepository: CommitmentRepository,
    private val localTransactionRunner: LocalTransactionRunner = DirectLocalTransactionRunner,
) {
    suspend operator fun invoke(
        commitmentId: EntityId,
        amount: Money,
        isDeposit: Boolean,
    ): Result<Unit> = localTransactionRunner.run {
        require(!amount.isZero()) { "Contribution must be positive" }
        val goal = requireNotNull(commitmentRepository.getById(commitmentId)) { "Goal no longer exists" }
        require(goal.type == CommitmentType.SAVINGS_GOAL && !goal.isSettled) { "Goal is not active" }
        require(goal.currencyCode == Money.CURRENCY_CODE) { "Contribution currency does not match" }
        val current = goal.currentAmount ?: Money.ZERO
        val updated = if (isDeposit) current + amount else (current - amount).getOrError()
        commitmentRepository.save(goal.copy(currentAmount = updated)).getOrThrow()
    }
}
