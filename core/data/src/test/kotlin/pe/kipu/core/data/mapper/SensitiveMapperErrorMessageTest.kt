package pe.kipu.core.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import pe.kipu.core.data.local.entity.CommitmentEntity
import pe.kipu.core.data.local.entity.EnvelopeEntity
import pe.kipu.core.data.local.entity.FinancialPlanEntity
import pe.kipu.core.data.local.entity.MonthlyServiceReceiptEntity
import pe.kipu.core.data.local.seed.DefaultEnvelopeIds
import pe.kipu.core.data.test.MapperTestFixtures
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.CommitmentType

class SensitiveMapperErrorMessageTest {

    @Test
    fun `movement mapper error omits stored amount`() {
        val exception = assertThrows(IllegalStateException::class.java) {
            MapperTestFixtures.sampleMovementEntity().copy(amountCents = -12_345L).toDomain()
        }

        assertEquals("Invalid stored amount cents", exception.message)
    }

    @Test
    fun `envelope mapper error omits stored amount`() {
        val exception = assertThrows(IllegalStateException::class.java) {
            EnvelopeEntity(
                id = DefaultEnvelopeIds.FOOD,
                name = "Comida",
                weeklyLimitCents = -12_345L,
                categoryId = CategoryIds.FOOD,
            ).toDomain()
        }

        assertEquals("Invalid stored weekly limit cents", exception.message)
    }

    @Test
    fun `commitment mapper error omits stored amount`() {
        val exception = assertThrows(IllegalStateException::class.java) {
            CommitmentEntity(
                id = "commitment-invalid",
                type = CommitmentType.SAVINGS_GOAL.name,
                title = "Meta",
                targetAmountCents = -12_345L,
                currentAmountCents = 0L,
                dueDateEpochDay = null,
                counterpartyName = null,
                isSettled = false,
            ).toDomain()
        }

        assertEquals("Invalid stored commitment amount cents", exception.message)
    }

    @Test
    fun `financial plan mapper error omits stored amount`() {
        val exception = assertThrows(IllegalStateException::class.java) {
            FinancialPlanEntity(
                id = "plan-invalid",
                estimatedMonthlyIncomeCents = -12_345L,
                fixedExpensesCents = 0L,
                envelopeIds = "",
            ).toDomain()
        }

        assertEquals("Invalid stored financial plan amount cents", exception.message)
    }

    @Test
    fun `monthly receipt mapper error omits stored amount`() {
        val exception = assertThrows(IllegalStateException::class.java) {
            MonthlyServiceReceiptEntity(
                id = "2026-08_LIGHT",
                monthKey = "2026-08",
                serviceKeyIdentifier = "LIGHT",
                title = "Luz",
                configuredAmountCents = -12_345L,
                isPaid = false,
            ).toDomain()
        }

        assertEquals("Invalid stored receipt amount cents", exception.message)
    }
}
