---
name: agp9-kotlin-migration
description: "Use for safe Android Gradle Plugin 9, Kotlin, Gradle wrapper, version catalog, Compose and Firebase BoM migrations."
---

# Purpose

Upgrade Android build tooling in small verified steps while preserving module structure, CI and dependency compatibility.

# When to use

- Migrating to AGP 9 or newer Kotlin tooling.
- Fixing Gradle sync errors after an upgrade.
- Modernizing Gradle Kotlin DSL, wrapper or version catalogs.

# Inputs to inspect

- `settings.gradle.kts`
- Root and module `build.gradle.kts` files
- `gradle/libs.versions.toml`
- `gradle/wrapper/gradle-wrapper.properties`
- CI workflow files
- Compose and Firebase BoM usage

# Required workflow

1. Record current Gradle, AGP, Kotlin, Compose and Firebase versions.
2. Check compatibility before editing.
3. Upgrade one tooling family at a time.
4. Run sync/build validation after each step.
5. Document rollback commands and residual risks.

# Rules

- Do not upgrade every dependency blindly.
- Do not mix alpha/beta versions unless requested.
- Do not change module structure without a reason.
- Keep version catalog aliases stable when possible.
- Preserve build variants and signing boundaries.

# Files commonly touched

- `settings.gradle.kts`
- `build.gradle.kts`
- `app/build.gradle.kts`
- `gradle/libs.versions.toml`
- `gradle-wrapper.properties`
- `.github/workflows/*`

# Commands to validate

```bash
./gradlew --version
./gradlew projects
./gradlew help
./gradlew assembleDebug
./gradlew test
```

# Common mistakes to avoid

- Skipping wrapper compatibility.
- Updating Kotlin and Compose compiler assumptions together without checks.
- Removing plugins still used by convention modules.
- Ignoring CI differences from local builds.

# Checklist

- Current versions captured.
- Compatibility noted.
- Small-step plan used.
- CI impact checked.
- Build and tests run or explained.

# Example prompts

- Use $super-android-kotlin-firebase to migrate this project to AGP 9 safely.
- Use $super-android-kotlin-firebase to fix this Gradle sync failure after a Kotlin upgrade.

# Expected response style

Respond with: brief diagnosis, change plan, affected files, code or diff summary, validation commands, tests added or recommended, risks, and next step. For review tasks, lead with findings ordered by severity.

# References

- ../../docs/audits/FUENTES_LOCALES.md
- references/agp9-upgrade-checklist.md
- references/kotlin-plugin-compatibility.md
- references/gradle-version-catalogs.md
- references/known-migration-issues.md
- templates/version-catalog-before-after.md
- templates/android-application-build-gradle-template.md
- templates/android-library-build-gradle-template.md
