package pe.kipu.core.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.Money
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.getOrError
import pe.kipu.core.domain.voice.VoiceFinancialIntent
import pe.kipu.core.domain.voice.VoiceIntentAnalyzer
import java.math.BigDecimal

class AnalyzeVoiceIntentUseCaseTest {

    private class FakeVoiceIntentAnalyzer(
        var response: VoiceFinancialIntent,
    ) : VoiceIntentAnalyzer {
        var lastAnalyzedText: String? = null

        override suspend fun analyze(rawText: String): VoiceFinancialIntent {
            lastAnalyzedText = rawText
            return response
        }
    }

    @Test
    fun `delegates analysis to injected VoiceIntentAnalyzer`() = runTest {
        val expectedExpense = VoiceFinancialIntent.Expense(
            rawText = "Gasté 5 soles en comida",
            amount = Money.of(BigDecimal("5.00")).getOrError(),
            categoryId = CategoryIds.FOOD,
            description = "Comida",
            channel = PaymentChannel.CASH,
        )
        val fakeAnalyzer = FakeVoiceIntentAnalyzer(response = expectedExpense)
        val useCase = AnalyzeVoiceIntentUseCase(voiceIntentAnalyzer = fakeAnalyzer)

        val result = useCase("Gasté 5 soles en comida")

        assertEquals("Gasté 5 soles en comida", fakeAnalyzer.lastAnalyzedText)
        assertEquals(expectedExpense, result)
    }
}
