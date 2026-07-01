# Design System Scope Notes

A contextual design system should grow from repeated product needs, not from a desire to abstract everything.

## Good candidates

- Buttons, cards, list items and form fields repeated across flows.
- Components with accessibility or security-sensitive behavior.
- Components that need consistent loading, empty, error or disabled states.
- Components whose visual behavior must be tested or previewed.

## Poor candidates

- One-off screen layout.
- Components that only wrap Material 3 without adding product meaning.
- Experimental style APIs adopted without explicit risk acceptance.

## Preservation rule

Reusable components should receive state and callbacks. ViewModels, repositories and Firebase SDK calls should stay outside leaf UI.
