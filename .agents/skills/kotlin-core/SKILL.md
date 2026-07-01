---
name: kotlin-core
description: "Use for Kotlin null safety, data classes, sealed classes/interfaces, coroutines, Flow, Result, extension functions and error handling."
---

# Purpose

Keep Kotlin code idiomatic, cancellable, explicit and testable.

# When to use

- Refactoring Kotlin domain, data or ViewModel code.
- Modeling UI state, domain results or errors.
- Fixing coroutine, Flow, cancellation or nullability issues.

# Inputs to inspect

- Kotlin version and compiler/plugin setup.
- Code under change and tests.
- Coroutine scopes, dispatchers and Flow operators.
- Expected error and state behavior.

# Required workflow

1. Read the current Kotlin style and version.
2. Identify state, error and async boundaries.
3. Choose sealed types, Result or domain errors intentionally.
4. Preserve cancellation and structured concurrency.
5. Add or update unit tests for changed behavior.

# Rules

- Never use GlobalScope.
- Do not swallow CancellationException.
- Avoid !! except at hard verified boundaries.
- Keep extension functions focused and discoverable.
- Expose immutable state to consumers.

# Files commonly touched

- `*.kt`
- `ViewModel files`
- `domain models`
- `repository interfaces`
- `test files`

# Commands to validate

```bash
./gradlew test
./gradlew compileDebugKotlin
./gradlew lint
```

# Common mistakes to avoid

- Launching coroutines without lifecycle or owner scope.
- Mixing UI strings into domain errors.
- Using nullable types where a sealed state is clearer.
- Creating magical extensions that hide side effects.

# Checklist

- Nullability explicit.
- Cancellation preserved.
- Flow/state testable.
- Errors modeled.
- Tests added or recommended.

# Example prompts

- Use $super-android-kotlin-firebase to convert this callback API to Flow.
- Use $super-android-kotlin-firebase to model loading/success/error state in Kotlin.

# Expected response style

Respond with: brief diagnosis, change plan, affected files, code or diff summary, validation commands, tests added or recommended, risks, and next step. For review tasks, lead with findings ordered by severity.

# References

- ../../docs/audits/FUENTES_LOCALES.md
- references/kotlin-state-and-errors.md
- templates/result-state-template.md
