# Nafs — Personal Life OS

A personal life operating system for Android: habits, long-term plans, and a
holistic life-status dashboard, built around the Islamic concept of
muhasaba (self-accountability). Offline-first, personal-first.

**Phase 1** (this code): life areas, habits, daily check-in, scoring engine,
habit detail with streaks and charts. Plans, dashboard, and cloud sync come in
Phases 2–4 (see the roadmap in the project brief).

## Stack

| Piece | Choice |
|---|---|
| UI | Jetpack Compose + Material 3 (no dynamic color — calm fixed palette) |
| Architecture | MVVM + Clean Architecture in a single `:app` module (packages: `domain` / `data` / `presentation`) |
| DB | Room (source of truth), schema exported to `app/schemas/` |
| DI | Hilt |
| Async | Coroutines + `StateFlow` |
| Navigation | Compose Navigation, type-safe `@Serializable` routes |
| Charts | Vico (`compose-m3`) |
| Build | AGP 9 (built-in Kotlin — do **not** apply `org.jetbrains.kotlin.android`), Gradle 9.4.1, version catalog |

## Domain rules (Phase 1)

- **Week runs Saturday → Friday** everywhere (quotas, trends, streaks).
- Frequencies: `Daily`, `Weekly(n)` = n times per week on any days,
  `SpecificDays` = fixed weekdays. Weekly habits stay on the Today list until
  the quota is met.
- **Scoring**: rolling 30-day window. Habit score = completed ÷ expected
  (weekly quotas pro-rated by active days and capped per week). Habits with
  nothing expected score `null` and are excluded rather than counted as zero.
  Habit scores roll up into area scores weighted by `targetScore`; area scores
  roll up into the overall status weighted by area `weight`.
- **Trend**: last 7 days vs the 7 before, ±3 points threshold.
- **Streaks**: daily/specific-day habits streak over scheduled days (an
  incomplete *today* is pending, not a break); weekly habits streak over weeks
  that met the quota.

All of this lives in pure Kotlin (`domain/scoring/`) and is covered by JVM
unit tests: `./gradlew :app:testDebugUnitTest`.

## Package map

```
com.nafs.app
├── di/                      Hilt modules (app, database, repositories)
├── domain/
│   ├── model/               LifeArea, Habit, HabitFrequency, HabitCheckIn, scoring models
│   ├── repository/          Repository interfaces
│   └── scoring/             HabitScheduler + ScoreEngine (pure Kotlin)
├── data/
│   ├── local/               Room database, entities, DAOs, converters
│   ├── mapper/              Entity ↔ domain mapping (incl. frequency encoding)
│   ├── repository/          Repository implementations
│   └── seed/                Six preset areas inserted on first launch
└── presentation/
    ├── theme/                Colors, Tajawal typography, shapes
    ├── common/               Icon registry, empty state, score ring, trend icon
    ├── navigation/           Routes + scaffold with bottom navigation
    ├── checkin/              Today screen (daily check-in)
    ├── areas/                Life areas list + editor sheet
    ├── habits/               Habits list + habit editor
    └── habitdetail/          Detail: streaks, 14-day strip, weekly Vico chart, notes
```

## Conventions

- Strings live in `values/strings.xml` (English) and `values-ar/strings.xml`
  (Arabic); RTL is fully supported (`AutoMirrored` icons, start/end padding).
- UI state is modeled as sealed interfaces (`Loading` / `Content` / …) exposed
  as `StateFlow` from ViewModels.
- No hardcoded colors in screens — everything through `MaterialTheme`. The
  only exception is user-chosen area colors, which are data, not theme.
- `java.time.Clock` is injected so "today" is controllable in tests.
- Room schema JSON is versioned in `app/schemas/` — later phases add tables
  via migrations, never destructive recreation.
