package pe.kipu.core.data.notification

import pe.kipu.core.domain.parser.ReceiptTextSanitizer

/**
 * Combines notification title and body into sanitized plain text for domain parsing.
 * Never logs the combined content.
 */
object NotificationListenerBridge {

    fun combine(title: String?, text: String?): String {
        val combined = listOfNotNull(
            title?.takeIf { it.isNotBlank() },
            text?.takeIf { it.isNotBlank() },
        ).joinToString("\n")
        return ReceiptTextSanitizer.sanitize(combined)
    }
}
