# Testing Checklist

- Domain logic has unit tests.
- ViewModels cover loading, success, empty, error and retry.
- Coroutine and Flow tests use virtual time.
- Repositories use fakes or local test data sources.
- Compose UI tests cover critical flows.
- Firebase rules/functions use Emulator Suite when relevant.
