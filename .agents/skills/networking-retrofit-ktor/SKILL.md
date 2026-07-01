---
name: networking-retrofit-ktor
description: "Use for Retrofit, Ktor Client, OkHttp, interceptors, DTO mapping, error handling, retries, timeouts and repository-backed network integration."
---

# Purpose

Integrate remote APIs without coupling UI to transport details.

# When to use

- Adding Retrofit or Ktor client.
- Handling HTTP errors and retries.
- Mapping DTOs to domain models.
- Adding auth headers or interceptors.

# Inputs to inspect

- API contract and base URL handling.
- Existing network stack.
- Serialization library.
- Error and retry requirements.
- Security requirements for headers/logging.

# Required workflow

1. Reuse the existing network client if present.
2. Define DTOs and service/client methods.
3. Map DTOs to domain at the data boundary.
4. Model errors and retry policy.
5. Add tests with fakes or mock server when available.

# Rules

- Do not call network directly from UI.
- Do not log tokens or secrets.
- Avoid infinite retries.
- Keep DTOs out of domain unless intentionally shared.
- Respect coroutine cancellation.

# Files commonly touched

- `network module`
- `DTOs`
- `API service/client`
- `repository implementation`
- `interceptors`
- `tests`

# Commands to validate

```bash
./gradlew test
./gradlew assembleDebug
./gradlew lint
```

# Common mistakes to avoid

- Returning raw Response to UI.
- Logging Authorization headers.
- No timeout policy.
- Swallowing HTTP error details needed by UX.

# Checklist

- Client reused/configured.
- Errors modeled.
- DTO mapping tested.
- Timeouts/retries explicit.
- No secrets logged.

# Example prompts

- Use $super-android-kotlin-firebase to connect this screen to a REST API.
- Use $super-android-kotlin-firebase to migrate a repository from Retrofit to Ktor.

# Expected response style

Respond with: brief diagnosis, change plan, affected files, code or diff summary, validation commands, tests added or recommended, risks, and next step. For review tasks, lead with findings ordered by severity.

# References

- ../../docs/audits/FUENTES_LOCALES.md
- references/network-boundaries.md
- templates/network-repository-template.md
