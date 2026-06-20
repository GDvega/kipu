package pe.kipu.core.domain.usecase

import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.Gathering
import pe.kipu.core.domain.model.GatheringExpense
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.ParticipantSettlement
import pe.kipu.core.domain.model.SettlementDirection

class CalculateGatheringSettlementUseCase @Inject constructor(
    private val calculateGatheringEqualSplit: CalculateGatheringEqualSplitUseCase,
) {

    operator fun invoke(
        gathering: Gathering,
        expenses: List<GatheringExpense>,
    ): DomainResult<List<ParticipantSettlement>> {
        val total = expenses.fold(Money.ZERO) { acc, expense -> acc + expense.amount }
        val fairShare = when (val split = calculateGatheringEqualSplit(total, gathering.participantCount)) {
            is DomainResult.Err -> return split
            is DomainResult.Ok -> split.value
        }

        val paidByParticipant = gathering.participantNames.associateWith { participant ->
            expenses.filter { expense ->
                expense.paidByParticipant.equals(participant, ignoreCase = true)
            }.fold(Money.ZERO) { acc, expense -> acc + expense.amount }
        }

        val settlements = gathering.participantNames.map { participant ->
            val paidAmount = paidByParticipant[participant] ?: Money.ZERO
            val difference = paidAmount.amount.subtract(fairShare.amount)
            val direction = when (difference.signum()) {
                1 -> SettlementDirection.RECEIVES
                -1 -> SettlementDirection.OWES
                else -> SettlementDirection.SETTLED
            }
            val balanceAmount = when (val magnitude = Money.of(difference.abs().setScale(SCALE, RoundingMode.HALF_UP))) {
                is DomainResult.Ok -> magnitude.value
                is DomainResult.Err -> Money.ZERO
            }
            ParticipantSettlement(
                participantName = participant,
                paidAmount = paidAmount,
                fairShare = fairShare,
                balanceDirection = direction,
                balanceAmount = balanceAmount,
            )
        }

        return DomainResult.Ok(settlements)
    }

    private companion object {
        const val SCALE = 2
    }
}
