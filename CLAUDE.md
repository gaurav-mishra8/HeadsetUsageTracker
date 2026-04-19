# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Commands

Run from the repo root:

```bash
./gradlew :app:assembleDebug          # Debug APK
./gradlew :app:installDebug           # Install on connected device/emulator
./gradlew testDebugUnitTest           # Unit tests (JVM)
./gradlew :app:connectedAndroidTest   # Instrumentation tests (requires device/emulator)
./gradlew ktlintCheck                 # Kotlin linting
./gradlew detekt                      # Static analysis (baseline exists for legacy issues)
./gradlew lintDebug                   # Android lint
```

## Tech Stack

- **Language:** Kotlin 1.9.22, JVM 17
- **Min SDK:** 26 (Android 8.0), Compile SDK: 36
- **UI:** Material Design 3, View Binding, MPAndroidChart (pie/bar charts), Fragments + Bottom Navigation
- **DI:** Hilt 2.46.1 (`@HiltAndroidApp`, `@AndroidEntryPoint`, `@Inject`)
- **DB:** Room 2.6.1 — single entity `HeadphoneUsage`, database version 2 with migration
- **Async:** Kotlin Coroutines + `lifecycleScope`, `withContext(Dispatchers.IO)` for DB ops
- **Background:** `LifecycleService` (tracking), WorkManager (periodic Drive sync)
- **Sync:** Google Drive API v3 writing to `appDataFolder`

## Architecture

**MVVM-lite + Repository pattern.** No dedicated ViewModels — Fragments query the Dao directly via `lifecycleScope`.

**Main components:**

| Component | Role |
|-----------|------|
| `HeadphoneTrackingService` | Foreground service; monitors headphone state via `AudioManager`, tracks active app via `UsageStatsManager`, writes sessions to DB every 5 s |
| `MainActivity` | Hosts 3 fragments via Bottom Navigation; handles onboarding redirect, permission checks, auto-prune (>3 months) |
| `TodayFragment` | Today's stats with pie/bar charts; observes `isTrackingFlow` StateFlow from the service |
| `StatsFragment` | Historical stats (7-day bar chart, date-range view) |
| `SettingsFragment` | Drive sync toggle, excluded apps, manual backup/restore |
| `AppDatabase` | Room singleton; single table `headphone_usage` with indexes on `date` and `(date, packageName)` |
| `SettingsRepository` | Wraps SharedPreferences; ~65 preference keys |
| `DriveSyncWorker` | `CoroutineWorker` — serializes all DB rows to JSON, uploads via `DriveSyncManager` |

**Data flow for tracking:**
```
HeadphoneTrackingService
  → inserts HeadphoneUsage rows via injected Dao
  → emits isTrackingFlow (StateFlow)
  → broadcasts widget update Intent

TodayFragment
  → observes isTrackingFlow
  → collects getUsageByDate() Flow for real-time list/chart updates
```

**Hilt wiring:** `DatabaseModule` provides `@Singleton AppDatabase` and `HeadphoneUsageDao`; `PreferencesModule` provides `@Singleton SharedPreferences`. All Activities, Fragments, and the Service are `@AndroidEntryPoint`.

## Key Files

- `HeadphoneTrackingService.kt` — ~700 lines; core tracking logic including wakelock, session management, notification updates
- `TodayFragment.kt` — ~700 lines; chart rendering, usage list, start/stop controls
- `data/HeadphoneUsageDao.kt` — all SQL queries (grouped by app, by day, date ranges)
- `data/SettingsRepository.kt` — all preference keys in one place
- `sync/DriveSyncManager.kt` — Drive auth + upload/download logic

## Testing

Unit tests use an in-memory Room database via `TestDatabaseModule` (replaces the real `DatabaseModule`). Mockito + MockitoKotlin for mocking; `InstantTaskExecutorRule` for LiveData. Instrumentation tests under `androidTest/`.
