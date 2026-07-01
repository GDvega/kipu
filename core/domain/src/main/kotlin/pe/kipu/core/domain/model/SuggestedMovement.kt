package pe.kipu.core.domain.model

import java.time.Instant

/**
 * Draft movement inferred from OCR, notifications or imports.
 * Must be reviewed and confirmed by the user before persisting as [Movement].
 */
data class SuggestedMovement(
    val draftId: EntityId,
    val source: MovementSource,
    val confidence: SuggestionConfidence = SuggestionConfidence.LOW,
    val type: MovementType? = null,
    val amount: Money? = null,
    val categoryId: EntityId? = null,
    val categorySuggestionReason: String? = null,
    val channel: PaymentChannel? = null,
    val counterpartyName: String? = null,
    val message: String? = null,
    val operationReference: String? = null,
    /** Parsed receipt date/time in America/Lima when both date and time are present. */
    val suggestedRecordedAt: Instant? = null,
) {
    fun validate(): DomainResult<Unit> = when {
        draftId.isBlank() -> DomainResult.Err(DomainError.InvalidId("Draft id must not be blank"))
        source == MovementSource.MANUAL ->
            DomainResult.Err(DomainError.InvalidField("Suggested movements cannot originate from manual entry"))
        type == null && amount == null && counterpartyName.isNullOrBlank() ->
            DomainResult.Err(DomainError.InvalidField("Suggestion must include at least one inferred field"))
        categoryId != null && categorySuggestionReason.isNullOrBlank() ->
            DomainResult.Err(DomainError.InvalidField("Category suggestion requires a reason"))
        categoryId == null && categorySuggestionReason != null ->
            DomainResult.Err(DomainError.InvalidField("Category suggestion reason requires a category id"))
        operationReference != null && operationReference.isBlank() ->
            DomainResult.Err(DomainError.InvalidField("Operation reference must not be blank when provided"))
        else -> DomainResult.Ok(Unit)
    }

    fun toPendingMovement(
        id: EntityId,
        categoryId: EntityId,
        recordedAt: Instant,
        createdAt: Instant,
        status: MovementStatus = MovementStatus.PENDING_CONFIRMATION,
    ): DomainResult<Movement> {
        val resolvedType = type
            ?: return DomainResult.Err(DomainError.InvalidField("Movement type is required to confirm"))
        val resolvedAmount = amount
            ?: return DomainResult.Err(DomainError.InvalidField("Amount is required to confirm"))
        val movement = Movement(
            id = id,
            type = resolvedType,
            amount = resolvedAmount,
            categoryId = categoryId,
            channel = channel ?: PaymentChannel.OTHER,
            source = source,
            status = status,
            description = message,
            counterpartyName = counterpartyName,
            operationNumber = operationReference,
            recordedAt = recordedAt,
            createdAt = createdAt,
        )
        return when (val validation = movement.validate()) {
            is DomainResult.Ok -> DomainResult.Ok(movement)
            is DomainResult.Err -> DomainResult.Err(validation.error)
        }
    }
}
