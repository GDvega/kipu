---
name: compose-performance
description: "Use for Compose recomposition, stability, LazyColumn/LazyGrid performance, derivedStateOf, remember, keys, baseline profiles, tracing and measurement."
---

# Purpose

Diagnose and fix measurable Compose performance issues without guessing.

# When to use

- A screen scrolls poorly or drops frames.
- Recomposition counts spike unexpectedly.
- Lazy lists, side effects or state reads are suspected performance causes.
- Baseline profiles, tracing or performance CI are needed.

# Inputs to inspect

- Affected composables and state models.
- Profiler, compiler report or benchmark evidence if available.
- Lazy list item models and keys.
- Build variants and benchmark tasks.

# Required workflow

1. Reproduce or describe the performance symptom.
2. Inspect state reads, stability, keys and side effects.
3. Apply the smallest targeted fix.
4. Measure again with available tools.
5. Document residual risks and validation gaps.

# Rules

- Measure before large rewrites.
- Do not add remember everywhere blindly.
- Do not use @Stable/@Immutable unless the contract is true.
- Use stable lazy keys and content types when needed.
- Prefer release-mode measurement for meaningful performance claims.

# Files commonly touched

- `Compose screens`
- `UI models`
- `benchmark module`
- `baseline-prof.txt`
- `build.gradle.kts`

# Commands to validate

```bash
./gradlew assembleRelease
./gradlew connectedBenchmarkAndroidTest
./gradlew test
adb shell am profile start
```

# Common mistakes to avoid

- Optimizing from intuition only.
- Ignoring image size/loading.
- Using unstable mutable models as parameters.
- Missing lazy item keys.

# Checklist

- Symptom known.
- Stability/recomposition checked.
- Lazy keys checked.
- Measurement command provided.
- Fix does not harm readability.

# Example prompts

- Use $super-android-kotlin-firebase to diagnose jank in this LazyColumn.
- Use $super-android-kotlin-firebase to review Compose stability and recomposition.

# Expected response style

Respond with: brief diagnosis, change plan, affected files, code or diff summary, validation commands, tests added or recommended, risks, and next step. For review tasks, lead with findings ordered by severity.

# References

- ../../docs/audits/FUENTES_LOCALES.md
- references/recomposition-checklist.md
- templates/performance-investigation-template.md
