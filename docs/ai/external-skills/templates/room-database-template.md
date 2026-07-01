# Room Database Template

```kotlin
@Entity(tableName = "items")
data class ItemEntity(@PrimaryKey val id: String, val title: String)

@Dao
interface ItemDao {
    @Query("SELECT * FROM items")
    fun observeItems(): Flow<List<ItemEntity>>
}
```
