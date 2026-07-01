---
name: ui-state-design
description: "Use to design Android and Compose UI states for loading, empty, content, error, offline, permission and sensitive-data flows while preserving ViewModels, data layers and tests."
---

# Purpose

Make every important UI state intentional, testable and accessible.

# When to use

- Designing or reviewing loading, empty, content, error, offline or permission states.
- Creating a screen state model before implementing Compose UI.
- Improving AI-generated UI that only handles the happy path.
- Connecting UI copy, affordances and tests to repository, network, Room, DataStore or Firebase behavior.

# Inputs to inspect

- Gate result from `design-test-gate`.
- ViewModel state model and events.
- Repository/network/local/Firebase result types.
- Existing previews and tests.
- Error, permission, offline, auth and sync behavior.
- Accessibility labels and recovery actions.

# Required workflow

1. Confirm build/tests passed before redesigning states.
2. Map the real state sources: local data, network, Firebase, permissions and user actions.
3. Define visible states and transitions without changing data ownership.
4. Design copy, actions and accessibility semantics for each state.
5. Add or recommend previews and tests for critical states.
6. Route implementation details to existing Compose, testing and data skills.

# Rules

- Do not invent states that the ViewModel or data layer cannot produce.
- Do not hide errors behind generic messages when recovery matters.
- Preserve ViewModel, repository, Firebase and navigation contracts.
- Give offline and permission-denied states clear next actions.
- Include sensitive-data handling in state design when the domain requires it.
- Prefer explicit UI models over scattered nullable flags.

# Related existing skills

## Local skills to invoke

- jetpack-compose
- testing
- accessibility
- room-datastore
- networking-retrofit-ktor
- firebase-core

## External companion skills to use when installed

Do not assume these companion skills are installed. Prefer the local skills above first, then consult [Companion Skills](../../docs/COMPANION_SKILLS.md) for install and verification commands.

- aldefy/compose-skill — use for deeper Jetpack Compose, Material 3, design-to-code, theming or Compose architecture guidance.
- skydoves/android-testing-skills — use for deeper Android or Compose UI testing, semantics, assertions or test workflow guidance.
# Files commonly touched

- `*UiState.kt`
- `*ViewModel.kt`
- `*Screen.kt`
- `*Route.kt`
- Repository result models
- Compose previews
- Unit and UI tests

# Commands to validate

```bash
./gradlew assembleDebug
./gradlew test
./gradlew connectedDebugAndroidTest
./gradlew lint
```

# Common mistakes to avoid

- Designing only content state.
- Using one generic error for auth, network, validation and permission failures.
- Changing repository behavior to fit a visual idea.
- Forgetting loading, empty, offline or stale-data states.
- Adding state that has no preview or test.

# Checklist

- Build/tests gate passed.
- State sources identified.
- State matrix defined.
- Recovery actions clear.
- Accessibility semantics considered.
- Previews and tests planned.
- Existing data contracts preserved.

# Example prompts

- Use $super-android-kotlin-firebase to design UI states for this Firebase-backed screen.
- Use $super-android-kotlin-firebase to add empty, offline and permission-denied design guidance before Compose implementation.
- Use $super-android-kotlin-firebase to review this ViewModel state model for UX gaps.

# Expected response style

Respond with: gate status, state-source map, state matrix, UI copy/actions, related skills, validation commands and risks. Do not recommend data-layer changes unless a missing state contract blocks correct UI.

# References

- ../../docs/audits/FUENTES_LOCALES.md
- references/ui-state-matrix-notes.md
- templates/ui-state-matrix-template.md
