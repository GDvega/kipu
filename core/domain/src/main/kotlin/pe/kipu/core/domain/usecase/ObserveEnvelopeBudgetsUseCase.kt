package pe.kipu.core.domain.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import pe.kipu.core.domain.model.BudgetCycle
import pe.kipu.core.domain.model.EnvelopeBudgetState
import pe.kipu.core.domain.repository.EnvelopeRepository
import pe.kipu.core.domain.repository.FinancialPlanRepository
import pe.kipu.core.domain.repository.GatheringExpenseRepository
import pe.kipu.core.domain.repository.MovementRepository
import pe.kipu.core.domain.time.TimeProvider
import pe.kipu.core.domain.time.CycleRangeCalculator
import pe.kipu.core.domain.time.refreshTicks

class ObserveEnvelopeBudgetsUseCase @Inject constructor(
    private val envelopeRepository: EnvelopeRepository,
    private val movementRepository: MovementRepository,
    private val gatheringExpenseRepository: GatheringExpenseRepository,
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
            financialPlanRepository.observePlans(),
            timeProvider.refreshTicks(),
        ) { envelopes, movements, gatheringLinkedIds, plans, referenceInstant ->
            val cycle = plans.firstOrNull()?.budgetCycle ?: BudgetCycle.WEEKLY
            val cycleRange = cycleRangeCalculator.currentCycleRange(cycle, referenceInstant)
            envelopes.map { envelope ->
                calculateEnvelopeBudgetState(
                    envelope = envelope,
                    movements = movements,
                    cycleRange = cycleRange,
                    gatheringLinkedMovementIds = gatheringLinkedIds,
                )
            }
        }
}
