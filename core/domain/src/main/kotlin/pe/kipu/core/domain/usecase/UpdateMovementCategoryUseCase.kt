package pe.kipu.core.domain.usecase

import javax.inject.Inject
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.model.EntityId
import pe.kipu.core.domain.model.Movement
import pe.kipu.core.domain.repository.CategoryRepository
import pe.kipu.core.domain.repository.MovementRepository

class UpdateMovementCategoryUseCase @Inject constructor(
    private val movementRepository: MovementRepository,
    private val categoryRepository: CategoryRepository,
) {
    suspend operator fun invoke(
        movementId: EntityId,
        categoryId: EntityId,
    ): Result<Unit> {
        if (categoryRepository.getById(categoryId) == null) {
            return Result.failure(IllegalArgumentException("Category not found"))
        }

        val movement = movementRepository.getById(movementId)
            ?: return Result.failure(IllegalArgumentException("Movement not found"))

        val updated = movement.copy(categoryId = categoryId)
        return when (val validation = updated.validate()) {
            is DomainResult.Err -> Result.failure(IllegalArgumentException("Invalid movement"))
            is DomainResult.Ok -> movementRepository.save(updated)
        }
    }
}
