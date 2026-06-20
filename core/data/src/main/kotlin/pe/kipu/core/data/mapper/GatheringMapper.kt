package pe.kipu.core.data.mapper

import pe.kipu.core.data.local.entity.GatheringEntity
import pe.kipu.core.domain.model.Gathering

private const val PARTICIPANT_SEPARATOR = "|"

fun GatheringEntity.toDomain(): Gathering = Gathering(
    id = id,
    name = name,
    participantCount = participantCount,
    participantNames = decodeParticipantNames(participantNames),
)

fun Gathering.toEntity(): GatheringEntity = GatheringEntity(
    id = id,
    name = name,
    participantCount = participantCount,
    participantNames = encodeParticipantNames(participantNames),
)

internal fun encodeParticipantNames(names: List<String>): String =
    names.joinToString(PARTICIPANT_SEPARATOR)

internal fun decodeParticipantNames(raw: String): List<String> =
    if (raw.isBlank()) {
        emptyList()
    } else {
        raw.split(PARTICIPANT_SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }
    }
