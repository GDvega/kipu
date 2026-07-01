# ViewModel Template

```kotlin
class FeatureViewModel(private val repository: FeatureRepository) : ViewModel() {
    val uiState: StateFlow<FeatureUiState> = repository.observeItems()
        .map { FeatureUiState(items = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FeatureUiState(isLoading = true))
}
```
