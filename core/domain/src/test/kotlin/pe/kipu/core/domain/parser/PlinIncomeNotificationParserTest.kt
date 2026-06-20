package pe.kipu.core.domain.parser

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementType
import pe.kipu.core.domain.model.NotificationParseResult
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.SuggestionConfidence
import pe.kipu.core.domain.test.NotificationFixtureLoader

class PlinIncomeNotificationParserTest {

    private val parser = PlinIncomeNotificationParser()

    @Test
    fun `extracts amount and income type from standard fixture`() {
        val text = NotificationFixtureLoader.load("notifications/plin_income_standard.txt")
        val result = parser.parse(text) as NotificationParseResult.Success

        assertEquals(MovementType.INCOME, result.suggestion.type)
        assertEquals(BigDecimal("100.00"), result.suggestion.amount?.amount)
    }

    @Test
    fun `extracts counterparty when present`() {
        val text = NotificationFixtureLoader.load("notifications/plin_income_standard.txt")
        val result = parser.parse(text) as NotificationParseResult.Success

        assertEquals("Ana López García", result.suggestion.counterpartyName)
    }

    @Test
    fun `rejects outgoing payment notification`() {
        val text = NotificationFixtureLoader.load("notifications/plin_expense_notification.txt")
        val result = parser.parse(text)

        assertTrue(result is NotificationParseResult.Failure)
    }

    @Test
    fun `rejects text without income signal`() {
        val text = NotificationFixtureLoader.load("notifications/yape_no_income_signal.txt")
        val result = parser.parse(text)

        assertTrue(result is NotificationParseResult.Failure)
    }

    @Test
    fun `sets notification source and plin channel`() {
        val text = NotificationFixtureLoader.load("notifications/plin_income_standard.txt")
        val result = parser.parse(text) as NotificationParseResult.Success

        assertEquals(MovementSource.NOTIFICATION, result.suggestion.source)
        assertEquals(PaymentChannel.PLIN, result.suggestion.channel)
        assertEquals(SuggestionConfidence.HIGH, result.suggestion.confidence)
    }
}
