package pe.kipu.core.domain.usecase

import javax.inject.Inject
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.model.MovementSource
import pe.kipu.core.domain.model.MovementStatus
import pe.kipu.core.domain.model.RegisterNotificationIncomeResult
import pe.kipu.core.domain.model.SuggestedMovement
import pe.kipu.core.domain.repository.MovementRepository
import pe.kipu.core.domain.time.TimeProvider

/**
 * Persists a parsed notification income as [MovementStatus.PENDING_CONFIRMATION].
 * Notification suggestions always require explicit human confirmation.
 *
 * Dedup: skips only when a [MovementStatus.PENDING_CONFIRMATION] with the same draft id
 * already exists. After the user confirms a prior income, a new notification with the same
 * draft id gets a unique movement id (suffix from [TimeProvider]).
 */
class RegisterNotificationIncomeUseCase @Inject constructor(
    private val movementRepository: MovementRepository,
    private val timeProvider: TimeProvider,
) {

    suspend operator fun invoke(suggestion: SuggestedMovement): RegisterNotificationIncomeResult {
        when (val validation = suggestion.validate()) {
            is DomainResult.Err -> return RegisterNotificationIncomeResult.ValidationFailed(validation.error)
            is DomainResult.Ok -> Unit
        }

        val baseMovementId = movementIdForDraft(suggestion.draftId)
        val existing = movementRepository.getById(baseMovementId)
        if (existing != null &&
            existing.status == MovementStatus.PENDING_CONFIRMATION &&
            existing.source == MovementSource.NOTIFICATION
        ) {
            return RegisterNotificationIncomeResult.AlreadyPending
        }

        val now = timeProvider.now()
        val movementId = if (existing == null) {
            baseMovementId
        } else {
            "$baseMovementId-${now.toEpochMilli()}"
        }
        val categoryId = suggestion.categoryId ?: CategoryIds.OTHER
        val pendingMovement = when (
            val buildResult = suggestion.toPendingMovement(
                id = movementId,
                categoryId = categoryId,
                recordedAt = now,
                createdAt = now,
                status = MovementStatus.PENDING_CONFIRMATION,
            )
        ) {
            is DomainResult.Err -> return RegisterNotificationIncomeResult.ValidationFailed(buildResult.error)
            is DomainResult.Ok -> buildResult.value
        }

        movementRepository.save(pendingMovement).getOrElse {
            return RegisterNotificationIncomeResult.ValidationFailed(
                pe.kipu.core.domain.model.DomainError.InvalidField("Could not save pending movement"),
            )
        }

        return RegisterNotificationIncomeResult.Saved(pendingMovement)
    }

    private fun movementIdForDraft(draftId: String): String = "movement-$draftId"
}
