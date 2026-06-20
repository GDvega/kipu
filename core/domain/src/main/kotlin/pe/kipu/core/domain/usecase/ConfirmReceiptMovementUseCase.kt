package pe.kipu.core.domain.usecase

import java.time.Instant
import javax.inject.Inject
import pe.kipu.core.domain.model.ConfirmMovementResult
import pe.kipu.core.domain.model.DuplicateResolution
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.SuggestedMovement
import pe.kipu.core.domain.time.TimeProvider

/**
 * Confirms a receipt-derived [SuggestedMovement] after user review.
 * Uses stable movement ids from the receipt draft id.
 */
class ConfirmReceiptMovementUseCase @Inject constructor(
    private val confirmWithDuplicateCheck: ConfirmSuggestedMovementWithDuplicateCheckUseCase,
    private val timeProvider: TimeProvider,
) {

    suspend operator fun invoke(
        suggestion: SuggestedMovement,
        categoryId: EntityId,
        recordedAt: Instant?,
        resolution: DuplicateResolution? = null,
    ): ConfirmMovementResult = confirmWithDuplicateCheck(
        suggestion = suggestion,
        movementId = movementIdForDraft(suggestion.draftId),
        categoryId = categoryId,
        createdAt = timeProvider.now(),
        recordedAt = recordedAt,
        resolution = resolution,
    )

    private fun movementIdForDraft(draftId: String): String = "movement-$draftId"
}
