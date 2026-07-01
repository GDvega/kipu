# Design Test Gate Notes

Use this reference when a design request arrives before the project health is known.

## Gate order

1. Identify the target module and UI surface.
2. Run the smallest build command that proves the app still compiles.
3. Run the lowest useful tests for the affected layer.
4. Run UI or instrumentation tests only when the request touches navigation, semantics, device behavior or visual regressions.
5. Continue to design only when failures are understood or absent.

## Stop conditions

- Gradle sync or build failure.
- Unit test failure in code related to the target flow.
- UI test failure that may be caused by semantics, navigation or state changes.
- Missing environment that prevents meaningful validation.

## Output expectation

The agent should say whether the gate passed, failed or was blocked. A failed gate should produce a debugging plan, not a redesign.
