# Snoozer

## 📚 Documentation
| | |
|---|---|
| [⚙️ Architecture](docs/architecture.md) | System design, components and flow |
| [📁 Structure](docs/structure.md) | Project organization and responsibilities |
| [🚀 Installation](docs/installation.md) | Requirements and steps to run the project |
| [🧠 Technical decisions](docs/decisions.md) | Trade-offs and design justifications |
| [📖 Usage guide](docs/usage.md) | How to use the app, settings and automation |
| [🔌 API](docs/api.md) | Tasker plug-in, launcher shortcut and other entry points |
| [🧪 Testing](docs/testing.md) | How to run tests and what they cover |

---

## Description

Snoozer is an Android app that snoozes every notification on the phone with one tap and brings them all back with another.

- **What it does:** while snoozing is on, every clearable notification, including new ones, is snoozed through Android's notification listener API. Turning it off restores all of them.
- **What problem it solves:** Do Not Disturb only silences notifications; they still pile up in the shade. Snoozer clears the shade without dismissing anything, and calls, alarms, media controls, navigation and other non-dismissible notifications stay untouched.
- **Real use case:** focus time, meetings or bedtime. Toggle from the app, a Quick Settings tile, a launcher shortcut, Pixel Quick Tap, or automatically via Tasker or Pixel Bedtime mode.

No internet permission, no analytics ([privacy policy](PRIVACY.md)). Android 8.0+, in English, Dutch, French, German, Spanish and Italian.

Source available for reference; all rights reserved. See [LICENSE](LICENSE).

## Quick start

Install the latest `snoozer-x.y.z.apk` from [Releases](https://github.com/impca201/snoozer/releases), open it, tap **Enable notification access** and tap **Snooze**.

To build and install a debug build from source on a connected phone:

```bash
git clone https://github.com/impca201/snoozer.git
cd snoozer
./gradlew installDebug        # Windows: build.bat install
```

## Technologies used

| Area | Technology |
|---|---|
| Language | Kotlin 2.4, coroutines and `StateFlow` |
| UI | Jetpack Compose, Material 3 (dynamic colour), `androidx.graphics:graphics-shapes` |
| Storage | Jetpack DataStore (Preferences) |
| Platform APIs | `NotificationListenerService`, `TileService`, `AutomaticZenRule` (modes), app shortcuts, Locale/Tasker plug-in intents |
| Build | Gradle 9.8 (Kotlin DSL, version catalog), AGP 9.4, R8 |
| CI/CD | GitHub Actions: tests, lint and an installable debug APK on every pull request; versioned, signed release APKs; Dependabot |
| Tests | JUnit 4 (JVM unit tests) |

## Quick installation

1. Install [Android Studio](https://developer.android.com/studio) (brings JDK and Android SDK).
2. Clone the repo and run `./gradlew assembleDebug` (or `build.bat` on Windows).
3. Install `app/build/outputs/apk/debug/app-debug.apk` and grant notification access.

Full steps: [docs/installation.md](docs/installation.md).

## Architecture (summary)

A single DataStore switch is the source of truth. Every control surface (app, tile, shortcut, Tasker, Bedtime sync) only writes that switch; the always-bound `NotificationListenerService` observes it and does the actual snoozing and restoring. The application class fans state changes out to the tile, the optional persistent notification and the optional Android 15 mode. Details: [docs/architecture.md](docs/architecture.md).

## Project structure

```
snoozer/
├── .github/            CI and release workflows, PR template, Dependabot
├── CLAUDE.md           Rules for Claude Code sessions
├── scripts/            Release helpers (versioning, keystore secret) with tests
├── app/src/main/kotlin/io/github/impca201/snoozer/
│   ├── SnoozerApp.kt   Application: app scope, repository, cross-surface sync
│   ├── data/           DataStore repository (switch, settings, snoozed keys)
│   ├── service/        Notification listener, persistent notification, mode, toasts
│   ├── bedtime/        Follow Bedtime: dumpsys reader and protobuf parser
│   ├── tile/           Quick Settings tile
│   ├── shortcut/       Launcher shortcut trampoline
│   ├── tasker/         Tasker/Locale plug-in
│   └── ui/             Compose screens (home, access guide, settings)
├── app/src/debug/      Debug-only resources (shortcut for the .debug app ID)
├── app/src/test/       JVM unit tests
└── build.bat           Windows helper for local debug builds
```

Full tree: [docs/structure.md](docs/structure.md).
