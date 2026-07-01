# Security Checklist

- No service accounts, keystores, tokens, `.env` or credentials are committed.
- Logs, Analytics and Crashlytics avoid PII.
- Firebase rules follow least privilege.
- Android permissions are minimal and explained.
- Storage validates path, auth, content type and size.
- Scripts are reviewed before execution.
