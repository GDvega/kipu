---
name: design-test-gate
description: "Use before Android UI, UX, Material 3, Compose design or visual refactor work to verify build and tests first, stop on failures, and define safe validation before design changes."
---

# Purpose

Protect Android design work with an engineering gate: build and tests first, design second.

# When to use

- Before changing Compose UI, Material 3 theme, screen layout, design system components or UX copy.
- When the user asks for a redesign, visual improvement, UI polish, app makeover or design refactor.
- When an AI-generated app needs design review but may have broken build, tests or Gradle setup.
- When design changes could affect navigation, ViewModels, Firebase data, local persistence or accessibility.

# Inputs to inspect

- Current branch status and changed files.
- Gradle files, module structure and available test tasks.
- Existing unit, UI, screenshot, preview or instrumentation tests.
- Recent build/test failures, CI logs and flaky test notes.
- Target screen, flow or component requested for design work.

# Required workflow

1. Inspect the project structure and identify the smallest relevant validation commands.
2. Run or request the baseline build/test commands before making visual changes.
3. If build or tests fail, stop design work and report the failure with the smallest next debugging step.
4. If validation passes, define the design scope, affected screens and regression risks.
5. Select related design skills only after the gate passes.
6. End with commands that should be rerun after design changes.

# Rules

- Always treat build/tests as the first step for UI or UX work.
- Do not redesign around a broken build unless the user explicitly asks to triage the failure first.
- Do not hide test failures behind visual recommendations.
- Preserve existing behavior, ViewModels, repositories, Firebase logic, navigation and persistence.
- Prefer the cheapest meaningful command first, then broaden validation for risky UI changes.
- If commands cannot run locally, explain the blocker and provide the exact command the user should run.

# Related existing skills

## Local skills to invoke

- testing
- debugging
- gradle-build
- code-review-refactor

## External companion skills to use when installed

Do not assume these companion skills are installed. Prefer the local skills above first, then consult [Companion Skills](../../docs/COMPANION_SKILLS.md) for install and verification commands.

- aldefy/compose-skill — use for deeper Jetpack Compose, Material 3, design-to-code, theming or Compose architecture guidance.
- skydoves/android-testing-skills — use for deeper Android or Compose UI testing, semantics, assertions or test workflow guidance.
# Files commonly touched

- `build.gradle.kts`
- `settings.gradle.kts`
- `gradle/libs.versions.toml`
- `src/test`
- `src/androidTest`
- CI workflow files
- Design target files only after validation passes

# Commands to validate

```bash
./gradlew assembleDebug
./gradlew test
./gradlew lint
./gradlew connectedDebugAndroidTest
npm run check
```

# Common mistakes to avoid

- Starting a redesign before confirming the app builds.
- Running only UI tests when a unit test would catch the broken state faster.
- Treating flaky tests as irrelevant to design work.
- Changing Gradle versions just to make design changes.
- Ignoring broken accessibility or UI tests because the screen "looks better."

# Checklist

- Baseline command identified.
- Build result known.
- Test result known or blocker reported.
- Design scope limited to requested surfaces.
- Related skills selected after the gate.
- Post-change validation commands listed.

# Example prompts

- Use $super-android-kotlin-firebase to run the design-test-gate before improving this Compose screen.
- Use $super-android-kotlin-firebase to verify this AI-generated Android app is safe to redesign.
- Use $super-android-kotlin-firebase to stop if build/tests fail, then tell me what to fix before UI polish.

# Expected response style

Start with gate status: passed, failed or blocked. If failed, lead with the failing command and stop design recommendations. If passed, summarize the safe design scope, related skills to use next, and validation commands to rerun.

# References

- ../../docs/audits/FUENTES_LOCALES.md
- references/design-test-gate-notes.md
- templates/design-test-gate-report-template.md
