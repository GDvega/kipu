package pe.kipu.core.domain.export

import javax.inject.Inject

class UserDataCsvSerializer @Inject constructor() {

    fun serializeMovements(
        snapshot: UserDataSnapshot,
        delimiter: Char = ',',
    ): String = buildString {
        appendLine(header(delimiter))
        snapshot.movements.forEach { movement ->
            append(csvField(movement.id, delimiter))
            append(delimiter)
            append(csvField(movement.type.name, delimiter))
            append(delimiter)
            append(csvField(movement.amount.amount.toPlainString(), delimiter))
            append(delimiter)
            append(csvField(movement.categoryId, delimiter))
            append(delimiter)
            append(csvField(movement.channel.name, delimiter))
            append(delimiter)
            append(csvField(movement.source.name, delimiter))
            append(delimiter)
            append(csvField(movement.status.name, delimiter))
            append(delimiter)
            append(csvField(movement.description, delimiter))
            append(delimiter)
            append(csvField(movement.counterpartyName, delimiter))
            append(delimiter)
            append(csvField(movement.operationNumber, delimiter))
            append(delimiter)
            append(csvField(movement.recordedAt.toString(), delimiter))
            append(delimiter)
            appendLine(csvField(movement.createdAt.toString(), delimiter))
        }
    }

    private fun header(delimiter: Char): String =
        MOVEMENTS_COLUMNS.joinToString(separator = delimiter.toString())

    private fun csvField(value: String?, delimiter: Char): String = when (value) {
        null -> ""
        else -> buildString {
            var needsQuotes = false
            value.forEach { char ->
                if (char == '"' || char == delimiter || char == '\n' || char == '\r') {
                    needsQuotes = true
                }
            }
            if (needsQuotes) {
                append('"')
                value.forEach { char ->
                    if (char == '"') append("\"\"")
                    else append(char)
                }
                append('"')
            } else {
                append(value)
            }
        }
    }

    private companion object {
        val MOVEMENTS_COLUMNS = listOf(
            "id",
            "type",
            "amount",
            "categoryId",
            "channel",
            "source",
            "status",
            "description",
            "counterpartyName",
            "operationNumber",
            "recordedAt",
            "createdAt",
        )
    }
}
