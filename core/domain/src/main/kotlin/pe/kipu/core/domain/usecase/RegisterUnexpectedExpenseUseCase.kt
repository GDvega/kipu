package pe.kipu.core.domain.usecase

import javax.inject.Inject
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.UnexpectedExpenseRecoveryPlan
import pe.kipu.core.domain.model.UnexpectedExpensePreview
import pe.kipu.core.domain.repository.DirectLocalTransactionRunner
import pe.kipu.core.domain.repository.LocalTransactionRunner

class RegisterUnexpectedExpenseUseCase @Inject constructor(
    private val createManualMovement: CreateManualMovementUseCase,
    private val applyRecoveryPlan: ApplyRecoveryPlanUseCase,
    private val prepareUnexpectedExpense: PrepareUnexpectedExpenseUseCase,
    private val localTransactionRunner: LocalTransactionRunner = DirectLocalTransactionRunner,
) {
    suspend operator fun invoke(
        amount: Money,
        categoryId: EntityId,
        channel: PaymentChannel,
        expectedPreview: UnexpectedExpensePreview,
        description: String? = null,
        counterpartyName: String? = null,
        envelopeId: EntityId? = null,
        reserveAmount: Money = Money.ZERO,
        recoveryPlan: UnexpectedExpenseRecoveryPlan? = null,
    ): Result<Unit> = localTransactionRunner.run {
        requireNotNull(expectedPreview.snapshot) { "A current preview is required" }
        val current = prepareUnexpectedExpense(amount)
        require(current == expectedPreview) { "Financial inputs changed; review the purchase again" }
        require(reserveAmount == current.coverage.fromReserve) { "Reserve allocation changed" }
        recoveryPlan?.let { proposal ->
            val ids = proposal.adjustments.map { it.envelopeId }.toSet()
            require(proposal == current.recoveryPlanFor(ids)) { "Recovery proposal changed" }
        }
        createManualMovement(
            type = MovementType.EXPENSE,
            amount = amount,
            categoryId = categoryId,
            channel = channel,
            description = description,
            counterpartyName = counterpartyName,
            envelopeId = envelopeId,
            reserveAmount = reserveAmount,
        ).getOrThrow()
        recoveryPlan?.takeIf { it.adjustments.isNotEmpty() }?.let { proposal ->
            applyRecoveryPlan(proposal, prepareUnexpectedExpense.currentBudgets()).getOrThrow()
        }
    }.map { Unit }
}
