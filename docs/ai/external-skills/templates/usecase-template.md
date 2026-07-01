# Use Case Template

```kotlin
class ObserveItemsUseCase(private val repository: FeatureRepository) {
    operator fun invoke(): Flow<List<Item>> = repository.observeItems()
}
```
