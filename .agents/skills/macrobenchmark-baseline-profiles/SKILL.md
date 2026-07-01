---
name: macrobenchmark-baseline-profiles
description: "Use for Android Macrobenchmark, Baseline Profile generation, startup measurement, scroll/jank benchmarks and release performance validation."
---

# Purpose

Measure real release-like performance and create Baseline Profiles for user-critical journeys.

# When to use

- Improving startup time.
- Measuring scroll jank or Compose performance in release.
- Adding a macrobenchmark module.
- Generating or validating Baseline Profiles.
- Comparing before/after performance changes.

# Inputs to inspect

- `settings.gradle.kts`
- Benchmark module setup
- App build types and profileable config
- `baseline-prof.txt`
- Startup path and critical screens
- CI benchmark workflow

# Required workflow

1. Identify a real user journey to measure.
2. Create or update macrobenchmark module.
3. Run release/profileable benchmarks.
4. Generate Baseline Profile for startup and critical paths.
5. Report results and tradeoffs clearly.

# Rules

- Do not measure only debug builds.
- Do not optimize without baseline numbers.
- Do not benchmark unrealistic flows.
- Keep benchmark code separate from production code.
- Explain device and build conditions.

# Files commonly touched

- `benchmark/build.gradle.kts`
- `settings.gradle.kts`
- `app/build.gradle.kts`
- `baseline-prof.txt`
- `*Benchmark.kt`
- `BaselineProfileGenerator.kt`

# Commands to validate

```bash
./gradlew connectedBenchmarkAndroidTest
./gradlew generateBaselineProfile
./gradlew assembleRelease
```

# Common mistakes to avoid

- Running benchmarks on debug.
- Changing multiple variables before comparison.
- Ignoring warmup and device state.
- Failing to commit generated profile updates.

# Checklist

- Journey chosen.
- Release-like variant used.
- Baseline measured.
- Profile generated.
- Results summarized.

# Example prompts

- Use $super-android-kotlin-firebase to add startup macrobenchmarks.
- Use $super-android-kotlin-firebase to generate a Baseline Profile for this Compose app.

# Expected response style

Respond with: brief diagnosis, change plan, affected files, code or diff summary, validation commands, tests added or recommended, risks, and next step. For review tasks, lead with findings ordered by severity.

# References

- ../../docs/audits/FUENTES_LOCALES.md
- references/macrobenchmark-setup.md
- references/baseline-profile-generation.md
- references/startup-and-scroll-benchmarks.md
- references/release-performance-checklist.md
- templates/macrobenchmark-module-template.md
- templates/startup-benchmark-template.kt
- templates/scroll-benchmark-template.kt
- templates/baseline-profile-generator-template.kt
