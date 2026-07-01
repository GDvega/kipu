---
name: firebase-messaging
description: "Use for Firebase Cloud Messaging, tokens, notifications, data messages, Android 13+ notification permission, foreground/background handling and push testing."
---

# Purpose

Integrate push messaging with clear permissions, safe payloads and testable token handling.

# When to use

- Adding FCM notifications or data messages.
- Managing device tokens.
- Handling POST_NOTIFICATIONS permission.
- Debugging foreground/background push behavior.

# Inputs to inspect

- Message type and target audience.
- Payload shape and sensitivity.
- Token lifecycle code.
- Permission UX and backend sender.

# Required workflow

1. Define notification vs data message behavior.
2. Add token storage/update strategy.
3. Add contextual notification permission flow.
4. Handle foreground/background display.
5. Test token refresh and message delivery states.

# Rules

- Do not put sensitive data in push payloads.
- Do not assume tokens are permanent.
- Handle Android 13+ notification permission.
- Avoid duplicate notifications.
- Document backend and topic risks.

# Files commonly touched

- `FirebaseMessagingService`
- `notification permission UI`
- `backend/functions sender`
- `AndroidManifest.xml`
- `tests/fakes`

# Commands to validate

```bash
./gradlew assembleDebug
adb shell pm grant <package> android.permission.POST_NOTIFICATIONS
adb logcat
firebase emulators:start
```

# Common mistakes to avoid

- Sending PHI or secrets in payload.
- Ignoring token refresh.
- No fallback when permission is denied.
- Assuming foreground and background behave the same.

# Checklist

- Permission flow exists.
- Token lifecycle handled.
- Payload safe.
- Foreground/background tested.
- Backend sender documented.

# Example prompts

- Use $super-android-kotlin-firebase to add FCM reminders.
- Use $super-android-kotlin-firebase to debug missing push notifications.

# Expected response style

Respond with: brief diagnosis, change plan, affected files, code or diff summary, validation commands, tests added or recommended, risks, and next step. For review tasks, lead with findings ordered by severity.

# References

- ../../docs/audits/FUENTES_LOCALES.md
- references/fcm-android-permissions.md
- templates/fcm-token-handling-template.md
