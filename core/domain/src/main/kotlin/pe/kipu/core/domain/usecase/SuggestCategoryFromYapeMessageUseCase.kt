package pe.kipu.core.domain.usecase

import javax.inject.Inject
import pe.kipu.core.domain.category.YapeMessageCategoryRules

data class CategorySuggestion(
    val categoryId: String,
    val reason: String,
)

class SuggestCategoryFromYapeMessageUseCase @Inject constructor() {

    operator fun invoke(message: String?): CategorySuggestion? {
        val categoryId = YapeMessageCategoryRules.suggestCategoryId(message) ?: return null
        return CategorySuggestion(
            categoryId = categoryId,
            reason = YapeMessageCategoryRules.REASON_KEY,
        )
    }
}
