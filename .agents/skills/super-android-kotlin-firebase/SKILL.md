---
name: super-android-kotlin-firebase
description: Master orchestration skill for building, reviewing, refactoring, testing, securing, optimizing and releasing modern Android apps with Kotlin, Jetpack Compose and Firebase.
---

# Super Android Kotlin Firebase

Use this as an orchestrator, not as a giant implementation guide. Select the smallest set of subskills needed for the user's task, inspect the repository first, then act with small verifiable changes.

## When to use this super skill

- Building or reviewing Android apps with Kotlin, Compose, Material 3, Gradle, Firebase, Room, DataStore, Retrofit/Ktor, Hilt/Koin or Play release concerns.
- Improving Android UI/UX design according to app context after build/tests are known.
- Creating features that cross UI, state, domain, data, local storage, remote APIs or Firebase.
- Auditing architecture, security, Firebase rules, tests, accessibility, performance or release readiness.
- Fixing Gradle, Compose, Firebase, Android Studio, adb, Logcat, Crashlytics or CI failures.

## When not to use it

- Non-Android apps unless Firebase, Kotlin Multiplatform or Android-specific code is involved.
- Unauthorized security testing, credential extraction, bypasses or destructive operations.
- Bulk rewrites without a user goal, tests or rollback path.

## How to select subskills

Read only the relevant `skills/<name>/SKILL.md` files:

- Platform: `android-core`, `gradle-build`, `debugging`.
- Build and migration: `agp9-kotlin-migration`, `dependency-upgrade-safety`.
- Kotlin/state: `kotlin-core`, `dependency-injection`.
- UI: `jetpack-compose`, `android-navigation-3`, `xml-to-compose-migration`, `edge-to-edge-adaptive-ui`, `compose-performance`, `accessibility`.
- Design and UX: `design-test-gate`, `context-aware-ui-design`, `compose-contextual-design-system`, `screen-ux-audit`, `ui-state-design`.
- Architecture/data: `android-architecture`, `room-datastore`, `networking-retrofit-ktor`, `workmanager-background-sync`.
- Firebase: `firebase-core`, `firebase-auth`, `firestore`, `firestore-costs-quotas`, `firebase-emulator-ci`, `firebase-cloud-functions`, `firebase-messaging`, `firebase-crashlytics-analytics`, `firebase-ai-logic`.
- Performance/release: `macrobenchmark-baseline-profiles`, `r8-proguard-optimization`, `play-store-release`.
- Quality/review: `testing`, `security-privacy`, `code-review-refactor`.

Use `docs/audits/FUENTES_LOCALES.md` to locate the local reference repositories when deeper source context is needed.

# Companion skill routing

- If a user asks for deep Compose guidance, prefer local `jetpack-compose` first, then suggest `aldefy/compose-skill` as a companion if installed.
- If a user asks for deep testing guidance, prefer local `testing` first, then suggest `skydoves/android-testing-skills` as a companion if installed.
- If a user asks for deep Compose performance guidance, prefer local `compose-performance` first, then suggest `skydoves/compose-performance-skills` as a companion if installed.
- If a user asks for official Android workflow alignment, prefer the relevant local skill first, then suggest `android/skills` as a companion if installed.
- Do not assume companion skills are installed.
- Do not copy external skill content into the answer.
- If companion skills are not installed, provide a safe install/list command from `docs/COMPANION_SKILLS.md`.

# Historical source awareness

- This repository was built from original synthesis informed by historical reference repositories up to `v0.4.4`.
- Do not quote or copy external repository content as if it belonged to this repo.
- When a user asks about source lineage, point them to `NOTICE.md`, `docs/audits/HISTORICAL_SOURCE_PROVENANCE_V0_4_4.md`, `docs/LEGAL_AND_ATTRIBUTION_POLICY.md` and `docs/COMPANION_SKILLS.md`.
- Prefer local skills first.
- Suggest companion skills only when useful and do not assume they are installed.

## Global agent rules

1. Understand the user's goal before editing.
2. Detect whether the project is Android Kotlin, Compose, Firebase, KMP or mixed.
3. Inspect structure, Gradle files, modules, tests and existing patterns before changing code.
4. Do not invent APIs.
5. Do not update Gradle, AGP, Kotlin, Compose or Firebase versions without compatibility checks.
6. Prefer small, safe, verifiable changes.
7. Explain files modified and commands to validate.
8. Propose or add tests when logic changes.
9. Never request or expose private credentials.
10. Preserve real line breaks in Markdown, YAML and JSON files; do not compress frontmatter, headings or code blocks into single-line raw output.
11. For visual design work, run or request build/tests first; if they fail, stop and report the failure before redesigning.

## Architecture rules

Separate UI, domain and data in real apps. Hide Firebase, Room and network SDKs behind repositories. Add modules only when they reduce coupling or build/test complexity. Preserve the existing architecture unless the user asks for a migration or the current design blocks the task.

## Kotlin rules

Use structured concurrency. Do not use `GlobalScope`. Preserve cancellation. Model state and errors explicitly. Avoid `!!` unless the invariant is enforced nearby. Keep domain logic testable.

## Compose rules

Use Route + Screen for stateful screens. Hoist state. Keep reusable Composables stateless. Use Material 3 theme tokens. Add previews and semantics. Treat recomposition and lazy list performance as measurable.

For design work, choose UI structure from the app context: domain, target user, primary flow, data sensitivity, environment, accessibility needs and device class. Preserve ViewModels, navigation, Firebase, repositories and existing architecture unless the user asks for a functional change.

## Firebase rules

Design data model and Security Rules together. Use Emulator Suite for rules/functions when possible. Separate dev/staging/prod. Avoid open production rules. Consider indexes, listener cost, Storage metadata and Realtime Database validation.

## Security rules

Do not commit service accounts, keystores, `.env`, private tokens or credentials. Do not log PII. Do not store production secrets in BuildConfig. Review scripts before running them.

## Testing rules

Test domain and ViewModel logic first. Use fakes for repository behavior. Use coroutine test APIs and virtual time. Add Compose UI tests for critical flows. Add Firebase emulator tests for sensitive rules.

## Performance rules

Measure before broad optimization. Check recomposition, stability, Lazy keys, images, database/network listeners and release-mode behavior. Consider baseline profiles for startup and critical journeys.

## Accessibility rules

Check labels, touch targets, contrast, font scale, TalkBack order and error semantics. Do not communicate status by color alone.

## Release rules

Validate release builds, signing, R8, privacy policy, Data Safety, permissions, Crashlytics and staged rollout. Never commit signing secrets.

## Required response format

Use this order unless the user asks for a different format:

1. Brief diagnosis.
2. Change plan.
3. Affected files.
4. Code or change summary.
5. Commands to validate.
6. Tests added or recommended.
7. Risks or warnings.
8. Next step.

## Before editing checklist

- Relevant files inspected.
- Correct subskills selected.
- Existing architecture understood.
- Security and privacy implications considered.
- Validation command identified.

## Before final answer checklist

- Changes are scoped.
- APIs are real or verified.
- Tests/commands are included.
- Secrets are not exposed.
- Remaining risks are named.
