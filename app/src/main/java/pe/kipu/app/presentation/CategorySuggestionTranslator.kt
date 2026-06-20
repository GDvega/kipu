package pe.kipu.app.presentation

/**
 * Maps domain category suggestion keys to user-facing Spanish copy.
 */
object CategorySuggestionTranslator {

    fun toDisplayText(reasonKey: String): String = when (reasonKey) {
        "plin_history_match" -> "Sugerido por tu historial en Plin"
        "yape_history_match" -> "Sugerido por tu historial en Yape"
        "receipt_keyword_match" -> "Sugerido por palabras del comprobante"
        "notification_pattern_match" -> "Sugerido por el patrón de la notificación"
        else -> "Sugerencia de categoría"
    }
}
