package pe.kipu.feature.juntas.presentation

import pe.kipu.core.designsystem.component.formatPenAmountForDisplay
import pe.kipu.core.domain.model.ParticipantSettlement
import pe.kipu.core.domain.model.SettlementDirection

object GatheringCurrencyFormatter {
    fun settlementLabel(settlement: ParticipantSettlement): String {
        val amount = formatPenAmountForDisplay(settlement.balanceAmount.amount)
        return when (settlement.balanceDirection) {
            SettlementDirection.RECEIVES -> "${settlement.participantName}: le deben $amount"
            SettlementDirection.OWES -> "${settlement.participantName}: debe $amount"
            SettlementDirection.SETTLED -> "${settlement.participantName}: al día"
        }
    }
}
