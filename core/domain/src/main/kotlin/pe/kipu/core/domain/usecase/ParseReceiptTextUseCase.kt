package pe.kipu.core.domain.usecase

import javax.inject.Inject
import pe.kipu.core.domain.model.PaymentChannel
import pe.kipu.core.domain.model.ReceiptParseResult
import pe.kipu.core.domain.model.SuggestedMovement
import pe.kipu.core.domain.parser.ReceiptParserRouter

class ParseReceiptTextUseCase @Inject constructor(
    private val receiptParserRouter: ReceiptParserRouter,
    private val suggestCategoryFromYapeMessage: SuggestCategoryFromYapeMessageUseCase,
    private val suggestCategoryFromPlinHistory: SuggestCategoryFromPlinHistoryUseCase,
) {

    suspend operator fun invoke(rawText: String): ReceiptParseResult {
        val parseResult = receiptParserRouter.parse(rawText)
        if (parseResult !is ReceiptParseResult.Success) return parseResult

        val enriched = enrichWithCategorySuggestion(parseResult.suggestion)
        return ReceiptParseResult.Success(enriched)
    }

    private suspend fun enrichWithCategorySuggestion(suggestion: SuggestedMovement): SuggestedMovement {
        if (suggestion.categoryId != null) return suggestion

        val messageSuggestion = suggestCategoryFromYapeMessage(suggestion.message)
        if (messageSuggestion != null) {
            return suggestion.copy(
                categoryId = messageSuggestion.categoryId,
                categorySuggestionReason = messageSuggestion.reason,
            )
        }

        if (suggestion.channel == PaymentChannel.PLIN && suggestion.message.isNullOrBlank()) {
            val historySuggestion = suggestCategoryFromPlinHistory(suggestion.counterpartyName)
            if (historySuggestion != null) {
                return suggestion.copy(
                    categoryId = historySuggestion.categoryId,
                    categorySuggestionReason = historySuggestion.reason,
                )
            }
        }

        return suggestion
    }
}
