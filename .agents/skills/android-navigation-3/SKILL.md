---
name: android-navigation-3
description: "Use for Android Navigation 3 setup, migration from Navigation Compose, typed routes, deep links, back stack handling and navigation tests."
---

# Purpose

Implement or migrate modern Android navigation without breaking screens, route arguments, deep links or back stack behavior.

# When to use

- Adding Navigation 3 to an Android app.
- Migrating from Navigation Compose or custom navigation.
- Fixing typed routes, deep links, nested graphs or back stack issues.

# Inputs to inspect

- `build.gradle.kts` and `libs.versions.toml`
- `MainActivity.kt` and app entry points
- `NavHost`, routes and feature navigation files
- `AndroidManifest.xml` deep links
- `screens/*` and ViewModel event flow

# Required workflow

1. Map current destinations, arguments and deep links.
2. Choose typed routes or preserve the existing route convention.
3. Move navigation decisions to route-level event handling.
4. Add or update navigation tests for critical flows.
5. Validate back behavior, process recreation and deep links.

# Rules

- Do not pass large objects as route arguments.
- Do not put business logic inside route definitions.
- Do not create duplicate NavHost owners.
- Preserve existing deep links unless the user approves a migration.
- Prefer typed route patterns when the project already uses them.

# Files commonly touched

- `settings.gradle.kts`
- `build.gradle.kts`
- `gradle/libs.versions.toml`
- `MainActivity.kt`
- `feature/*/navigation/*`
- `AndroidManifest.xml`
- `androidTest/*Navigation*`

# Commands to validate

```bash
./gradlew assembleDebug
./gradlew test
./gradlew connectedDebugAndroidTest
```

# Common mistakes to avoid

- Navigating directly from deep reusable Composables.
- Changing route strings without migration notes.
- Forgetting saved state and process death.
- Testing only happy-path forward navigation.

# Checklist

- Destinations and arguments documented.
- Deep links preserved or explicitly migrated.
- Back stack behavior tested.
- Navigation events flow from UI state/events.
- Validation commands provided.

# Example prompts

- Use $super-android-kotlin-firebase to migrate this app to Navigation 3.
- Use $super-android-kotlin-firebase to add typed routes and deep links safely.

# Expected response style

Respond with: brief diagnosis, change plan, affected files, code or diff summary, validation commands, tests added or recommended, risks, and next step. For review tasks, lead with findings ordered by severity.

# References

- ../../docs/audits/FUENTES_LOCALES.md
- references/navigation-3-patterns.md
- references/migration-from-navigation-compose.md
- references/deep-links-and-back-stack.md
- templates/typed-route-template.md
- templates/navigation-host-template.md
- templates/navigation-test-template.md
