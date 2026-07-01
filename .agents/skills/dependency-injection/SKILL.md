---
name: dependency-injection
description: "Use for Hilt, Koin, DI modules, scopes, ViewModel injection, repository bindings and testing with dependency injection."
---

# Purpose

Make dependencies explicit and replaceable without adding unnecessary framework weight.

# When to use

- Adding Hilt or Koin.
- Injecting ViewModels, repositories or data sources.
- Fixing scope or binding problems.
- Replacing dependencies in tests.

# Inputs to inspect

- Existing DI framework or manual DI.
- Object lifetimes and scopes.
- ViewModel and repository constructors.
- Test replacement needs.

# Required workflow

1. Detect current DI pattern.
2. Add the smallest binding/module needed.
3. Use scopes intentionally.
4. Wire ViewModel and repository dependencies.
5. Add test fakes or replacement modules.

# Rules

- Do not mix Hilt and Koin without strong reason.
- Avoid hidden service locators.
- Do not inject Context unless needed.
- Keep singletons immutable or thread-safe.
- Do not break Compose previews.

# Files commonly touched

- `di modules`
- `Application class`
- `ViewModels`
- `repositories`
- `test modules`

# Commands to validate

```bash
./gradlew test
./gradlew assembleDebug
./gradlew lint
```

# Common mistakes to avoid

- Injecting everything by default.
- Creating circular dependencies.
- Using singletons for mutable state.
- Forgetting test replacements.

# Checklist

- Bindings minimal.
- Scopes correct.
- Tests can replace dependencies.
- Previews still work.
- Build passes.

# Example prompts

- Use $super-android-kotlin-firebase to add Hilt for repositories and ViewModels.
- Use $super-android-kotlin-firebase to fix this DI binding error.

# Expected response style

Respond with: brief diagnosis, change plan, affected files, code or diff summary, validation commands, tests added or recommended, risks, and next step. For review tasks, lead with findings ordered by severity.

# References

- ../../docs/audits/FUENTES_LOCALES.md
- references/di-scopes-and-tests.md
- templates/hilt-module-template.md
