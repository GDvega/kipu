---
name: dependency-upgrade-safety
description: "Use for safe Android dependency upgrades across Gradle, AGP, Kotlin, Compose, Firebase BoM, Hilt, Room, Retrofit and CI."
---

# Purpose

Upgrade dependencies with compatibility checks, grouped changes, validation commands and rollback plans.

# When to use

- Updating dependencies in `libs.versions.toml`.
- Upgrading Compose, Firebase, Hilt, Room, Retrofit or Kotlin.
- Reviewing a dependency update PR.
- Creating a rollback-safe upgrade plan.

# Inputs to inspect

- `gradle/libs.versions.toml`
- Root and module `build.gradle.kts`
- `gradle-wrapper.properties`
- Release notes for changed libraries
- CI workflow and test coverage

# Required workflow

1. Inventory current dependency families.
2. Group upgrades by risk and compatibility.
3. Upgrade one group at a time.
4. Run targeted build/tests after each group.
5. Write rollback plan and known risks.

# Rules

- Do not update 20 unrelated libraries together.
- Group Gradle/AGP/Kotlin separately from Compose and Firebase.
- Do not jump to prerelease versions unless requested.
- Preserve BoM usage where appropriate.
- Document rollback commands.

# Files commonly touched

- `gradle/libs.versions.toml`
- `build.gradle.kts`
- `gradle-wrapper.properties`
- `.github/workflows/*`
- Test files affected by API changes

# Commands to validate

```bash
./gradlew --version
./gradlew assembleDebug
./gradlew test
./gradlew lint
```

# Common mistakes to avoid

- Mixing tooling and app library upgrades in one commit.
- Ignoring release notes for breaking changes.
- Removing BoM alignment accidentally.
- Skipping CI after local success.

# Checklist

- Dependencies grouped.
- Compatibility checked.
- Rollback plan written.
- Tests run per group.
- Risk summary included.

# Example prompts

- Use $super-android-kotlin-firebase to plan safe dependency upgrades.
- Use $super-android-kotlin-firebase to review this libs.versions.toml update.

# Expected response style

Respond with: brief diagnosis, change plan, affected files, code or diff summary, validation commands, tests added or recommended, risks, and next step. For review tasks, lead with findings ordered by severity.

# References

- ../../docs/audits/FUENTES_LOCALES.md
- references/android-dependency-upgrade-strategy.md
- references/compose-bom-upgrade.md
- references/firebase-bom-upgrade.md
- references/compatibility-matrix-template.md
- templates/dependency-upgrade-plan-template.md
- templates/libs-versions-toml-review-template.md
- templates/rollback-plan-template.md
