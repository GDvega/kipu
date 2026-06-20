package pe.kipu.core.domain.usecase

import javax.inject.Inject
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.NotificationParseResult
import pe.kipu.core.domain.model.SuggestedMovement
import pe.kipu.core.domain.notification.MonitoredPaymentApps
import pe.kipu.core.domain.parser.NotificationParserRouter

class ParseNotificationTextUseCase @Inject constructor(
    private val notificationParserRouter: NotificationParserRouter,
) {

    operator fun invoke(packageName: String, rawText: String): NotificationParseResult {
        val channel = MonitoredPaymentApps.channelForPackage(packageName)
            ?: return NotificationParseResult.UnsupportedChannel

        val parseResult = notificationParserRouter.parse(channel, rawText)
        if (parseResult !is NotificationParseResult.Success) return parseResult

        val enriched = enrichWithCategorySuggestion(parseResult.suggestion)
        return NotificationParseResult.Success(enriched)
    }

    private fun enrichWithCategorySuggestion(suggestion: SuggestedMovement): SuggestedMovement {
        if (suggestion.categoryId != null) return suggestion
        return suggestion.copy(
            categoryId = CategoryIds.OTHER,
            categorySuggestionReason = "notification_pattern_match",
        )
    }
}
