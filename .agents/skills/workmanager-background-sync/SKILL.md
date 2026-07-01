---
name: workmanager-background-sync
description: "Use for WorkManager background sync, offline-first uploads, retry/backoff policies, constraints, foreground work and worker tests."
---

# Purpose

Implement reliable Android background work without leaking scopes, draining battery or duplicating sync jobs.

# When to use

- Scheduling background sync.
- Uploading data when network returns.
- Adding periodic or one-time WorkManager jobs.
- Testing retry and backoff behavior.
- Designing offline-first synchronization.

# Inputs to inspect

- Worker classes
- Repository and Room transaction boundaries
- Network layer and DTO mapping
- DI setup for workers
- `AndroidManifest.xml` permissions
- Existing sync scheduler code

# Required workflow

1. Define the sync contract and idempotency key.
2. Choose one-time or periodic work with constraints.
3. Keep work inside structured coroutine APIs.
4. Persist progress and retry safely.
5. Add unit tests for success, retry and failure.

# Rules

- Do not use `GlobalScope`.
- Do not create unnecessary Services.
- Do not run infinite loops inside Workers.
- Use constraints for network/battery when relevant.
- Make sync idempotent and retry-safe.

# Files commonly touched

- `*Worker.kt`
- `SyncScheduler.kt`
- Repository and DAO files
- DI modules
- `AndroidManifest.xml`
- Worker tests

# Commands to validate

```bash
./gradlew test
./gradlew assembleDebug
```

# Common mistakes to avoid

- Scheduling duplicate unique work.
- Retrying non-idempotent uploads.
- Doing long foreground work without notification requirements.
- Ignoring process death and app restart.

# Checklist

- Constraints chosen.
- Unique work policy documented.
- Backoff configured.
- Sync idempotent.
- Worker tests added or recommended.

# Example prompts

- Use $super-android-kotlin-firebase to add WorkManager offline sync.
- Use $super-android-kotlin-firebase to test this CoroutineWorker retry behavior.

# Expected response style

Respond with: brief diagnosis, change plan, affected files, code or diff summary, validation commands, tests added or recommended, risks, and next step. For review tasks, lead with findings ordered by severity.

# References

- ../../docs/audits/FUENTES_LOCALES.md
- references/workmanager-patterns.md
- references/offline-sync-strategy.md
- references/retry-and-backoff.md
- references/foreground-work.md
- templates/coroutine-worker-template.kt
- templates/sync-scheduler-template.kt
- templates/worker-test-template.md
