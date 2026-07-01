```kotlin
@Test
fun generateBaselineProfile() {
    baselineProfileRule.collect(packageName = "com.example") {
        startActivityAndWait()
    }
}
```
