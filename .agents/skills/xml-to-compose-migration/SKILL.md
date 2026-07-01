---
name: xml-to-compose-migration
description: "Use for incremental migration from XML layouts, ViewBinding, Fragments and legacy themes to Jetpack Compose screens and previews."
---

# Purpose

Move legacy XML UI to Compose incrementally while preserving resources, navigation, ViewModel logic and test coverage.

# When to use

- Converting XML layout screens to Compose.
- Migrating ViewBinding or Fragment UI to Compose.
- Keeping XML and Compose interoperability during a transition.
- Migrating themes, colors and strings into Compose-friendly usage.

# Inputs to inspect

- `res/layout/*.xml`
- `Fragment.kt` and `Activity.kt`
- ViewBinding usage
- `themes.xml`, `colors.xml`, `strings.xml`
- Navigation graph and screen tests

# Required workflow

1. Identify the smallest screen or component to migrate.
2. Keep business logic in ViewModel or domain layers.
3. Use ComposeView or route/screen split for incremental adoption.
4. Preserve strings and accessibility labels.
5. Add previews and critical UI tests.

# Rules

- Do not delete XML that is still referenced.
- Do not move ViewModel logic into Composables.
- Keep string resources in resources unless project conventions differ.
- Preserve navigation contracts.
- Prefer incremental migration over large rewrites.

# Files commonly touched

- `res/layout/*.xml`
- `Fragment.kt`
- `Activity.kt`
- `feature/*/*Screen.kt`
- `ui/theme/*`
- `navigation/*`
- `androidTest/*`

# Commands to validate

```bash
./gradlew assembleDebug
./gradlew test
./gradlew connectedDebugAndroidTest
```

# Common mistakes to avoid

- Rewriting the whole feature at once.
- Dropping resource qualifiers or accessibility labels.
- Breaking Fragment lifecycle assumptions.
- Skipping previews for empty/error states.

# Checklist

- XML references checked.
- Compose screen is stateless where practical.
- Resources preserved.
- Preview added.
- Tests added or recommended.

# Example prompts

- Use $super-android-kotlin-firebase to migrate this XML screen to Compose.
- Use $super-android-kotlin-firebase to replace ViewBinding with Compose incrementally.

# Expected response style

Respond with: brief diagnosis, change plan, affected files, code or diff summary, validation commands, tests added or recommended, risks, and next step. For review tasks, lead with findings ordered by severity.

# References

- ../../docs/audits/FUENTES_LOCALES.md
- references/incremental-migration-strategy.md
- references/xml-compose-interoperability.md
- references/theme-migration.md
- templates/fragment-to-compose-template.md
- templates/xml-screen-to-compose-screen-template.md
- templates/compose-preview-template.md
