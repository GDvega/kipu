package pe.kipu.core.domain.usecase

import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import pe.kipu.core.domain.model.BudgetCycle
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.Envelope
import pe.kipu.core.domain.model.EnvelopeBudgetState
import pe.kipu.core.domain.model.FinancialPlan
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.repository.EnvelopeRepository
import pe.kipu.core.domain.repository.FinancialPlanRepository
import pe.kipu.core.domain.repository.GatheringExpenseRepository
import pe.kipu.core.domain.repository.MonthlyServiceReceiptRepository
import pe.kipu.core.domain.repository.MovementRepository
import pe.kipu.core.domain.time.TimeProvider
import pe.kipu.core.domain.time.CycleRangeCalculator
import pe.kipu.core.domain.time.refreshTicks

class ObserveEnvelopeBudgetsUseCase @Inject constructor(
    private val envelopeRepository: EnvelopeRepository,
    private val movementRepository: MovementRepository,
    private val gatheringExpenseRepository: GatheringExpenseRepository,
    private val monthlyServiceReceiptRepository: MonthlyServiceReceiptRepository,
    private val financialPlanRepository: FinancialPlanRepository,
    private val calculateEnvelopeBudgetState: CalculateEnvelopeBudgetStateUseCase,
    private val cycleRangeCalculator: CycleRangeCalculator,
    private val timeProvider: TimeProvider,
) {

    operator fun invoke(): Flow<List<EnvelopeBudgetState>> =
        combine(
            envelopeRepository.observeEnvelopes(),
            movementRepository.observeMovements(),
            gatheringExpenseRepository.observeActiveGatheringLinkedMovementIds(),
            monthlyServiceReceiptRepository.observeAllPaidMovementIds(),
            financialPlanRepository.observePlans(),
            timeProvider.refreshTicks(),
        ) { args: Array<Any?> ->
            @Suppress("UNCHECKED_CAST")
            val envelopes = args[0] as List<Envelope>
            @Suppress("UNCHECKED_CAST")
            val movements = args[1] as List<Movement>
            @Suppress("UNCHECKED_CAST")
            val gatheringLinkedIds = args[2] as Set<EntityId>
            @Suppress("UNCHECKED_CAST")
            val paidReceiptMovementIds = args[3] as Set<String>
            @Suppress("UNCHECKED_CAST")
            val plans = args[4] as List<FinancialPlan>
            val referenceInstant = args[5] as Instant

            val cycle = plans.firstOrNull()?.budgetCycle ?: BudgetCycle.WEEKLY
            val cycleRange = cycleRangeCalculator.currentCycleRange(cycle, referenceInstant)
            val allExcludedIds = gatheringLinkedIds + paidReceiptMovementIds
            envelopes.map { envelope ->
                calculateEnvelopeBudgetState(
                    envelope = envelope,
                    movements = movements,
                    cycleRange = cycleRange,
                    gatheringLinkedMovementIds = allExcludedIds,
                    allEnvelopes = envelopes,
                )
            }
        }
}
