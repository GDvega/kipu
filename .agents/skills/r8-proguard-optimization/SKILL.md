---
name: r8-proguard-optimization
description: "Use for Android release shrinking, R8 and ProGuard keep rules, minify release failures, resource shrinking and Crashlytics mapping uploads."
---

# Purpose

Fix release-only failures and optimize shrinking without disabling R8 or adding broad unsafe keep rules.

# When to use

- App fails only in release builds.
- `minifyEnabled` or `shrinkResources` causes crashes.
- Retrofit, serialization, Hilt, Room or Firebase needs targeted keep rules.
- Crashlytics mapping upload must be configured.

# Inputs to inspect

- `app/build.gradle.kts` release build type
- `proguard-rules.pro` and `consumer-rules.pro`
- Reflection, serialization and Retrofit converters
- Room entities and Hilt modules
- Firebase and Crashlytics Gradle setup

# Required workflow

1. Reproduce using release or minified build.
2. Read R8 error output and mapping context.
3. Add the narrowest keep rule possible.
4. Verify release tests or smoke tests.
5. Document mapping file handling for Crashlytics.

# Rules

- Do not disable R8 as a permanent fix.
- Do not add `-keep class ** { *; }` except as a temporary diagnostic.
- Prefer library-documented rules and narrow app rules.
- Validate release behavior, not only debug.
- Preserve obfuscation unless debugging requires a temporary change.

# Files commonly touched

- `app/build.gradle.kts`
- `proguard-rules.pro`
- `consumer-rules.pro`
- `crashlytics` Gradle config
- Serialization models
- Network and DI setup

# Commands to validate

```bash
./gradlew assembleRelease
./gradlew app:minifyReleaseWithR8
./gradlew testReleaseUnitTest
```

# Common mistakes to avoid

- Testing only debug after changing R8 rules.
- Adding global keep rules that erase shrinking benefits.
- Forgetting mapping upload for crash reports.
- Ignoring consumer rules in library modules.

# Checklist

- Release failure reproduced.
- Specific rule added.
- Release build validated.
- Mapping behavior documented.
- Residual obfuscation risk mentioned.

# Example prompts

- Use $super-android-kotlin-firebase to fix this release-only R8 crash.
- Use $super-android-kotlin-firebase to review my ProGuard rules before release.

# Expected response style

Respond with: brief diagnosis, change plan, affected files, code or diff summary, validation commands, tests added or recommended, risks, and next step. For review tasks, lead with findings ordered by severity.

# References

- ../../docs/audits/FUENTES_LOCALES.md
- references/r8-release-checklist.md
- references/common-keep-rules.md
- references/firebase-r8-notes.md
- references/reflection-serialization-rules.md
- templates/proguard-rules-template.pro
- templates/release-buildtype-template.md
- templates/crashlytics-mapping-upload-template.md
