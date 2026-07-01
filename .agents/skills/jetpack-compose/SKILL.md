---
name: jetpack-compose
description: "Use for Jetpack Compose screens, Route + Screen structure, state hoisting, remember, rememberSaveable, ViewModel + StateFlow, Material 3, Navigation, previews, adaptive UI and basic accessibility."
---

# Purpose

Build Compose UI that is maintainable, state-driven, accessible and aligned with Material 3.

# When to use

- Creating or refactoring a Compose screen.
- Connecting ViewModel and StateFlow to UI.
- Adding Navigation, previews, adaptive layouts or accessibility semantics.

# Inputs to inspect

- Composable files, UI state and ViewModel.
- Theme/design system and Material 3 usage.
- Navigation graph and route parameters.
- Preview and UI test setup.

# Required workflow

1. Define UI state and user events.
2. Create a Route composable for ViewModel and navigation wiring.
3. Create a stateless Screen composable that renders state.
4. Add previews for loading, empty, content and error states.
5. Add semantics and tests for the critical flow.

# Rules

- Use Route + Screen for stateful screens.
- Hoist state and keep reusable components stateless.
- Do not perform network, database or Firebase calls in Composables.
- Use Material 3 theme tokens.
- Add stable keys for lazy lists.

# Files commonly touched

- `feature/*/*Screen.kt`
- `feature/*/*Route.kt`
- `feature/*/*ViewModel.kt`
- `navigation files`
- `ui/theme files`
- `androidTest Compose tests`

# Commands to validate

```bash
./gradlew assembleDebug
./gradlew test
./gradlew connectedDebugAndroidTest
```

# Common mistakes to avoid

- Passing ViewModel into reusable leaf components.
- Using remember for business state that belongs in ViewModel.
- Missing previews for error/empty states.
- Ignoring semantics, content descriptions and font scale.

# Checklist

- Route and Screen separated.
- State hoisted.
- Material 3 used.
- Previews added.
- Accessibility and UI tests considered.

# Example prompts

- Use $super-android-kotlin-firebase to create a weekly budget Compose screen.
- Use $super-android-kotlin-firebase to refactor this Compose screen into Route + Screen.

# Expected response style

Respond with: brief diagnosis, change plan, affected files, code or diff summary, validation commands, tests added or recommended, risks, and next step. For review tasks, lead with findings ordered by severity.

# References

- ../../docs/audits/FUENTES_LOCALES.md
- references/compose-state-patterns.md
- references/navigation-patterns.md
- templates/route-screen-viewmodel-template.md
