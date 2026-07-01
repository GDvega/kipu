# Gradle Version Catalog Template

```toml
[versions]
agp = "x.y.z"
kotlin = "x.y.z"
firebaseBom = "x.y.z"

[libraries]
firebase-bom = { module = "com.google.firebase:firebase-bom", version.ref = "firebaseBom" }
```
