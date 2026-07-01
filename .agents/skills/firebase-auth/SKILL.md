---
name: firebase-auth
description: "Use for Firebase Auth email/password, Google Sign-In, anonymous auth, auth state, user profile, error handling and authorization-aware UI/data flows."
---

# Purpose

Implement authentication without leaking SDK details or weakening authorization.

# When to use

- Adding login, signup or logout.
- Handling auth state in ViewModel/UI.
- Linking user profile data.
- Mapping Firebase Auth errors safely.

# Inputs to inspect

- Required providers and UX flow.
- Auth state handling.
- User profile model.
- Firestore/Storage rules that depend on auth.

# Required workflow

1. Define providers and session UX.
2. Create or update an AuthRepository boundary.
3. Expose auth state to ViewModel.
4. Map errors to user-safe messages.
5. Add fakes/tests and rule checks.

# Rules

- Do not store passwords or tokens manually.
- Do not call FirebaseAuth directly from reusable Composables.
- Never trust UI checks as authorization.
- Rules must validate request.auth.uid.
- Keep errors useful but not secret-revealing.

# Files commonly touched

- `AuthRepository.kt`
- `AuthViewModel.kt`
- `login Compose screens`
- `firestore.rules`
- `storage.rules`
- `tests/fakes`

# Commands to validate

```bash
./gradlew test
./gradlew assembleDebug
firebase emulators:exec "npm test"
```

# Common mistakes to avoid

- Persisting provider tokens.
- Showing raw Firebase exception internals.
- Creating profile documents without rules.
- Ignoring anonymous account upgrade flows.

# Checklist

- AuthRepository boundary exists.
- Auth state observable.
- Rules checked.
- Errors mapped.
- Tests/fakes added.

# Example prompts

- Use $super-android-kotlin-firebase to add Google Sign-In and email login.
- Use $super-android-kotlin-firebase to review auth state handling.

# Expected response style

Respond with: brief diagnosis, change plan, affected files, code or diff summary, validation commands, tests added or recommended, risks, and next step. For review tasks, lead with findings ordered by severity.

# References

- ../../docs/audits/FUENTES_LOCALES.md
- references/auth-state-and-rules.md
- templates/auth-repository-template.md
