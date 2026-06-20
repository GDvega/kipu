package pe.kipu.core.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Test
import pe.kipu.core.data.local.entity.CommitmentEntity
import pe.kipu.core.domain.plan.CommitmentIds
import pe.kipu.core.domain.model.Commitment
import pe.kipu.core.domain.model.CommitmentType
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.getOrError
import java.math.BigDecimal
import java.time.LocalDate

class CommitmentMapperTest {

    @Test
    fun `entity to domain to entity round trip`() {
        val original = CommitmentEntity(
            id = CommitmentIds.EMERGENCY_FUND,
            type = CommitmentType.SAVINGS_GOAL.name,
            title = "Fondo emergencia",
            targetAmountCents = 50_000L,
            currentAmountCents = 12_000L,
            dueDateEpochDay = LocalDate.of(2026, 12, 31).toEpochDay(),
            counterpartyName = null,
            isSettled = false,
        )

        val domain = original.toDomain()
        val roundTrip = domain.toEntity()

        assertEquals(original, roundTrip)
        assertEquals(BigDecimal("500.00"), domain.targetAmount?.amount)
        assertEquals(BigDecimal("120.00"), domain.currentAmount?.amount)
    }

    @Test
    fun `maps social debt without target amount`() {
        val entity = CommitmentEntity(
            id = "commitment-debt-juan",
            type = CommitmentType.SOCIAL_DEBT.name,
            title = "Deuda con Juan",
            targetAmountCents = null,
            currentAmountCents = 8_000L,
            dueDateEpochDay = null,
            counterpartyName = "Juan",
            isSettled = false,
        )

        val domain = entity.toDomain()

        assertEquals(CommitmentType.SOCIAL_DEBT, domain.type)
        assertEquals(Money.of(BigDecimal("80.00")).getOrError(), domain.currentAmount)
    }
}
