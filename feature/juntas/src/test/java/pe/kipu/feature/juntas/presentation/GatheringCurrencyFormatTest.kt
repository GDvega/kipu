package pe.kipu.feature.juntas.presentation

import java.math.BigDecimal
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.ParticipantSettlement
import pe.kipu.core.domain.model.SettlementDirection
import pe.kipu.core.domain.model.getOrError

class GatheringCurrencyFormatTest {

    @Test
    fun settlementLabelDoesNotDuplicateCurrencyPrefix() {
        val settlement = ParticipantSettlement(
            participantName = "Luis",
            paidAmount = Money.of(BigDecimal("0.00")).getOrError(),
            fairShare = Money.of(BigDecimal("10.00")).getOrError(),
            balanceAmount = Money.of(BigDecimal("10.00")).getOrError(),
            balanceDirection = SettlementDirection.OWES,
        )

        val label = GatheringCurrencyFormatter.settlementLabel(settlement)

        assertTrue(label.contains("S/ 10.00"))
        assertFalse(label.contains("S/ S/"))
    }
}
