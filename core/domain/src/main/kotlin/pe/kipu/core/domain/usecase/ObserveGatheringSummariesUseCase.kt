package pe.kipu.core.domain.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.GatheringSummary
import pe.kipu.core.domain.model.GatheringsDashboard
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.repository.GatheringExpenseRepository
import pe.kipu.core.domain.repository.GatheringRepository
import pe.kipu.core.domain.repository.MovementRepository

class ObserveGatheringSummariesUseCase @Inject constructor(
    private val gatheringRepository: GatheringRepository,
    private val gatheringExpenseRepository: GatheringExpenseRepository,
    private val movementRepository: MovementRepository,
    private val calculateGatheringEqualSplit: CalculateGatheringEqualSplitUseCase,
    private val calculateGatheringSettlement: CalculateGatheringSettlementUseCase,
) {
    operator fun invoke(): Flow<GatheringsDashboard> = combine(
        gatheringRepository.observeGatherings(),
        gatheringExpenseRepository.observeExpensesByGathering(),
        movementRepository.observeMovements(),
        gatheringExpenseRepository.observeLinkedMovementIds(),
    ) { gatherings, expensesByGathering, movements, linkedMovementIds ->
        val summaries = gatherings.map { gathering ->
            val expenses = expensesByGathering[gathering.id].orEmpty()
            val totalExpenses = expenses.fold(Money.ZERO) { acc, expense -> acc + expense.amount }
            val perPerson = calculateGatheringEqualSplit(totalExpenses, gathering.participantCount)
                .let { result ->
                    when (result) {
                        is DomainResult.Ok -> result.value
                        is DomainResult.Err -> Money.ZERO
                    }
                }
            val settlements = calculateGatheringSettlement(gathering, expenses)
                .let { result ->
                    when (result) {
                        is DomainResult.Ok -> result.value
                        is DomainResult.Err -> emptyList()
                    }
                }
            GatheringSummary(
                gathering = gathering,
                totalExpenses = totalExpenses,
                perPersonAmount = perPerson,
                settlements = settlements,
            )
        }
        val unlinkedMovements = movements.filter { movement ->
            movement.type == MovementType.EXPENSE &&
                movement.status == MovementStatus.CONFIRMED &&
                movement.id !in linkedMovementIds
        }
        GatheringsDashboard(
            summaries = summaries,
            unlinkedMovements = unlinkedMovements,
        )
    }
}
