# Project structure

```
snoozer/
├── .editorconfig                     ktlint/editor rules (4 spaces, 140 cols, ktlint_official)
├── .gitattributes                    *.bat → CRLF, gradlew → LF
├── .github/
│   ├── scripts/restore_keystore.py   Decodes the keystore secret tolerantly, explains failures
│   └── workflows/release.yml         Version → build APK → publish GitHub Release
├── PRIVACY.md                        Privacy policy
├── build.bat                         Windows helper: debug | install | test | lint | all | clean
├── build.gradle.kts                  Root build: plugin declarations, Kotlin Gradle plugin pin
├── settings.gradle.kts               Repositories, single :app module
├── gradle.properties                 JVM args, build cache, configuration cache
├── gradle/
│   ├── libs.versions.toml            Version catalog (all dependency versions)
│   └── wrapper/                      Gradle 9.8.1 wrapper
└── app/
    ├── build.gradle.kts              Android config, signing, versioning, dependencies
    ├── proguard-rules.pro            Empty by design (no reflection)
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── java/io/github/impca201/snoozer/
        │   │   ├── SnoozerApp.kt
        │   │   ├── bedtime/
        │   │   │   ├── BedtimeMonitor.kt      Runs dumpsys, publishes status
        │   │   │   ├── BedtimeParser.kt       Finds the Bedtime rule in the zen proto
        │   │   │   └── ProtoReader.kt         Minimal protobuf wire-format reader
        │   │   ├── data/
        │   │   │   └── SnoozeRepository.kt
        │   │   ├── service/
        │   │   │   ├── NotificationAccess.kt
        │   │   │   ├── SnoozeIndicator.kt     Persistent notification + UnsnoozeReceiver
        │   │   │   ├── SnoozeListenerService.kt
        │   │   │   ├── SnoozeMode.kt
        │   │   │   └── SnoozeToaster.kt
        │   │   ├── shortcut/
        │   │   │   └── ShortcutActivity.kt
        │   │   ├── tasker/
        │   │   │   ├── TaskerContract.kt
        │   │   │   ├── TaskerEditActivity.kt
        │   │   │   └── TaskerFireReceiver.kt
        │   │   ├── tile/
        │   │   │   └── SnoozeTileService.kt
        │   │   └── ui/
        │   │       ├── Destination.kt         Home | AccessGuide | Settings
        │   │       ├── MainActivity.kt        Intent → destination routing
        │   │       ├── SnoozerNavigation.kt   Back stack, permission requests, screen wiring
        │   │       ├── components/            Shared motion specs
        │   │       ├── guide/                 Notification access guide + shared guide widgets
        │   │       ├── home/                  Snooze toggle, glow, expressive shapes
        │   │       ├── settings/              Settings screen and its collapsible sections
        │   │       └── theme/                 Material 3 theme, dynamic colour
        │   └── res/
        │       ├── drawable/                  Vector icons (launcher, tile, settings)
        │       ├── mipmap-anydpi/             Adaptive launcher icons
        │       ├── values/ values-night/      Colours, themes, English strings
        │       ├── values-{nl,fr,de,es,it}/   Translations
        │       └── xml/                       Shortcuts, locales, backup exclusion rules
        └── test/java/io/github/impca201/snoozer/
            ├── bedtime/BedtimeParserTest.kt
            └── tasker/TaskerContractTest.kt
```

## Responsibilities and layers

The app has three layers, enforced by convention rather than modules:

| Layer | Packages | Rules |
|---|---|---|
| **State** | `data/` | Only place that reads or writes DataStore. Exposes `StateFlow`s and suspend operations. No Android UI or service code. |
| **Effects** | `service/`, `bedtime/`, `SnoozerApp.kt` | Talk to platform APIs (listener, notifications, zen rules, dumpsys). They react to state and never hold their own copy of the switch. |
| **Entry points** | `ui/`, `tile/`, `shortcut/`, `tasker/`, `UnsnoozeReceiver` | Translate user or automation input into repository calls. They must not snooze notifications directly. |

## What does NOT go where

- **`data/`:** no platform side effects. Snoozing belongs in the listener, not in the repository.
- **Entry points:** don't call `SnoozeListenerService` or `snoozeNotification`. Write the switch and let the listener converge.
- **`bedtime/`:** `BedtimeParser` and `ProtoReader` stay free of Android types so they remain JVM-testable. Process execution and permissions live in `BedtimeMonitor`.
- **`proguard-rules.pro`:** only add rules if reflection is introduced. Manifest entry points are kept automatically.
- **`ui/`:** no navigation library. New screens are added to the `Destination` enum and `SnoozerNavigation`.
