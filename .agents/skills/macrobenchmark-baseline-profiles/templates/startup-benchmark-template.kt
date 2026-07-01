```kotlin
@Test
fun startup() = benchmarkRule.measureRepeated(
    packageName = "com.example",
    metrics = listOf(StartupTimingMetric()),
    iterations = 5
) { startActivityAndWait() }
```
