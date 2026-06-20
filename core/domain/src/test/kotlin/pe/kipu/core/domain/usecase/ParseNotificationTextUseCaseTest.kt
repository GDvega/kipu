package pe.kipu.core.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.NotificationParseResult
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.notification.MonitoredPaymentApps
import pe.kipu.core.domain.parser.NotificationParserRouter
import pe.kipu.core.domain.parser.PlinIncomeNotificationParser
import pe.kipu.core.domain.parser.YapeIncomeNotificationParser
import pe.kipu.core.domain.test.NotificationFixtureLoader

class ParseNotificationTextUseCaseTest {

    private val useCase = ParseNotificationTextUseCase(
        notificationParserRouter = NotificationParserRouter(
            yapeIncomeNotificationParser = YapeIncomeNotificationParser(),
            plinIncomeNotificationParser = PlinIncomeNotificationParser(),
        ),
    )

    @Test
    fun `routes yape package to yape parser`() {
        val text = NotificationFixtureLoader.load("notifications/yape_income_standard.txt")
        val result = useCase(MonitoredPaymentApps.YAPE_PACKAGE, text) as NotificationParseResult.Success

        assertEquals(PaymentChannel.YAPE, result.suggestion.channel)
        assertEquals(CategoryIds.OTHER, result.suggestion.categoryId)
        assertEquals("notification_pattern_match", result.suggestion.categorySuggestionReason)
    }

    @Test
    fun `routes plin package to plin parser`() {
        val text = NotificationFixtureLoader.load("notifications/plin_income_standard.txt")
        val result = useCase(MonitoredPaymentApps.PLIN_PACKAGE, text) as NotificationParseResult.Success

        assertEquals(PaymentChannel.PLIN, result.suggestion.channel)
    }

    @Test
    fun `returns unsupported channel for unknown package`() {
        val text = NotificationFixtureLoader.load("notifications/yape_income_standard.txt")
        val result = useCase("com.unknown.app", text)

        assertEquals(NotificationParseResult.UnsupportedChannel, result)
    }
}
