package pe.kipu.core.domain.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import pe.kipu.core.domain.model.EnvelopeBudgetState
import pe.kipu.core.domain.repository.EnvelopeRepository
import pe.kipu.core.domain.repository.GatheringExpenseRepository
import pe.kipu.core.domain.repository.MovementRepository
import pe.kipu.core.domain.time.TimeProvider
import pe.kipu.core.domain.time.CycleRangeCalculator
import pe.kipu.core.domain.time.refreshTicks

class ObserveEnvelopeBudgetsUseCase @Inject constructor(
    private val envelopeRepository: EnvelopeRepository,
    private val movementRepository: MovementRepository,
    private val gatheringExpenseRepository: GatheringExpenseRepository,
    private val calculateEnvelopeBudgetState: CalculateEnvelopeBudgetStateUseCase,
    private val cycleRangeCalculator: CycleRangeCalculator,
    private val timeProvider: TimeProvider,
) {

    operator fun invoke(): Flow<List<EnvelopeBudgetState>> =
        combine(
            envelopeRepository.observeEnvelopes(),
            movementRepository.observeMovements(),
            gatheringExpenseRepository.observeActiveGatheringLinkedMovementIds(),
            timeProvider.refreshTicks(),
        ) { envelopes, movements, gatheringLinkedIds, referenceInstant ->
            val cycleRange = cycleRangeCalculator.currentCycleRange(pe.kipu.core.domain.model.BudgetCycle.WEEKLY, referenceInstant)
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
