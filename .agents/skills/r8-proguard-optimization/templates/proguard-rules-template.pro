# ProGuard Rules Template

```proguard
# Keep only the specific model or adapter required by the failing release path.
-keep class com.example.feature.model.RequiredModel { *; }
```
