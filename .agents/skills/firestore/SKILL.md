---
name: firestore
description: "Use for Firestore and Realtime Database data modeling, collections/documents/paths, queries, indexes, transactions, batch writes, offline persistence, Security Rules and cost-aware access patterns."
---

# Purpose

Design Firebase databases around access patterns, rules, sync and cost.

# When to use

- Modeling Firestore collections or Realtime Database paths.
- Adding queries, listeners, transactions or batch writes.
- Writing or auditing Security Rules.
- Designing caregiver/patient, team, role or ownership access.

# Inputs to inspect

- Entities and access patterns.
- Readers, writers and roles.
- Queries, indexes and listener frequency.
- Current firestore.rules/database.rules.json.
- Expected data volume and offline needs.

# Required workflow

1. List access patterns before schema.
2. Choose paths/collections and denormalization intentionally.
3. Design rules with schema and roles.
4. Identify indexes and cost-sensitive listeners.
5. Add repository code and emulator allow/deny tests.

# Rules

- Rules are part of the feature, not an afterthought.
- Do not trust client-provided owner fields alone.
- Validate required fields, types and immutable fields.
- Use transactions for invariants.
- Mention cost risks for broad listeners.

# Files commonly touched

- `firestore.rules`
- `database.rules.json`
- `indexes config`
- `repository/data source files`
- `Firebase emulator tests`

# Commands to validate

```bash
firebase emulators:start
firebase emulators:exec "npm test"
./gradlew test
./gradlew assembleDebug
```

# Common mistakes to avoid

- Open rules.
- Queries that rules cannot authorize.
- Missing indexes.
- Unbounded realtime listeners.
- Denormalization without write strategy.

# Checklist

- Access patterns documented.
- Rules least-privilege.
- Indexes considered.
- Offline behavior defined.
- Cost risks mentioned.

# Example prompts

- Use $super-android-kotlin-firebase to design Firestore for patients and caregivers.
- Use $super-android-kotlin-firebase to audit these Firestore rules.

# Expected response style

Respond with: brief diagnosis, change plan, affected files, code or diff summary, validation commands, tests added or recommended, risks, and next step. For review tasks, lead with findings ordered by severity.

# References

- ../../docs/audits/FUENTES_LOCALES.md
- references/firestore-data-modeling.md
- references/security-rules-patterns.md
- templates/firestore-rules-template.md
