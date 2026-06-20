package pe.kipu.core.domain.usecase

import java.time.Instant
import javax.inject.Inject
import pe.kipu.core.domain.model.ConfirmMovementResult
import pe.kipu.core.domain.model.DuplicateConfirmationRequiredException
import pe.kipu.core.domain.model.DuplicateResolution
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.SuggestedMovement

/**
 * Persists a user-confirmed suggestion as a [Movement] with [MovementStatus.CONFIRMED].
 * Delegates to [ConfirmSuggestedMovementWithDuplicateCheckUseCase]; never bypasses duplicate detection.
 *
 * Prefer [ConfirmSuggestedMovementWithDuplicateCheckUseCase] when the UI must handle
 * [ConfirmMovementResult.DuplicatePending].
 */
class ConfirmSuggestedMovementUseCase @Inject constructor(
    private val confirmWithDuplicateCheck: ConfirmSuggestedMovementWithDuplicateCheckUseCase,
) {

    suspend operator fun invoke(
        suggestion: SuggestedMovement,
        movementId: EntityId,
        categoryId: EntityId,
        createdAt: Instant,
        recordedAt: Instant? = null,
        resolution: DuplicateResolution? = null,
    ): Result<Unit> = try {
        when (
            val outcome = confirmWithDuplicateCheck(
                suggestion = suggestion,
                movementId = movementId,
                categoryId = categoryId,
                createdAt = createdAt,
                recordedAt = recordedAt,
                resolution = resolution,
            )
        ) {
            is ConfirmMovementResult.Saved -> Result.success(Unit)
            is ConfirmMovementResult.DuplicatePending ->
                Result.failure(DuplicateConfirmationRequiredException())
            ConfirmMovementResult.Cancelled ->
                Result.failure(IllegalStateException("Movement confirmation cancelled"))
        }
    } catch (error: IllegalArgumentException) {
        Result.failure(error)
    }
}
