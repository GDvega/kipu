---
name: screen-ux-audit
description: "Use to audit an Android or Compose screen for UX clarity, hierarchy, accessibility, state coverage, performance risk and safe design changes after build/tests pass."
---

# Purpose

Review a screen like a senior Android UX maintainer: findings first, evidence-based, and safe to act on.

# When to use

- Auditing an existing Compose screen before visual changes.
- Reviewing an AI-generated screen for confusing hierarchy or fragile UX.
- Checking forms, dashboards, onboarding, settings, patient/caregiver or sensitive-data flows.
- Deciding which design issues should be fixed now versus deferred.

# Inputs to inspect

- Gate result from `design-test-gate`.
- Screen, route, ViewModel and UI state files.
- Navigation entry and exit points.
- Loading, empty, error, offline and permission states.
- Theme, accessibility semantics, previews and UI tests.
- Performance-sensitive UI such as lazy lists, images or animations.

# Required workflow

1. Confirm build/tests passed before auditing design changes.
2. Identify the screen goal, target user and risk level.
3. Inspect hierarchy, affordances, content clarity, state coverage and recovery paths.
4. Check accessibility, adaptive behavior and visual performance risks.
5. Lead with findings ordered by severity.
6. Recommend the smallest safe design changes and validation commands.

# Rules

- Findings must separate user-impacting problems from preferences.
- Do not rewrite the screen during audit unless the user asks for implementation.
- Do not duplicate generic code review; focus on UX and design behavior.
- Do not propose visual changes that require business logic changes without naming the contract.
- Include tests or previews needed to protect the audit findings.

# Related existing skills

## Local skills to invoke

- code-review-refactor
- accessibility
- jetpack-compose
- compose-performance
- design-test-gate

## External companion skills to use when installed

Do not assume these companion skills are installed. Prefer the local skills above first, then consult [Companion Skills](../../docs/COMPANION_SKILLS.md) for install and verification commands.

- aldefy/compose-skill — use for deeper Jetpack Compose, Material 3, design-to-code, theming or Compose architecture guidance.
- skydoves/android-testing-skills — use for deeper Android or Compose UI testing, semantics, assertions or test workflow guidance.
- skydoves/compose-performance-skills — use for deeper Compose performance, recomposition, stability or release-mode measurement guidance.
# Files commonly touched

- `*Screen.kt`
- `*Route.kt`
- `*ViewModel.kt`
- `ui/theme/*`
- Compose previews
- Compose UI tests

# Commands to validate

```bash
./gradlew assembleDebug
./gradlew test
./gradlew connectedDebugAndroidTest
./gradlew lint
```

# Common mistakes to avoid

- Listing subjective design preferences without user impact.
- Ignoring empty, error, permission or offline states.
- Auditing only the happy path.
- Recommending dense UI for stressful or sensitive workflows.
- Missing contrast, touch target, TalkBack or font-scale issues.

# Checklist

- Build/tests gate passed.
- Screen goal identified.
- User and risk level identified.
- State coverage inspected.
- Accessibility issues considered.
- Adaptive and performance risks considered.
- Findings prioritized.

# Example prompts

- Use $super-android-kotlin-firebase to audit this Compose screen UX before editing files.
- Use $super-android-kotlin-firebase to review this AI-generated onboarding screen and stop if tests fail.
- Use $super-android-kotlin-firebase to find UX risks in this medication tracking flow.

# Expected response style

Lead with prioritized findings. For each finding include severity, evidence, user impact, recommended fix and validation. End with deferred issues and commands to run after changes.

# References

- ../../docs/audits/FUENTES_LOCALES.md
- references/ux-audit-rubric.md
- templates/screen-ux-audit-report-template.md
