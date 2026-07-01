# Unit Test Template

```kotlin
@Test
fun emitsContentWhenRepositoryReturnsItems() = runTest {
    val repo = FakeFeatureRepository(items = listOf(Item("1")))
    val viewModel = FeatureViewModel(repo)
    assertThat(viewModel.uiState.first { !it.isLoading }.items).hasSize(1)
}
```
