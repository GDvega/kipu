---
name: firebase-core
description: "Use for Firebase Android setup, google-services files, Firebase BoM, Gradle plugins, Emulator Suite, dev/staging/prod environments, Storage, Realtime Database, Remote Config and AI Logic foundations."
---

# Purpose

Set up Firebase products safely and consistently in Android apps.

# When to use

- Adding Firebase to an Android app.
- Fixing Firebase Gradle or google-services setup.
- Separating environments.
- Configuring Emulator Suite, Storage, Realtime Database, Remote Config or AI Logic.

# Inputs to inspect

- Firebase products required.
- Gradle files and version catalog.
- google-services policy and environments.
- Existing rules and emulator config.

# Required workflow

1. Identify required Firebase products and environments.
2. Review Gradle and BoM setup.
3. Add the minimum SDKs/plugins.
4. Plan rules and Emulator Suite validation.
5. Run Android build and document manual Firebase console steps.

# Rules

- Do not ask for private service accounts or production secrets.
- Separate dev/staging/prod projects when possible.
- Do not ship open rules.
- Use Emulator Suite for rules/functions when available.
- Consider costs and quotas for realtime listeners and storage.

# Files commonly touched

- `app/build.gradle.kts`
- `settings.gradle.kts`
- `gradle/libs.versions.toml`
- `google-services.json policy docs`
- `firebase.json`
- `firestore.rules`
- `database.rules.json`
- `storage.rules`

# Commands to validate

```bash
./gradlew assembleDebug
firebase emulators:start
firebase emulators:exec "npm test"
firebase deploy --only <target>
```

# Common mistakes to avoid

- Committing service accounts.
- Mixing dev and prod Firebase projects.
- Using allow read, write: if true in production.
- Ignoring Storage/Realtime Database rules.

# Checklist

- BoM configured.
- Environments separated.
- Rules planned.
- Emulator path known.
- Secrets not committed.

# Example prompts

- Use $super-android-kotlin-firebase to add Firebase Auth and Firestore safely.
- Use $super-android-kotlin-firebase to review my Firebase setup for dev/staging/prod.

# Expected response style

Respond with: brief diagnosis, change plan, affected files, code or diff summary, validation commands, tests added or recommended, risks, and next step. For review tasks, lead with findings ordered by severity.

# References

- ../../docs/audits/FUENTES_LOCALES.md
- references/firebase-setup-android.md
- templates/firebase-product-setup-template.md
