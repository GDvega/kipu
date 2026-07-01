---
name: gradle-build
description: "Use for Gradle Kotlin DSL, Android Gradle Plugin, Kotlin plugin, version catalogs, Firebase BoM, convention plugins, build variants and modularized Android builds."
---

# Purpose

Keep the Android build reproducible, compatible and easy to evolve.

# When to use

- Fixing Gradle sync/build errors.
- Adding dependencies or plugins.
- Migrating to version catalogs or convention plugins.
- Changing build variants, flavors, AGP, Kotlin or Firebase BoM.

# Inputs to inspect

- settings.gradle(.kts), build.gradle(.kts), gradle/libs.versions.toml.
- Gradle, AGP, Kotlin and Java versions.
- Full build error and command.
- CI workflow if relevant.

# Required workflow

1. Read current version strategy before changing versions.
2. Check compatibility between Gradle, AGP, Kotlin and Firebase BoM.
3. Change the narrowest build file.
4. Run sync/build validation.
5. Document impact and rollback path.

# Rules

- Do not upgrade versions blindly.
- Do not duplicate Firebase dependency versions outside the BoM.
- Keep Kotlin DSL consistent.
- Keep signing secrets out of Gradle files.
- Use convention plugins only when the project already benefits from them.

# Files commonly touched

- `settings.gradle.kts`
- `build.gradle.kts`
- `gradle/libs.versions.toml`
- `build-logic/*`
- `.github/workflows/*`

# Commands to validate

```bash
./gradlew help
./gradlew assembleDebug
./gradlew test
./gradlew lint
./gradlew --version
```

# Common mistakes to avoid

- Changing AGP and Kotlin together without reason.
- Mixing Groovy and Kotlin DSL patterns.
- Adding duplicate plugin declarations.
- Committing local SDK paths or secrets.

# Checklist

- Version compatibility checked.
- BoM used correctly.
- Variants preserved.
- Build command passes or failure is documented.
- CI considered.

# Example prompts

- Use $super-android-kotlin-firebase to fix this Gradle sync error.
- Use $super-android-kotlin-firebase to add Firebase BoM using version catalogs.

# Expected response style

Respond with: brief diagnosis, change plan, affected files, code or diff summary, validation commands, tests added or recommended, risks, and next step. For review tasks, lead with findings ordered by severity.

# References

- ../../docs/audits/FUENTES_LOCALES.md
- references/version-catalogs-and-agp.md
- templates/version-catalog-template.md
