package pe.kipu.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import pe.kipu.core.domain.repository.CategoryRepository
import pe.kipu.core.domain.usecase.ObserveHomeInsightsUseCase
import pe.kipu.core.domain.usecase.UpdateDailyAvailableWidgetUseCase

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeHomeInsights: ObserveHomeInsightsUseCase,
    categoryRepository: CategoryRepository,
    private val updateDailyAvailableWidget: UpdateDailyAvailableWidgetUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                observeHomeInsights(),
                categoryRepository.observeCategories(),
            ) { insights, categories ->
                HomeUiState.Content(
                    insights = insights,
                    categoryNamesById = categories.associate { category ->
                        category.id to category.name
                    },
                )
            }
                .catch {
                    _uiState.value = HomeUiState.Error("No pudimos cargar tu resumen")
                }
                .collect { state ->
                    _uiState.value = state
                    updateDailyAvailableWidget(state.insights)
                }
        }
    }
}
