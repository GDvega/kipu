package pe.kipu.feature.home.presentation

import pe.kipu.core.domain.model.Category
import pe.kipu.core.domain.model.HomeInsights

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Content(
        val insights: HomeInsights,
        val categoryNamesById: Map<String, String> = emptyMap(),
        val userCategories: List<Category> = emptyList(),
    ) : HomeUiState

    data class Error(val message: String) : HomeUiState
}
