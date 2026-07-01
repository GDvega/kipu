---
name: context-aware-ui-design
description: "Use for Android and Compose UI design decisions that must adapt to app domain, target users, data sensitivity, environment, accessibility, performance and platform context after build/tests pass."
---

# Purpose

Design Android UI according to the app context, not generic aesthetics.

# When to use

- Designing or improving an Android screen after build/tests have passed.
- Choosing UI structure for a domain such as health, finance, education, productivity, maps or social features.
- Reviewing an AI-generated app for context mismatch, unclear hierarchy or risky interaction design.
- Adapting Compose UI for sensitive data, older adults, outdoor use, low connectivity or multi-device workflows.

# Inputs to inspect

- Gate result from `design-test-gate`.
- App domain, target user, primary journey and business goal.
- Sensitivity of displayed data and privacy/security implications.
- Environment of use: mobility, lighting, one-handed use, network quality and device class.
- Existing navigation, ViewModels, state models, Firebase/local data and permissions.
- Existing theme, typography, content density, accessibility and performance constraints.

# Required workflow

1. Confirm the design-test gate has passed or document why design cannot proceed.
2. Identify domain, user type, primary flow, data sensitivity, environment and risks.
3. Preserve existing architecture, ViewModels, repositories, Firebase behavior and navigation contracts.
4. Choose a UI direction that supports the user's task, not just visual novelty.
5. Route implementation details to related existing skills.
6. Recommend tests, previews and accessibility checks for the redesigned flow.

# Rules

- Never skip build/tests for visual redesign work.
- Design choices must name the context they serve.
- Do not change business logic, data ownership, Firebase rules or navigation semantics for aesthetics.
- Use Material 3 tokens and platform conventions unless the product context justifies a different pattern.
- Include accessibility, performance and security/privacy implications in the design plan.
- Prefer incremental design changes that can be reviewed and tested.

# Related existing skills

## Local skills to invoke

- jetpack-compose
- accessibility
- edge-to-edge-adaptive-ui
- compose-performance
- security-privacy
- design-test-gate

## External companion skills to use when installed

Do not assume these companion skills are installed. Prefer the local skills above first, then consult [Companion Skills](../../docs/COMPANION_SKILLS.md) for install and verification commands.

- aldefy/compose-skill — use for deeper Jetpack Compose, Material 3, design-to-code, theming or Compose architecture guidance.
- skydoves/compose-performance-skills — use for deeper Compose performance, recomposition, stability or release-mode measurement guidance.
# Files commonly touched

- Compose screen and route files
- `ui/theme/*`
- Design system components
- Navigation route files
- Preview files
- Compose UI tests

# Commands to validate

```bash
./gradlew assembleDebug
./gradlew test
./gradlew connectedDebugAndroidTest
./gradlew lint
```

# Common mistakes to avoid

- Applying a trendy layout without checking the app's real workflow.
- Treating all user groups as equally technical, patient or visually able.
- Increasing content density for sensitive or stressful tasks.
- Breaking navigation, state restoration or Firebase sync while polishing UI.
- Forgetting empty, loading, error, offline and permission-denied states.

# Checklist

- Build/tests gate passed.
- Domain and target user identified.
- Primary flow and risk level named.
- Data sensitivity considered.
- Environment and device constraints considered.
- Existing architecture preserved.
- Related skills selected for implementation details.

# Example prompts

- Use $super-android-kotlin-firebase to design this medication reminder screen for older adults after tests pass.
- Use $super-android-kotlin-firebase to review whether this AI-generated app UI matches its domain and users.
- Use $super-android-kotlin-firebase to propose a context-aware redesign without changing Firebase or ViewModels.

# Expected response style

Respond with: gate status, context diagnosis, design direction, preserved contracts, related skills to invoke, validation commands, and risks. If the gate failed, stop at failure analysis.

# References

- ../../docs/audits/FUENTES_LOCALES.md
- references/context-discovery-notes.md
- templates/context-aware-design-brief-template.md
