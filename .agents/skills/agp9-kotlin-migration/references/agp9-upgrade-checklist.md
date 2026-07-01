# AGP 9 Upgrade Checklist

- Capture current Gradle, AGP, Kotlin and JDK versions.
- Verify wrapper requirements before plugin changes.
- Upgrade root plugins before module-specific cleanup.
- Run `./gradlew help` before full builds to isolate configuration errors.
