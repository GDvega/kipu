package pe.kipu.core.domain.model

/**
 * Basic gathering (junta) model for shared expenses in future phases.
 */
data class Gathering(
    val id: EntityId,
    val name: String,
    val participantCount: Int,
    val participantNames: List<String> = emptyList(),
) {
    fun validate(): DomainResult<Unit> = when {
        id.isBlank() -> DomainResult.Err(DomainError.InvalidId("Gathering id must not be blank"))
        name.isBlank() -> DomainResult.Err(DomainError.InvalidField("Gathering name must not be blank"))
        participantCount < 1 ->
            DomainResult.Err(DomainError.InvalidField("Gathering must have at least one participant"))
        participantNames.isNotEmpty() && participantNames.size != participantCount ->
            DomainResult.Err(DomainError.InvalidField("Participant names count must match participant count"))
        else -> DomainResult.Ok(Unit)
    }
}
