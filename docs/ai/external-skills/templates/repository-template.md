# Repository Template

```kotlin
interface FeatureRepository {
    fun observeItems(): Flow<List<Item>>
    suspend fun refresh(): Result<Unit>
}
```
