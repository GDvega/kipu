package pe.kipu.core.domain.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import pe.kipu.core.domain.model.MovementAuditEntry
import pe.kipu.core.domain.repository.CategoryRepository
import pe.kipu.core.domain.repository.MovementAuditRepository

class ObserveMovementAuditLogsUseCase @Inject constructor(
    private val movementAuditRepository: MovementAuditRepository,
    private val categoryRepository: CategoryRepository,
) {
    operator fun invoke(): Flow<List<MovementAuditEntry>> =
        combine(
            movementAuditRepository.observeAuditLogs(),
            categoryRepository.observeCategories(),
        ) { logs, categories ->
            val categoriesById = categories.associateBy { it.id }
            logs.map { log ->
                if (log.categoryName.isNullOrBlank()) {
                    val resolvedName = categoriesById[log.categoryId]?.name ?: "General"
                    log.copy(categoryName = resolvedName)
                } else {
                    log
                }
            }
        }
}
