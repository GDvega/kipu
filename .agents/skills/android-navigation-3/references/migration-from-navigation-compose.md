# Migration From Navigation Compose

1. Inventory current destinations, arguments and deep links.
2. Add the new navigation dependency in a separate commit.
3. Migrate one feature route at a time.
4. Preserve old route strings until incoming links are verified.
5. Add regression tests for back behavior and process recreation.
