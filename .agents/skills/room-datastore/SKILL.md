---
name: room-datastore
description: "Use for Room, DAO, entities, migrations, DataStore Preferences/Proto, local cache, offline-first repositories and local/remote synchronization."
---

# Purpose

Persist local data with explicit migrations, Flow APIs and offline-first boundaries.

# When to use

- Adding Room or DataStore.
- Designing local cache and sync.
- Writing migrations.
- Moving local data behind a repository.

# Inputs to inspect

- Domain model and local queries.
- Database version and existing migrations.
- Preference vs structured data needs.
- Sync/conflict behavior.

# Required workflow

1. Choose Room for structured relational data or DataStore for preferences/config.
2. Define entity/DAO or DataStore schema.
3. Add migration strategy.
4. Expose repository/domain models, not raw entities.
5. Test DAO, migrations and ViewModel behavior.

# Rules

- Do not run DB work on main thread.
- Do not use destructive migration in production without explicit approval.
- Do not store relational data in Preferences DataStore.
- Use Flow for observed data.
- Document conflict handling.

# Files commonly touched

- `Room entities`
- `DAO files`
- `Database class`
- `DataStore serializers`
- `Repository implementation`
- `migration tests`

# Commands to validate

```bash
./gradlew test
./gradlew connectedDebugAndroidTest
./gradlew assembleDebug
```

# Common mistakes to avoid

- Dropping user data in migration.
- Leaking entities to UI without reason.
- Ignoring timezone/conflict concerns.
- Using DataStore for large lists.

# Checklist

- Entity/DAO clear.
- Migration safe.
- Repository boundary exists.
- Offline behavior defined.
- Tests added.

# Example prompts

- Use $super-android-kotlin-firebase to add Room cache for Firestore data.
- Use $super-android-kotlin-firebase to migrate SharedPreferences to DataStore.

# Expected response style

Respond with: brief diagnosis, change plan, affected files, code or diff summary, validation commands, tests added or recommended, risks, and next step. For review tasks, lead with findings ordered by severity.

# References

- ../../docs/audits/FUENTES_LOCALES.md
- references/room-datastore-offline-first.md
- templates/room-datastore-template.md
