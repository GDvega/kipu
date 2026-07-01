package pe.kipu.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import pe.kipu.core.domain.category.CategoryIds
import pe.kipu.core.domain.repository.CategoryRepository
import pe.kipu.core.domain.usecase.ObserveHomeInsightsUseCase
import pe.kipu.core.domain.usecase.UpdateDailyAvailableWidgetUseCase

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val observeHomeInsights: ObserveHomeInsightsUseCase,
    private val categoryRepository: CategoryRepository,
    private val updateDailyAvailableWidget: UpdateDailyAvailableWidgetUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val reloadRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init {
        viewModelScope.launch {
            reloadRequests
                .onStart { emit(Unit) }
                .flatMapLatest { observeHomeState() }
                .collect { state ->
                    _uiState.value = state
                    if (state is HomeUiState.Content) {
                        updateDailyAvailableWidget(state.insights)
                    }
                }
        }
    }

    fun retryLoad() {
        reloadRequests.tryEmit(Unit)
    }

    private fun observeHomeState(): Flow<HomeUiState> =
        combine(
            observeHomeInsights(),
            categoryRepository.observeCategories(),
        ) { insights, categories ->
            HomeUiState.Content(
                insights = insights,
                categoryNamesById = categories.associate { category ->
                    category.id to category.name
                },
                userCategories = categories
                    .filter { !CategoryIds.isBuiltIn(it.id) }
                    .sortedBy { it.name },
            )
        }
            .map<HomeUiState.Content, HomeUiState> { it }
            .catch {
                emit(HomeUiState.Error("No pudimos cargar tu resumen"))
            }
}
