# UI State Matrix Notes

Use a state matrix when a screen has more than one meaningful condition.

## Common states

- Loading.
- Empty.
- Content.
- Error.
- Offline.
- Permission denied.
- Auth required.
- Syncing.
- Stale data.

## State quality checks

- The user knows what happened.
- The user knows what they can do next.
- Sensitive data is not overexposed.
- The state can be previewed.
- Critical transitions can be tested.

## Contract rule

UI state design should reflect the ViewModel and data layer. If a needed state cannot be represented, report the missing contract before changing UI.
