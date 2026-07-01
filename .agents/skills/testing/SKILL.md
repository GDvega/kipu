---
name: testing
description: "Use for Android unit tests, fake repositories, coroutine tests, Flow tests, Compose UI tests, instrumented tests, ADB-driven tests and Firebase Emulator tests."
---

# Purpose

Create high-value tests with low fragility across Android layers.

# When to use

- Adding or reviewing tests.
- Testing ViewModels, Flow or coroutines.
- Testing Compose UI or accessibility.
- Testing Firebase rules/functions with Emulator Suite.

# Inputs to inspect

- Behavior under test.
- Existing test dependencies and source sets.
- Repository fakes or data builders.
- Gradle tasks and CI constraints.

# Required workflow

1. Choose the lowest useful test level first.
2. Create fakes for behavior-focused tests.
3. Use coroutine test APIs and virtual time.
4. Add Compose UI/instrumented tests only for critical flows.
5. Run the relevant Gradle task and report results.

# Rules

- No Thread.sleep.
- Prefer fakes over mocks when state transitions matter.
- Test loading, success, empty, error and permission-denied states.
- Use Firebase Emulator for rules when available.
- Keep UI tests focused.

# Files commonly touched

- `src/test`
- `src/androidTest`
- `ViewModel tests`
- `repository tests`
- `Compose tests`
- `rules tests`

# Commands to validate

```bash
./gradlew test
./gradlew connectedDebugAndroidTest
./gradlew lint
firebase emulators:exec "npm test"
adb logcat
```

# Common mistakes to avoid

- Only testing happy path.
- Using UI tests for pure logic.
- Over-mocking repositories.
- Ignoring source set setup.

# Checklist

- Test level chosen.
- Fakes/data builders exist.
- Coroutine/Flow deterministic.
- Firebase rules tested when relevant.
- Command executed or explained.

# Example prompts

- Use $super-android-kotlin-firebase to generate ViewModel and Compose UI tests.
- Use $super-android-kotlin-firebase to audit missing tests in this feature.

# Expected response style

Respond with: brief diagnosis, change plan, affected files, code or diff summary, validation commands, tests added or recommended, risks, and next step. For review tasks, lead with findings ordered by severity.

# References

- ../../docs/audits/FUENTES_LOCALES.md
- references/android-testing-strategy.md
- templates/unit-test-template.md
- templates/compose-ui-test-template.md
