---
name: compose-contextual-design-system
description: "Use for creating or reviewing a Compose Material 3 design system whose tokens, components, previews, accessibility and performance rules fit the app context after build/tests pass."
---

# Purpose

Shape a Compose design system around the product context, while preserving architecture and testability.

# When to use

- Creating or refactoring reusable Compose components.
- Defining Material 3 color, typography, spacing, shape, elevation or motion decisions.
- Turning repeated screen UI into a small component system.
- Reviewing whether AI-generated components are reusable, accessible and consistent.

# Inputs to inspect

- Gate result from `design-test-gate`.
- Existing theme files and Material 3 setup.
- Reusable components, previews and component tests.
- Product context from `context-aware-ui-design`.
- Edge-to-edge/adaptive requirements.
- Performance-sensitive components such as lists, cards, images and animations.

# Required workflow

1. Confirm build/tests passed before changing theme or components.
2. Identify existing tokens and component ownership.
3. Decide which components are product-specific and which should stay local to a screen.
4. Define component contracts: state, slots, callbacks, semantics and preview states.
5. Preserve stable APIs and avoid broad visual rewrites.
6. Add or recommend previews/tests for component states and accessibility.

# Rules

- Use existing Material 3 theme tokens before adding new ones.
- Do not create a design system for one-off UI.
- Keep reusable components stateless where possible.
- Do not pass ViewModels into reusable design components.
- Avoid experimental APIs unless the user explicitly accepts the risk.
- Check performance risks for reusable list items and animated components.

# Related existing skills

## Local skills to invoke

- jetpack-compose
- xml-to-compose-migration
- edge-to-edge-adaptive-ui
- compose-performance
- accessibility

## External companion skills to use when installed

Do not assume these companion skills are installed. Prefer the local skills above first, then consult [Companion Skills](../../docs/COMPANION_SKILLS.md) for install and verification commands.

- aldefy/compose-skill — use for deeper Jetpack Compose, Material 3, design-to-code, theming or Compose architecture guidance.
- skydoves/compose-performance-skills — use for deeper Compose performance, recomposition, stability or release-mode measurement guidance.
# Files commonly touched

- `ui/theme/*`
- `ui/components/*`
- Compose preview files
- Component UI tests
- Screen files that consume shared components

# Commands to validate

```bash
./gradlew assembleDebug
./gradlew test
./gradlew connectedDebugAndroidTest
./gradlew lint
```

# Common mistakes to avoid

- Adding abstract tokens without a product need.
- Moving screen-specific layout into global components too early.
- Hard-coding colors, typography or dimensions inside reusable components.
- Creating components without loading, disabled, error or long-text previews.
- Breaking semantics or test tags when replacing local UI.

# Checklist

- Build/tests gate passed.
- Existing theme inspected.
- Product context known.
- Component ownership clear.
- State and callback contract defined.
- Previews cover major states.
- Accessibility and performance considered.

# Example prompts

- Use $super-android-kotlin-firebase to create a contextual Compose design system for this app after tests pass.
- Use $super-android-kotlin-firebase to review these AI-generated Material 3 components for reuse and accessibility.
- Use $super-android-kotlin-firebase to extract shared UI without changing ViewModels or navigation.

# Expected response style

Respond with: gate status, design-system diagnosis, token/component plan, files affected, related skills, validation commands and risks. Keep recommendations scoped to reusable UI that the app actually needs.

# References

- ../../docs/audits/FUENTES_LOCALES.md
- references/design-system-scope-notes.md
- templates/component-contract-template.md
