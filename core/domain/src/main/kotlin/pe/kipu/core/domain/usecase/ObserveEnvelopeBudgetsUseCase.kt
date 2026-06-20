package pe.kipu.core.domain.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import pe.kipu.core.domain.model.EnvelopeBudgetState
import pe.kipu.core.domain.repository.EnvelopeRepository
import pe.kipu.core.domain.repository.MovementRepository
import pe.kipu.core.domain.time.TimeProvider
import pe.kipu.core.domain.time.WeekRangeCalculator
import pe.kipu.core.domain.time.refreshTicks

class ObserveEnvelopeBudgetsUseCase @Inject constructor(
    private val envelopeRepository: EnvelopeRepository,
    private val movementRepository: MovementRepository,
    private val calculateEnvelopeBudgetState: CalculateEnvelopeBudgetStateUseCase,
    private val weekRangeCalculator: WeekRangeCalculator,
    private val timeProvider: TimeProvider,
) {

    operator fun invoke(): Flow<List<EnvelopeBudgetState>> =
        combine(
            envelopeRepository.observeEnvelopes(),
            movementRepository.observeMovements(),
            timeProvider.refreshTicks(),
        ) { envelopes, movements, referenceInstant ->
            val weekRange = weekRangeCalculator.currentWeekRange(referenceInstant)
            envelopes.map { envelope ->
                calculateEnvelopeBudgetState(
                    envelope = envelope,
                    movements = movements,
                    weekRange = weekRange,
                )
            }
        }
}
