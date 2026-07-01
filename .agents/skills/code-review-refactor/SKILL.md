---
name: code-review-refactor
description: "Use for Android code review, architecture review, UI/security/performance/testing review and safe step-by-step refactoring without giant changes."
---

# Purpose

Provide senior review findings and turn risky refactors into small verifiable steps.

# When to use

- Reviewing a diff or project.
- Planning a refactor.
- Finding architecture, security, testing or performance risks.
- Reducing technical debt safely.

# Inputs to inspect

- Diff, files or module tree.
- User goal and risk tolerance.
- Existing tests and failing commands.
- Architecture constraints.

# Required workflow

1. Read relevant files before judging.
2. Lead with findings ordered by severity.
3. Separate bugs from preferences.
4. Propose the smallest safe refactor.
5. Add tests/validation before broad cleanup.

# Rules

- Findings first for reviews.
- Do not do cosmetic rewrites unless requested.
- Do not revert unrelated user changes.
- Preserve behavior unless asked to change it.
- Mention test gaps honestly.

# Files commonly touched

- `Any changed files`
- `README/AGENTS`
- `build files`
- `tests`
- `rules files`

# Commands to validate

```bash
./gradlew test
./gradlew lint
./gradlew assembleDebug
node scripts/validate-skills.mjs
```

# Common mistakes to avoid

- Large unrelated refactors.
- No file/line evidence.
- Ignoring security or test gaps.
- Mixing personal style with correctness.

# Checklist

- Findings prioritized.
- Evidence included.
- Safe plan given.
- Tests/commands included.
- Residual risks named.

# Example prompts

- Use $super-android-kotlin-firebase to review this Android project.
- Use $super-android-kotlin-firebase to refactor this repository layer safely.

# Expected response style

Respond with: brief diagnosis, change plan, affected files, code or diff summary, validation commands, tests added or recommended, risks, and next step. For review tasks, lead with findings ordered by severity.

# References

- ../../docs/audits/FUENTES_LOCALES.md
- references/review-refactor-playbook.md
- templates/review-report-template.md
