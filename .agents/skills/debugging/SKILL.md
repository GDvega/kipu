---
name: debugging
description: "Use for Android debugging, Gradle build errors, Gradle sync, Logcat, adb, crashes, ANR, Firebase Crashlytics evidence and minimal reproductions."
---

# Purpose

Find root causes with evidence and apply minimal fixes.

# When to use

- A build, sync, runtime crash or ANR occurs.
- Logcat, adb or Crashlytics evidence needs triage.
- A minimal reproduction is needed.

# Inputs to inspect

- Exact command that failed.
- Full stacktrace and first error.
- Relevant Gradle versions and files.
- Reproduction steps and device/API.

# Required workflow

1. Separate first cause from downstream noise.
2. Locate the file or config responsible.
3. Apply one minimal fix.
4. Re-run the failing command.
5. Document remaining uncertainty and next diagnostic step.

# Rules

- Do not shotgun-update dependencies.
- Do not delete caches as the main solution.
- Do not silence warnings that indicate real incompatibility.
- Preserve crash evidence.
- Mention local-only issues separately.

# Files commonly touched

- `build.gradle.kts`
- `Gradle logs`
- `Logcat output`
- `Crashlytics reports`
- `source file in stacktrace`

# Commands to validate

```bash
./gradlew --stacktrace <task>
./gradlew build --scan
adb logcat
adb bugreport
./gradlew test
```

# Common mistakes to avoid

- Fixing the last error instead of the first cause.
- Changing many versions at once.
- Ignoring device/API-specific reproduction.
- Removing code without explaining risk.

# Checklist

- First cause identified.
- Minimal fix proposed.
- Verification command given.
- Reproduction documented.
- Risk/rollback noted.

# Example prompts

- Use $super-android-kotlin-firebase to debug this Gradle error.
- Use $super-android-kotlin-firebase to triage this Crashlytics stacktrace.

# Expected response style

Respond with: brief diagnosis, change plan, affected files, code or diff summary, validation commands, tests added or recommended, risks, and next step. For review tasks, lead with findings ordered by severity.

# References

- ../../docs/audits/FUENTES_LOCALES.md
- references/android-debugging-triage.md
- templates/debugging-report-template.md
