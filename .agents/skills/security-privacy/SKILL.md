---
name: security-privacy
description: "Use for Android and Firebase security, secrets, API keys, Firebase Security Rules, local encryption, biometrics, least privilege, privacy-first data handling and sensitive data reviews."
---

# Purpose

Reduce real security and privacy risks in Android/Firebase apps.

# When to use

- Auditing sensitive data handling.
- Reviewing Firebase rules.
- Checking secrets, logs or permissions.
- Adding biometric or local encryption flows.

# Inputs to inspect

- Data classification and threat model.
- Rules files and storage paths.
- Logs, analytics and Crashlytics usage.
- Permissions and local storage.

# Required workflow

1. Classify data and trust boundaries.
2. Search for secrets and sensitive logs.
3. Review Firebase rules and Android permissions.
4. Propose least-privilege fixes.
5. Add tests or manual verification steps.

# Rules

- Never exfiltrate or print secrets.
- No open production rules.
- Do not rely on client-only authorization.
- Do not log PII, tokens or sensitive health/financial data.
- Use encryption only with a clear threat model.

# Files commonly touched

- `firestore.rules`
- `database.rules.json`
- `storage.rules`
- `AndroidManifest.xml`
- `logging/analytics code`
- `local storage code`

# Commands to validate

```bash
git grep -n "apiKey\|secret\|password\|token"
firebase emulators:exec "npm test"
./gradlew lint
./gradlew test
```

# Common mistakes to avoid

- Putting private keys in BuildConfig.
- Treating Firebase API keys as authorization.
- Logging user identifiers unnecessarily.
- Opening rules for convenience.

# Checklist

- Secrets not committed.
- Rules least-privilege.
- Logs safe.
- Permissions minimal.
- Risks documented.

# Example prompts

- Use $super-android-kotlin-firebase to audit Firestore and Storage rules.
- Use $super-android-kotlin-firebase to check this app for secret leaks.

# Expected response style

Respond with: brief diagnosis, change plan, affected files, code or diff summary, validation commands, tests added or recommended, risks, and next step. For review tasks, lead with findings ordered by severity.

# References

- ../../docs/audits/FUENTES_LOCALES.md
- references/android-firebase-security.md
- templates/security-review-template.md
