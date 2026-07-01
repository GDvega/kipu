---
name: edge-to-edge-adaptive-ui
description: "Use for Android edge-to-edge, WindowInsets, system bars, adaptive layouts, tablets, foldables and Compose UI resilience."
---

# Purpose

Modernize UI for current Android devices without content overlap, broken system bars or inaccessible layouts.

# When to use

- Enabling edge-to-edge display.
- Fixing content hidden under status or navigation bars.
- Adding tablet, foldable or adaptive layouts.
- Improving Compose insets and window size behavior.

# Inputs to inspect

- `MainActivity.kt`
- Theme and system bar setup
- `Scaffold`, app bars and navigation bars
- WindowInsets usage
- Adaptive layout breakpoints
- Dark mode and accessibility settings

# Required workflow

1. Inspect current system bar and decor fits setup.
2. Replace fixed padding with WindowInsets-aware layout.
3. Check compact, medium and expanded layouts.
4. Validate gesture and three-button navigation.
5. Test dark mode, font scale and TalkBack order.

# Rules

- Do not use fixed status bar padding as a universal fix.
- Do not hide important content behind bars or cutouts.
- Use WindowInsets or Material components intentionally.
- Do not break accessibility while optimizing layout.
- Consider tablets and foldables for core workflows.

# Files commonly touched

- `MainActivity.kt`
- `ui/theme/*`
- `*Screen.kt`
- `*Scaffold.kt`
- `res/values/themes.xml`
- Compose previews and UI tests

# Commands to validate

```bash
./gradlew assembleDebug
./gradlew test
./gradlew connectedDebugAndroidTest
```

# Common mistakes to avoid

- Testing only one navigation mode.
- Adding padding twice through Scaffold and manual insets.
- Forgetting landscape, tablets or font scale.
- Changing colors without contrast checks.

# Checklist

- Insets handled once.
- System bars readable.
- Adaptive states considered.
- Dark mode checked.
- Accessibility risks named.

# Example prompts

- Use $super-android-kotlin-firebase to fix edge-to-edge overlap in this Compose screen.
- Use $super-android-kotlin-firebase to make this layout tablet-ready.

# Expected response style

Respond with: brief diagnosis, change plan, affected files, code or diff summary, validation commands, tests added or recommended, risks, and next step. For review tasks, lead with findings ordered by severity.

# References

- ../../docs/audits/FUENTES_LOCALES.md
- references/edge-to-edge-checklist.md
- references/insets-patterns-compose.md
- references/adaptive-layouts.md
- references/tablets-foldables-notes.md
- templates/edge-to-edge-mainactivity-template.md
- templates/adaptive-scaffold-template.md
- templates/window-size-class-template.md
