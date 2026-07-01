---
name: firebase-crashlytics-analytics
description: "Use for Firebase Crashlytics, non-fatal errors, logs, Analytics events, user properties, Remote Config experiment context and privacy-safe observability."
---

# Purpose

Observe Android app quality and behavior without collecting unnecessary sensitive data.

# When to use

- Adding Crashlytics or Analytics.
- Defining event taxonomy.
- Logging non-fatal errors.
- Adding privacy-aware user properties.
- Connecting Remote Config context to diagnostics.

# Inputs to inspect

- Events and funnels desired.
- Sensitive data classes.
- Build types and release behavior.
- Privacy policy requirements.

# Required workflow

1. Define minimal useful events and non-fatal signals.
2. Add wrappers if the project already uses abstraction.
3. Remove PII from logs/events.
4. Differentiate debug and release.
5. Validate dashboards or test logs manually.

# Rules

- Never log tokens, emails, medical/financial data or secrets.
- Use stable event names.
- Do not use Analytics as authorization.
- Respect consent/opt-out requirements.
- Keep breadcrumbs useful and minimal.

# Files commonly touched

- `analytics wrapper`
- `Crashlytics setup`
- `ViewModels/use cases for non-fatals`
- `privacy docs`
- `build.gradle.kts`

# Commands to validate

```bash
./gradlew assembleRelease
./gradlew test
adb logcat
firebase crashlytics:symbols:upload --help
```

# Common mistakes to avoid

- Logging raw exception context with PII.
- Too many noisy events.
- Debug-only verification for release behavior.
- User properties containing sensitive values.

# Checklist

- No PII.
- Events documented.
- Non-fatals actionable.
- Build type behavior clear.
- Privacy policy impact mentioned.

# Example prompts

- Use $super-android-kotlin-firebase to add Crashlytics non-fatal logging.
- Use $super-android-kotlin-firebase to define Analytics events for onboarding.

# Expected response style

Respond with: brief diagnosis, change plan, affected files, code or diff summary, validation commands, tests added or recommended, risks, and next step. For review tasks, lead with findings ordered by severity.

# References

- ../../docs/audits/FUENTES_LOCALES.md
- references/privacy-safe-observability.md
- templates/analytics-event-taxonomy-template.md
