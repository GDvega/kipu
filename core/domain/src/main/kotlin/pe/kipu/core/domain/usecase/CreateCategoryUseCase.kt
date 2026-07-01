package pe.kipu.core.domain.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.first
import pe.kipu.core.domain.model.Category
import pe.kipu.core.domain.model.DomainResult
import pe.kipu.core.domain.repository.CategoryRepository
import pe.kipu.core.domain.time.TimeProvider

class CreateCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val timeProvider: TimeProvider,
) {
    suspend operator fun invoke(name: String, iconKey: String? = null): Result<Category> {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Category name is required"))
        }

        val existing = categoryRepository.observeCategories().first()
            .firstOrNull { it.name.equals(trimmedName, ignoreCase = true) }
        if (existing != null) {
            return Result.success(existing)
        }

        val category = Category(
            id = "category-${timeProvider.now().toEpochMilli()}",
            name = trimmedName,
            iconKey = iconKey ?: DEFAULT_ICON_KEY,
        )

        when (val validation = category.validate()) {
            is DomainResult.Err -> return Result.failure(IllegalArgumentException(validation.error.message))
            is DomainResult.Ok -> Unit
        }

        return categoryRepository.save(category).map { category }
    }

    private companion object {
        const val DEFAULT_ICON_KEY: String = "other"
    }
}
