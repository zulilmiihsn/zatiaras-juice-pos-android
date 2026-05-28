# ZatiarasPOS Android

Production Android POS app for Zatiaras Juice, built with Kotlin, Jetpack Compose, Hilt, Room, WorkManager, Supabase, and modular feature modules.

## Build

```powershell
.\gradlew.bat test
.\gradlew.bat lintDebug
.\gradlew.bat assembleRelease
```

## Production Checks

```powershell
.\gradlew.bat spotlessCheck
.\gradlew.bat detekt
.\gradlew.bat koverXmlReport
```

## Engineering Guides

- `docs/rules.md` defines architecture and coding rules.
- `docs/maintainability.md` defines readability ratchets for humans and AI agents.

## Local Configuration

`local.properties` is ignored by git and should provide local-only values:

```properties
sdk.dir=D:\\kulyeah\\project\\zatiarasposapp\\.tools\\android-sdk
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-public-anon-key
SENTRY_DSN=your-public-sentry-dsn
SENTRY_ENVIRONMENT=local
```

Never put Supabase service-role keys, Sentry auth tokens, keystores, or signing passwords in the repository. CI values must come from repository secrets.
