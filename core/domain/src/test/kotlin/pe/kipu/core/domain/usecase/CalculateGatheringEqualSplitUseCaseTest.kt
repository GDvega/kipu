package pe.kipu.core.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.getOrError
import java.math.BigDecimal

class CalculateGatheringEqualSplitUseCaseTest {

    private val useCase = CalculateGatheringEqualSplitUseCase()

    @Test
    fun splitsTotalEquallyAmongParticipants() {
        val total = Money.of(BigDecimal("100.00")).getOrError()

        val result = useCase(total, participantCount = 4)

        assertTrue(result is DomainResult.Ok)
        assertEquals(
            Money.of(BigDecimal("25.00")).getOrError(),
            (result as DomainResult.Ok).value,
        )
    }

    @Test
    fun rejectsZeroParticipants() {
        val total = Money.of(BigDecimal("10.00")).getOrError()

        val result = useCase(total, participantCount = 0)

        assertTrue(result is DomainResult.Err)
    }
}
