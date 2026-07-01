# Compose Screen Template

```kotlin
@Composable
fun FeatureRoute(viewModel: FeatureViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    FeatureScreen(state = state, onAction = viewModel::onAction)
}

@Composable
fun FeatureScreen(state: FeatureUiState, onAction: (FeatureAction) -> Unit) {
    // Render loading, empty, content and error states.
}
```
