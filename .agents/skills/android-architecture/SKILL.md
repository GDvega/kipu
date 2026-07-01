---
name: android-architecture
description: "Use for Clean Architecture, MVVM, MVI, multimodule design, domain/data/presentation boundaries, repositories, use cases, DTO/entity/domain mapping and offline-first architecture."
---

# Purpose

Design or review pragmatic Android architecture for real apps without over-engineering.

# When to use

- Creating a new app structure.
- Refactoring UI, domain and data boundaries.
- Introducing multimodule structure or offline-first behavior.
- Reviewing repositories, use cases and mappers.

# Inputs to inspect

- Module tree and dependency graph.
- Current package structure.
- Data sources and sync requirements.
- Tests and build times.

# Required workflow

1. Map current architecture and dependencies.
2. Identify concrete coupling or testability problems.
3. Propose the smallest boundary that solves the problem.
4. Refactor around contracts and tests.
5. Validate build and affected tests.

# Rules

- UI must not know Firebase, Room or Retrofit details directly.
- Domain should not depend on Android SDK.
- Repositories hide local/remote data sources.
- Add modules only when they reduce real coupling.
- Prefer offline-first when product requirements need resilience.

# Files commonly touched

- `settings.gradle.kts`
- `feature modules`
- `core/data`
- `core/domain`
- `core/database`
- `core/network`
- `repository and mapper files`

# Commands to validate

```bash
./gradlew projects
./gradlew test
./gradlew assembleDebug
./gradlew lint
```

# Common mistakes to avoid

- Adding empty layers for dogma.
- Moving everything in one giant refactor.
- Creating trivial use cases for every repository call.
- Breaking dependency direction.

# Checklist

- Boundaries named.
- Dependencies directional.
- Repositories testable.
- Offline strategy explicit.
- Incremental plan exists.

# Example prompts

- Use $super-android-kotlin-firebase to review my Android architecture.
- Use $super-android-kotlin-firebase to split this feature into UI/domain/data.

# Expected response style

Respond with: brief diagnosis, change plan, affected files, code or diff summary, validation commands, tests added or recommended, risks, and next step. For review tasks, lead with findings ordered by severity.

# References

- ../../docs/audits/FUENTES_LOCALES.md
- references/clean-architecture-pragmatic.md
- templates/feature-module-template.md
