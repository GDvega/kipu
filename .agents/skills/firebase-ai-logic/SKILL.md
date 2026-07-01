---
name: firebase-ai-logic
description: "Use for Firebase AI Logic and Gemini integration in Android, safe prompts, privacy boundaries, model selection, fallback behavior and not exposing private keys."
---

# Purpose

Add Gemini/AI features to Android apps safely, with clear privacy and fallback behavior.

# When to use

- Adding Firebase AI Logic or Gemini features.
- Designing prompts or multimodal input.
- Choosing cloud vs on-device AI.
- Handling model errors, limits and fallback UX.

# Inputs to inspect

- AI use case and user value.
- Data sent to model and privacy classification.
- Firebase/Google AI provider choice.
- Fallback and human review requirements.

# Required workflow

1. Classify data and decide if AI is appropriate.
2. Choose Firebase AI Logic, backend proxy or on-device path.
3. Design prompts and output validation.
4. Handle rate limits, safety and offline fallback.
5. Add fakes/tests for AI boundaries.

# Rules

- Do not expose private API keys in the client.
- Minimize sensitive data sent to models.
- Do not rely on AI for critical irreversible decisions without review.
- Validate structured outputs.
- Provide deterministic fallback.

# Files commonly touched

- `AI repository/service`
- `prompt templates`
- `ViewModel state`
- `Firebase setup`
- `tests/fakes`

# Commands to validate

```bash
./gradlew test
./gradlew assembleDebug
firebase emulators:start
```

# Common mistakes to avoid

- Hardcoding secret keys.
- Sending unnecessary PII.
- Trusting AI output without validation.
- No fallback or error UX.

# Checklist

- Privacy reviewed.
- Provider chosen.
- Prompt/output validated.
- Fallback exists.
- Tests/fakes included.

# Example prompts

- Use $super-android-kotlin-firebase to add Gemini summaries for user notes.
- Use $super-android-kotlin-firebase to review this AI feature for privacy.

# Expected response style

Respond with: brief diagnosis, change plan, affected files, code or diff summary, validation commands, tests added or recommended, risks, and next step. For review tasks, lead with findings ordered by severity.

# References

- ../../docs/audits/FUENTES_LOCALES.md
- references/firebase-ai-logic-android.md
- templates/ai-feature-boundary-template.md
