package pe.kipu.core.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import pe.kipu.core.data.local.entity.FinancialPlanEntity
import pe.kipu.core.data.local.seed.DefaultEnvelopeIds
import pe.kipu.core.domain.plan.FinancialPlanIds
import pe.kipu.core.domain.plan.IncomeProfile
import pe.kipu.core.domain.plan.PayFrequency
import pe.kipu.core.domain.model.FinancialPlan
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.BudgetCycle
import pe.kipu.core.domain.model.getOrError
import java.math.BigDecimal

class FinancialPlanMapperTest {

    @Test
    fun `entity to domain to entity round trip`() {
        val original = FinancialPlanEntity(
            id = FinancialPlanIds.PRIMARY,
            estimatedMonthlyIncomeCents = 300_000L,
            fixedExpensesCents = 180_000L,
            initialBalanceCents = 50_000L,
            envelopeIds = listOf(
                DefaultEnvelopeIds.FOOD,
                DefaultEnvelopeIds.TRANSPORT,
            ).joinToString(","),
            incomeProfile = "VARIABLE",
            payFrequency = "BIWEEKLY",
            budgetCycle = "MONTHLY",
            antSpendingLimitCents = 7_500L,
            antSpendingAlertEnabled = false,
            antSpendingAlertPercent = 75,
            antSpendingTrackedCategoryIds = "category-coffee,category-food",
            reserveMonthlyContributionCents = 20_000L,
        )

        val domain = original.toDomain()
        val roundTrip = domain.toEntity()

        assertEquals(original, roundTrip)
        assertEquals(BigDecimal("3000.00"), domain.estimatedMonthlyIncome.amount)
        assertEquals(BigDecimal("500.00"), domain.initialBalance.amount)
        assertEquals(2, domain.envelopeIds.size)
        assertEquals(IncomeProfile.VARIABLE, domain.incomeProfile)
        assertEquals(PayFrequency.BIWEEKLY, domain.payFrequency)
        assertEquals(BudgetCycle.MONTHLY, domain.budgetCycle)
        assertEquals(BigDecimal("75.00"), domain.antSpendingLimit?.amount)
        assertFalse(domain.antSpendingAlertEnabled)
        assertEquals(75, domain.antSpendingAlertPercent)
        assertEquals(BigDecimal("200.00"), domain.reserveMonthlyContribution.amount)
        assertEquals(
            setOf("category-food", "category-coffee"),
            domain.antSpendingTrackedCategoryIds,
        )
    }

    @Test
    fun `maps empty envelope ids`() {
        val plan = FinancialPlan(
            id = "plan-empty",
            estimatedMonthlyIncome = Money.of(BigDecimal("1000.00")).getOrError(),
            fixedExpenses = Money.of(BigDecimal("500.00")).getOrError(),
            envelopeIds = emptyList(),
        )

        val entity = plan.toEntity()
        val domain = entity.toDomain()

        assertEquals("", entity.envelopeIds)
        assertEquals(emptyList<String>(), domain.envelopeIds)
    }
}
