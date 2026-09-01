package pe.kipu.core.domain.voice

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.getOrError
import java.math.BigDecimal

class LocalVoiceIntentAnalyzerTest {

    private val analyzer = LocalVoiceIntentAnalyzer()

    @Test
    fun `analyzes expense accurately using local parser`() = runTest {
        val result = analyzer.analyze("Gasté 5 soles en comida")

        assertTrue(result is VoiceFinancialIntent.Expense)
        val expense = result as VoiceFinancialIntent.Expense
        assertEquals(Money.of(BigDecimal("5.00")).getOrError(), expense.amount)
        assertEquals(CategoryIds.FOOD, expense.categoryId)
        assertEquals(PaymentChannel.CASH, expense.channel)
        assertEquals("Comida", expense.description)
    }

    @Test
    fun `analyzes income accurately using local parser`() = runTest {
        val result = analyzer.analyze("Me pagaron 200 soles de sueldo")

        assertTrue(result is VoiceFinancialIntent.Income)
        val income = result as VoiceFinancialIntent.Income
        assertEquals(Money.of(BigDecimal("200.00")).getOrError(), income.amount)
        assertEquals(CategoryIds.OTHER, income.categoryId)
    }

    @Test
    fun `returns unknown for empty or unintelligible text`() = runTest {
        val result = analyzer.analyze("hola buenos días")

        assertTrue(result is VoiceFinancialIntent.Unknown)
    }
}
