package pe.kipu.core.domain.usecase

import javax.inject.Inject
import pe.kipu.core.domain.repository.MovementRepository

class SuggestCategoryFromPlinHistoryUseCase @Inject constructor(
    private val movementRepository: MovementRepository,
) {

    companion object {
        const val REASON_KEY: String = "plin_history_match"
    }

    suspend operator fun invoke(counterpartyName: String?): CategorySuggestion? {
        if (counterpartyName.isNullOrBlank()) return null

        val matching = movementRepository.findByCounterpartyName(counterpartyName)
        if (matching.isEmpty()) return null

        val categoryId = matching
            .groupingBy { it.categoryId }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
            ?: return null

        return CategorySuggestion(
            categoryId = categoryId,
            reason = REASON_KEY,
        )
    }
}
