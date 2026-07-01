---
name: android-core
description: "Use for Android project structure, AndroidManifest, lifecycle, permissions, resources, compatibility, edge-to-edge, Android Studio and basic device debugging."
---

# Purpose

Guide base Android decisions before touching Compose, Firebase, architecture or release work.

# When to use

- Creating or auditing an Android project baseline.
- Changing AndroidManifest.xml, permissions, resources, minSdk/targetSdk or edge-to-edge behavior.
- Debugging Android Studio, emulator, adb, Logcat or lifecycle issues.

# Inputs to inspect

- settings.gradle(.kts), root build files and app/build.gradle(.kts).
- AndroidManifest.xml, permissions, resources and themes.
- minSdk, targetSdk, compileSdk and device/API constraints.
- Crash, Logcat, adb or Android Studio messages.

# Required workflow

1. Map the project structure and modules before editing.
2. Inspect Manifest, permissions, themes and lifecycle entry points.
3. Choose the smallest compatible Android API or Jetpack API change.
4. Apply the change and keep behavior explicit for older SDKs.
5. Run build/lint or the narrowest available validation command.

# Rules

- Explain every sensitive permission.
- Respect lifecycle and process death.
- Do not block the main thread.
- Do not add platform APIs that conflict with minSdk without guards.
- Prefer official Android skills for AGP, R8, Play, edge-to-edge, Navigation and security tasks.

# Files commonly touched

- `settings.gradle.kts`
- `build.gradle.kts`
- `app/build.gradle.kts`
- `src/main/AndroidManifest.xml`
- `src/main/res/values/*.xml`
- `MainActivity.kt`

# Commands to validate

```bash
./gradlew assembleDebug
./gradlew lint
adb logcat
adb shell dumpsys package <package>
```

# Common mistakes to avoid

- Adding broad permissions without runtime flow.
- Treating target SDK behavior changes as warnings only.
- Putting platform work in Composables or Activities instead of appropriate owners.
- Ignoring edge-to-edge insets on modern Android.

# Checklist

- Manifest reviewed.
- Permissions minimized and justified.
- SDK compatibility checked.
- Lifecycle and configuration changes considered.
- Build/lint command provided.

# Example prompts

- Use $super-android-kotlin-firebase to review my Manifest, permissions and edge-to-edge setup.
- Use $super-android-kotlin-firebase to fix this Android Studio/adb device issue.

# Expected response style

Respond with: brief diagnosis, change plan, affected files, code or diff summary, validation commands, tests added or recommended, risks, and next step. For review tasks, lead with findings ordered by severity.

# References

- ../../docs/audits/FUENTES_LOCALES.md
- references/android-project-baseline.md
- templates/android-manifest-permission-note.md
