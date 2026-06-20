package pe.kipu.core.domain.model

/**
 * User-defined category for classifying movements.
 */
data class Category(
    val id: EntityId,
    val name: String,
    val iconKey: String? = null,
) {
    fun validate(): DomainResult<Unit> = when {
        id.isBlank() -> DomainResult.Err(DomainError.InvalidId("Category id must not be blank"))
        name.isBlank() -> DomainResult.Err(DomainError.InvalidField("Category name must not be blank"))
        else -> DomainResult.Ok(Unit)
    }
}
