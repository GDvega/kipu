package pe.kipu.feature.home.presentation

import org.junit.Assert.assertEquals
import org.junit.Test
import pe.kipu.core.domain.model.BudgetCycle

class HomeCycleTextTest {
    @Test
    fun `hero identifies the daily unit for every budget cycle`() {
        assertEquals("DISPONIBLE HOY", HomeCycleText.heroHeader(BudgetCycle.DAILY))
        assertEquals("POR DÍA ESTA SEMANA", HomeCycleText.heroHeader(BudgetCycle.WEEKLY))
        assertEquals("POR DÍA ESTE MES", HomeCycleText.heroHeader(BudgetCycle.MONTHLY))
    }
}
