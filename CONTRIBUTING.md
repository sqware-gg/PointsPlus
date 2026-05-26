# Contributing

Use focused changes and keep behavior covered by either command testing or a build verification.

## Build

```powershell
.\mvnw.cmd package
```

## Style

- Match the existing package structure.
- Keep messages configurable.
- Keep user-facing commands permission-gated.
- Avoid changing persisted data formats without adding migration handling.
