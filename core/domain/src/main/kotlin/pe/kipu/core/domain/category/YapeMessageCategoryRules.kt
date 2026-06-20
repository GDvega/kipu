package pe.kipu.core.domain.category

/**
 * Maps Yape receipt message keywords to default category ids.
 */
object YapeMessageCategoryRules {

    const val REASON_KEY: String = "receipt_keyword_match"

    private data class KeywordRule(
        val keywords: List<String>,
        val categoryId: String,
    )

    private val rules: List<KeywordRule> = listOf(
        KeywordRule(
            keywords = listOf("almuerzo", "comida", "lonche", "restaurant", "restaurante", "cena"),
            categoryId = CategoryIds.FOOD,
        ),
        KeywordRule(
            keywords = listOf("pasaje", "transporte", "taxi", "uber", "micro", "metropolitano"),
            categoryId = CategoryIds.TRANSPORT,
        ),
        KeywordRule(
            keywords = listOf("agua", "luz", "internet", "servicio", "sedapal", "enel"),
            categoryId = CategoryIds.SERVICES,
        ),
    )

    fun suggestCategoryId(message: String?): String? {
        if (message.isNullOrBlank()) return null
        val normalized = message.lowercase()
        return rules.firstOrNull { rule ->
            rule.keywords.any { keyword -> normalized.contains(keyword) }
        }?.categoryId
    }
}
