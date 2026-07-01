---
name: firestore-costs-quotas
description: "Use for Firestore data modeling, query design, listener usage, pagination, indexes, quotas and cost-aware architecture reviews."
---

# Purpose

Design Firestore access patterns that are secure, queryable and cost-aware before the app scales.

# When to use

- Designing collections and documents.
- Optimizing Firestore queries or listeners.
- Estimating Firestore cost risk.
- Reviewing indexes, pagination or real-time updates.

# Inputs to inspect

- Data model diagrams or repository code
- Collections, documents and subcollections
- Queries, listeners and pagination
- Indexes and Security Rules
- Offline persistence and sync behavior

# Required workflow

1. Start from user journeys and query requirements.
2. Model documents around reads and authorization boundaries.
3. Estimate read/write/listener behavior.
4. Design pagination and indexes from real queries.
5. Name cost, quota and security risks.

# Rules

- Do not store images or base64 blobs in Firestore.
- Do not read full collections when pagination works.
- Do not attach broad global listeners by default.
- Avoid huge arrays for complex relationships.
- Design rules and indexes with the data model.

# Files commonly touched

- Repository Firestore code
- `firestore.rules`
- `firestore.indexes.json`
- DTO/domain models
- Sync managers
- Pagination UI state

# Commands to validate

```bash
./gradlew test
firebase emulators:exec --only firestore "npm test"
```

# Common mistakes to avoid

- Counting writes but ignoring listener re-reads.
- Using document IDs that leak sensitive relationships.
- Designing indexes after shipping slow queries.
- Ignoring offline cache invalidation.

# Checklist

- Queries listed.
- Indexes considered.
- Pagination planned.
- Listener scope bounded.
- Cost and quota risks documented.

# Example prompts

- Use $super-android-kotlin-firebase to review this Firestore model for cost risks.
- Use $super-android-kotlin-firebase to design paginated Firestore queries.

# Expected response style

Respond with: brief diagnosis, change plan, affected files, code or diff summary, validation commands, tests added or recommended, risks, and next step. For review tasks, lead with findings ordered by severity.

# References

- ../../docs/audits/FUENTES_LOCALES.md
- references/firestore-cost-model.md
- references/query-and-index-design.md
- references/listeners-and-pagination.md
- references/anti-patterns.md
- templates/firestore-data-model-review-template.md
- templates/pagination-query-template.md
- templates/cost-risk-report-template.md
