# Snoozer

Snooze **all** notifications with one tap — indefinitely, until you turn it off again.

Snoozer is a small Android app built with Kotlin, Jetpack Compose and Material 3. While snoozing
is on, every notification that arrives (and every one already in your shade) is quietly snoozed.
Turn it off and they all come back.

## Features

- **One big toggle** on the home screen, with a calm ripple animation while snoozing is active.
- **Quick Settings tile** — snooze from anywhere; long-press the tile to open the app.
  On Android 13+ the in-app guide can add the tile for you.
- **Launcher shortcut** — long-press the app icon and tap *Snooze on/off* (can be pinned).
- **Tasker plug-in** — actions *Snooze*, *Unsnooze* and *Toggle*. Works with any app that
  supports the Tasker/Locale plug-in API (MacroDroid, Automate, …).
- **Optional toast** — "Notification from {app} snoozed" (Settings ⚙ → *Enable toast notification*).
  Bursts are merged into one "N notifications snoozed" toast.
- **Guided setup** — a banner and step-by-step guide (with a deep link) for granting notification
  access, including the *Allow restricted settings* step for sideloaded installs.
- Material You dynamic color, dark theme, edge-to-edge, predictive back, TalkBack labels,
  respects "Remove animations".
- Translated into English, Dutch, French, German, Spanish and Italian, with per-app language
  support on Android 13+.

## How it works

Snoozer is a `NotificationListenerService`. Android has no "snooze forever" and no public
"unsnooze" API, so:

- notifications are snoozed with `snoozeNotification(key, 7 days)` and snoozed again if one ever
  comes back while snoozing is still on;
- the keys of snoozed notifications are stored, and on *Unsnooze* each one is re-snoozed for one
  second, which makes the system repost it almost immediately.

Ongoing / non-dismissible notifications (calls, media controls, navigation, alarms, foreground
services) are never snoozed.

All state lives in a Jetpack DataStore. The home screen, tile, shortcut and Tasker plug-in all
write to it; the listener service observes it, so every entry point behaves the same.

## Project layout

```
app/src/main/java/io/github/impca201/snoozer/
├── SnoozerApp.kt              Application: app scope, keeps tile + listener in sync
├── data/SnoozeRepository.kt   DataStore: snooze switch, toast setting, snoozed keys
├── service/                   NotificationListenerService, toast helper, access helpers
├── tile/                      Quick Settings TileService
├── shortcut/                  Invisible activity behind the launcher shortcut
├── tasker/                    Locale/Tasker plug-in (edit activity + fire receiver)
└── ui/                        Compose UI: home, guides, theme
```

## Requirements

| | |
|---|---|
| Min SDK | 26 (Android 8.0) |
| Target / compile SDK | 37 (Android 17) |
| Build | AGP 9.4, Gradle 9.8, Kotlin 2.4 (AGP built-in Kotlin), JDK 17+ |
| UI | Jetpack Compose (BOM 2026.09.00), Material 3 |

## Building

### Windows

```bat
build.bat            :: debug APK  -> app\build\outputs\apk\debug\
build.bat install    :: build and install on a connected device
build.bat bundle     :: release .aab for Google Play
build.bat all        :: everything CI runs (build, tests, lint)
```

`build.bat` finds Java (`JAVA_HOME` or Android Studio's bundled JBR) and the Android SDK
(`ANDROID_HOME` or `%LOCALAPPDATA%\Android\Sdk`) and creates `local.properties` if needed.

### macOS / Linux

```bash
./gradlew assembleDebug          # debug APK
./gradlew testDebugUnitTest      # unit tests
./gradlew lintDebug              # Android lint
./gradlew bundleRelease          # Play Store bundle (needs signing, see below)
```

CI (`.github/workflows/android.yml`) builds, tests and lints every push and uploads the debug APK.

### Release signing

Create an untracked `keystore.properties` in the project root:

```properties
storeFile=/absolute/path/to/release.jks
storePassword=…
keyAlias=…
keyPassword=…
```

Without it, `assembleRelease` produces an unsigned APK.

## Privacy

Snoozer has **no internet permission**, no analytics and no ads. It never reads, stores or
shares notification content. See [PRIVACY.md](PRIVACY.md).
