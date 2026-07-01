---
name: firebase-emulator-ci
description: "Use for Firebase Emulator Suite setup, Firestore/Auth/Functions/Storage rules tests and CI validation with emulators:exec."
---

# Purpose

Run Firebase-sensitive tests locally and in CI without touching production projects or open rules.

# When to use

- Testing Firebase Security Rules.
- Configuring Firebase Emulator Suite.
- Adding CI for Firestore, Auth, Functions or Storage tests.
- Separating dev, staging and production Firebase environments.

# Inputs to inspect

- `firebase.json`
- `firestore.rules` and `storage.rules`
- `functions/` package files
- `.github/workflows/*`
- `package.json` scripts
- `test/` and `androidTest/`

# Required workflow

1. Identify Firebase products that need emulators.
2. Configure emulator ports and project aliases.
3. Write rules or functions tests against emulators only.
4. Run tests with `emulators:exec` in CI.
5. Document environment separation and production safeguards.

# Rules

- Do not point emulator tests at production.
- Do not use open rules in production.
- Use `emulators:exec` for CI lifecycle when possible.
- Keep service accounts out of the repo.
- Fail CI on rules regressions.

# Files commonly touched

- `firebase.json`
- `firestore.rules`
- `storage.rules`
- `functions/package.json`
- `.github/workflows/*`
- `test/**/*rules*`

# Commands to validate

```bash
firebase emulators:exec --only firestore "npm test"
npm test
./gradlew test
```

# Common mistakes to avoid

- Running tests against real production data.
- Keeping permissive demo rules after setup.
- Starting emulators manually in CI without cleanup.
- Forgetting Storage rules when Firestore is tested.

# Checklist

- Emulator config exists.
- Rules tests use emulator host.
- CI command uses emulator lifecycle.
- No secrets committed.
- Dev/staging/prod separation documented.

# Example prompts

- Use $super-android-kotlin-firebase to add Firebase Emulator CI for Firestore rules.
- Use $super-android-kotlin-firebase to make these Firebase tests avoid production.

# Expected response style

Respond with: brief diagnosis, change plan, affected files, code or diff summary, validation commands, tests added or recommended, risks, and next step. For review tasks, lead with findings ordered by severity.

# References

- ../../docs/audits/FUENTES_LOCALES.md
- references/emulator-suite-setup.md
- references/firestore-rules-testing.md
- references/github-actions-emulator-ci.md
- references/dev-staging-prod-separation.md
- templates/firebase-json-template.md
- templates/firestore-rules-test-template.md
- templates/github-actions-firebase-emulator-template.yml
- templates/emulator-exec-script-template.md
