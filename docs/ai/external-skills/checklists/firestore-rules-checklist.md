# Firestore Rules Checklist

- Auth is required for private data.
- Owner and role checks use trusted auth context.
- Creates validate required fields and types.
- Updates prevent privilege escalation and immutable owner changes.
- Deletes are restricted or audited.
- Queries are compatible with rules and indexes.
- Emulator allow/deny tests cover owner, wrong user and unauthenticated user.
