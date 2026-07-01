# Compose UI Test Template

```kotlin
@get:Rule val composeRule = createComposeRule()

@Test fun showsContent() {
    composeRule.setContent { FeatureScreen(state = contentState, onAction = {}) }
    composeRule.onNodeWithText("Title").assertIsDisplayed()
}
```
