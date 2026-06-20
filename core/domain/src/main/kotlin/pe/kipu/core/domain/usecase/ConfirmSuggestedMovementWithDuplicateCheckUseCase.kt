package pe.kipu.core.domain.usecase

import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import pe.kipu.core.domain.model.ConfirmMovementResult
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.DuplicateDetectionResult
import pe.kipu.core.domain.model.DuplicateResolution
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.SuggestedMovement
import pe.kipu.core.domain.repository.MovementRepository

/**
 * Confirms a suggested movement after duplicate detection.
 * Never persists when a duplicate is pending without an explicit [resolution].
 */
class ConfirmSuggestedMovementWithDuplicateCheckUseCase @Inject constructor(
    private val movementRepository: MovementRepository,
    private val detectDuplicateMovement: DetectDuplicateMovementUseCase,
) {

    suspend operator fun invoke(
        suggestion: SuggestedMovement,
        movementId: EntityId,
        categoryId: EntityId,
        createdAt: Instant,
        recordedAt: Instant? = null,
        resolution: DuplicateResolution? = null,
    ): ConfirmMovementResult {
        when (val suggestionValidation = suggestion.validate()) {
            is DomainResult.Err ->
                throw IllegalArgumentException(suggestionValidation.error.message)
            is DomainResult.Ok -> Unit
        }

        val resolvedRecordedAt = recordedAt ?: suggestion.suggestedRecordedAt
            ?: throw IllegalArgumentException("Recorded at is required to confirm")

        val candidate = when (
            val buildResult = buildConfirmedMovementFromSuggestion(
                suggestion = suggestion,
                movementId = movementId,
                categoryId = categoryId,
                createdAt = createdAt,
                recordedAt = resolvedRecordedAt,
            )
        ) {
            is DomainResult.Err ->
                throw IllegalArgumentException(buildResult.error.message)
            is DomainResult.Ok -> buildResult.value
        }

        val existingMovements = movementRepository.observeMovements().first()
        val detection = detectDuplicateMovement(candidate, existingMovements)

        return when (detection) {
            DuplicateDetectionResult.NoMatch -> {
                movementRepository.save(candidate).getOrThrow()
                ConfirmMovementResult.Saved(candidate)
            }

            is DuplicateDetectionResult.Matches -> when (resolution) {
                null -> ConfirmMovementResult.DuplicatePending(
                    candidate = candidate,
                    matches = detection.existing,
                )

                DuplicateResolution.SAVE_AS_NEW -> {
                    movementRepository.save(candidate).getOrThrow()
                    ConfirmMovementResult.Saved(candidate)
                }

                DuplicateResolution.MERGE,
                DuplicateResolution.CANCEL,
                -> ConfirmMovementResult.Cancelled
            }
        }
    }
}
