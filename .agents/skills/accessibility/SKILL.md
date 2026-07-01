---
name: accessibility
description: "Use for Android and Compose accessibility, contentDescription, touch targets, contrast, TalkBack, semantics, keyboard navigation and font-scale resilience."
---

# Purpose

Make mobile UI understandable and operable for more users.

# When to use

- Reviewing a Compose screen for accessibility.
- Adding semantics or content descriptions.
- Fixing font scale, contrast, touch target or TalkBack issues.
- Designing older-adult or patient-friendly flows.

# Inputs to inspect

- Composables and UI states.
- Icons/images and labels.
- Theme colors and typography.
- Forms, error messages and navigation order.

# Required workflow

1. Inspect visual and semantics hierarchy.
2. Add labels, roles and error descriptions.
3. Check touch targets, contrast and font scale.
4. Review TalkBack/focus order if possible.
5. Add UI tests or manual checks.

# Rules

- Content descriptions must add useful information.
- Do not rely on color alone.
- Preserve readable text at large font scale.
- Prefer clear labels and recoverable errors.
- Keep semantics aligned with visual order.

# Files commonly touched

- `Compose screen files`
- `design system components`
- `theme files`
- `Compose UI tests`

# Commands to validate

```bash
./gradlew connectedDebugAndroidTest
./gradlew lint
adb shell settings put system font_scale 1.3
```

# Common mistakes to avoid

- Redundant descriptions.
- Icon-only buttons without labels.
- Text clipped at font scale.
- Hidden error state for screen readers.

# Checklist

- Labels useful.
- Touch targets checked.
- Contrast checked.
- Font scale considered.
- TalkBack/focus order considered.

# Example prompts

- Use $super-android-kotlin-firebase to review accessibility of this Compose screen.
- Use $super-android-kotlin-firebase to make this form TalkBack-friendly.

# Expected response style

Respond with: brief diagnosis, change plan, affected files, code or diff summary, validation commands, tests added or recommended, risks, and next step. For review tasks, lead with findings ordered by severity.

# References

- ../../docs/audits/FUENTES_LOCALES.md
- references/compose-accessibility-review.md
- templates/accessibility-review-template.md
