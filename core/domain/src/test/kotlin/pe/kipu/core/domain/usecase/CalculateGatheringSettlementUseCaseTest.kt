package pe.kipu.core.domain.usecase

import pe.kipu.core.domain.model.SettlementDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.Gathering
import pe.kipu.core.domain.model.GatheringExpense
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.getOrError
import java.math.BigDecimal
import java.time.Instant

class CalculateGatheringSettlementUseCaseTest {

    private val useCase = CalculateGatheringSettlementUseCase(CalculateGatheringEqualSplitUseCase())

    @Test
    fun calculatesWhoPaidAndWhoOwes() {
        val gathering = Gathering(
            id = "gathering-1",
            name = "Cena",
            participantCount = 2,
            participantNames = listOf("Ana", "Luis"),
        )
        val expenses = listOf(
            expense("Ana", "40.00"),
            expense("Luis", "20.00"),
        )

        val result = useCase(gathering, expenses)

        assertTrue(result is DomainResult.Ok)
        val settlements = (result as DomainResult.Ok).value
        val ana = settlements.first { it.participantName == "Ana" }
        val luis = settlements.first { it.participantName == "Luis" }
        assertEquals(SettlementDirection.RECEIVES, ana.balanceDirection)
        assertEquals(Money.of(BigDecimal("10.00")).getOrError(), ana.balanceAmount)
        assertEquals(SettlementDirection.OWES, luis.balanceDirection)
        assertEquals(Money.of(BigDecimal("10.00")).getOrError(), luis.balanceAmount)
    }

    private fun expense(participant: String, amount: String): GatheringExpense {
        val parsedAmount = Money.of(BigDecimal(amount)).getOrError()
        return GatheringExpense(
            id = "expense-$participant",
            gatheringId = "gathering-1",
            amount = parsedAmount,
            paidByParticipant = participant,
            recordedAt = Instant.parse("2026-06-16T20:00:00Z"),
        )
    }
}
