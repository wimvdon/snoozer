# Project structure

```
snoozer/
├── .editorconfig                     ktlint/editor rules (4 spaces, 140 cols, ktlint_official)
├── .gitattributes                    LF everywhere; *.bat → CRLF; keystores/images binary
├── .github/
│   ├── dependabot.yml                Weekly Gradle and GitHub Actions update PRs
│   ├── pull_request_template.md      Plain-language PR description + checklist
│   └── workflows/
│       ├── ci.yml                    Script tests, unit tests, lint, debug + release builds, debug APK artifact
│       └── release.yml               Release rules → verify signing key → build APK → publish GitHub Release
├── CLAUDE.md                         Rules for Claude Code sessions (workflow, never-dos, code rules)
├── DESIGN.md                         Visual design: colour, shape, motion
├── LICENSE                           Copyright notice: all rights reserved, no open-source licence
├── PRIVACY.md                        Privacy policy
├── scripts/                          Release helpers, shared with the android-app template
│   ├── release_version.py            Tag parsing, next version, versionCode, "is newer"
│   ├── decode_keystore.py            Decodes the keystore secret tolerantly, explains failures
│   ├── VerifySigningKey.java         Checks the key password before the build
│   └── tests/                        unittest suites for both Python scripts
├── build.bat                         Windows helper: debug | install | test | lint | all | clean
├── build.gradle.kts                  Root build: plugin declarations, Kotlin Gradle plugin pin
├── settings.gradle.kts               Repositories, single :app module
├── gradle.properties                 JVM args, build cache, configuration cache
├── gradle/
│   ├── libs.versions.toml            Version catalog (all dependency versions)
│   └── wrapper/                      Gradle 9.8.1 wrapper
└── app/
    ├── build.gradle.kts              App ID, versioning, signing, lint, dependencies, printApkBaseName
    ├── proguard-rules.pro            Empty by design (no reflection)
    └── src/
        ├── debug/res/                    Debug build only: "Snoozer Dev" name, shortcut for the .debug ID
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── kotlin/io/github/impca201/snoozer/
        │   │   ├── SnoozerApp.kt
        │   │   ├── bedtime/
        │   │   │   ├── BedtimeMonitor.kt      Runs dumpsys, publishes status
        │   │   │   ├── BedtimeParser.kt       Finds the Bedtime rule in the zen proto
        │   │   │   └── ProtoReader.kt         Minimal protobuf wire-format reader
        │   │   ├── data/
        │   │   │   ├── Appearance.kt          Theme choice (system / light / dark)
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
        │   │       ├── guide/                 Notification access guide + shared guide widgets
        │   │       ├── home/                  Snooze toggle, glow, expressive shapes
        │   │       ├── settings/              Settings screen and its collapsible sections
        │   │       └── theme/                 Material 3 theme, dynamic colour, shapes, motion, settings kit
        │   └── res/
        │       ├── drawable/                  Vector icons (launcher, tile, settings)
        │       ├── mipmap-anydpi/             Adaptive launcher icons
        │       ├── values/ values-night/      Colours, themes, English strings
        │       ├── values-{nl,fr,de,es,it}/   Translations
        │       └── xml/                       Shortcuts, backup rules (the locale list is generated)
        └── test/kotlin/io/github/impca201/snoozer/
            ├── bedtime/BedtimeMonitorTest.kt
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
- **`scripts/`:** release tooling only, never app code. Every change runs `python3 -m unittest discover -s scripts/tests`.
- **`src/debug/`:** only what must differ for the `.debug` application ID. Keep it in sync with `src/main/`.
- **`ui/`:** no navigation library. New screens are added to the `Destination` enum and `SnoozerNavigation`.
