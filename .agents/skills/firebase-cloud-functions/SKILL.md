---
name: firebase-cloud-functions
description: "Use for Firebase Cloud Functions callable functions, HTTP functions, triggers, server-side validation, security, deployment and local testing."
---

# Purpose

Move privileged or trusted logic server-side while keeping Android clients simple.

# When to use

- Adding callable or HTTP functions.
- Adding Firestore/Auth triggers.
- Validating operations that rules cannot express.
- Testing functions locally.

# Inputs to inspect

- Use case and trust boundary.
- Input schema and auth/role requirements.
- Runtime, region and emulator setup.
- Android caller contract.

# Required workflow

1. Choose callable, HTTP or trigger based on caller and event source.
2. Define input/output and error contract.
3. Validate auth, roles and data server-side.
4. Test locally with Emulator Suite.
5. Document deploy command and rollback risk.

# Rules

- Do not use functions as an insecure bypass.
- Validate every input.
- Do not log secrets or sensitive payloads.
- Make idempotency explicit for triggers.
- Control region and cost implications.

# Files commonly touched

- `functions/src/*`
- `firebase.json`
- `firestore.rules`
- `Android repository/service caller`
- `function tests`

# Commands to validate

```bash
npm test
firebase emulators:start --only functions,firestore
firebase emulators:exec "npm test"
firebase deploy --only functions:<name>
```

# Common mistakes to avoid

- No auth checks.
- Returning inconsistent error shapes.
- Deploying without emulator test.
- Creating duplicate writes from non-idempotent triggers.

# Checklist

- Auth checked.
- Input validated.
- Errors typed.
- Emulator tested.
- Deploy command documented.

# Example prompts

- Use $super-android-kotlin-firebase to create a callable invite validation function.
- Use $super-android-kotlin-firebase to review this Firestore trigger.

# Expected response style

Respond with: brief diagnosis, change plan, affected files, code or diff summary, validation commands, tests added or recommended, risks, and next step. For review tasks, lead with findings ordered by severity.

# References

- ../../docs/audits/FUENTES_LOCALES.md
- references/functions-validation-and-emulator.md
- templates/cloud-functions-template.md
