package pe.kipu.app

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test
import pe.kipu.core.domain.model.BudgetCycle
import pe.kipu.feature.envelopes.presentation.daysRemainingInCycle
import pe.kipu.feature.envelopes.presentation.daysRemainingLabel

class EnvelopePresentationTimeZoneTest {

    @Test
    fun weeklyRemainingDaysUsesTheProvidedPeruDate() {
        val saturdayInPeru = LocalDate.of(2026, 6, 20)

        assertEquals(1, daysRemainingInCycle(BudgetCycle.WEEKLY, saturdayInPeru))
        assertEquals("1 día restante", daysRemainingLabel(BudgetCycle.WEEKLY, saturdayInPeru))
    }
}
