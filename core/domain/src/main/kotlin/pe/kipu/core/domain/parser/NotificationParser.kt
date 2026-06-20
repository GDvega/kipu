package pe.kipu.core.domain.parser

import pe.kipu.core.domain.model.NotificationParseResult

fun interface NotificationParser {
    fun parse(rawText: String): NotificationParseResult
}
