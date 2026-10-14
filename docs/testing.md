# Testing

## Running the tests

JVM unit tests only, with no device or emulator needed:

```bash
./gradlew testDebugUnitTest
```

```bat
build.bat test        :: Windows
build.bat all         :: debug build + unit tests + lint
```

Reports: `app/build/reports/tests/testDebugUnitTest/index.html`.

Android lint (errors fail the build; no baseline, no disabled checks):

```bash
./gradlew lintDebug   # report: app/build/reports/lint-results-debug.html
```

Release-script tests (Python standard library only):

```bash
python3 -m unittest discover -s scripts/tests
```

Everything CI runs, in one go:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
python3 -m unittest discover -s scripts/tests
```

## What CI runs

`.github/workflows/ci.yml`, on every pull request and push to `main`:

| Check | Command | Catches |
|---|---|---|
| Release script tests | `python3 -m unittest discover -s scripts/tests` | Version maths, keystore-secret decoding |
| Unit tests | `./gradlew testDebugUnitTest` | Logic bugs in the JVM-testable classes |
| Lint | `./gradlew lintDebug` | API misuse, missing translations, accessibility, hard-coded text |
| Debug build | `./gradlew assembleDebug` | Compile errors; produces `snoozer-<version>-debug.apk` to test on the phone |
| Release build | `./gradlew assembleRelease` | Problems caused by R8 minification and resource shrinking |

Test and lint reports are attached to every run as the `reports` artifact, also when it fails. The release workflow runs the unit tests and lint again before it builds a signed APK.

## What is covered

| Test | Subject | Why it matters |
|---|---|---|
| `bedtime/BedtimeMonitorTest` | `BedtimeMonitor.adbCommands` | The Settings screen shows these commands; they must name the installed package (`.debug` for test builds) |
| `bedtime/BedtimeParserTest` | `BedtimeParser` + `ProtoReader` on hand-built protobuf dumps | The dump format is internal to Android; these pin the expected layout and the safety rules |
| `tasker/TaskerContractTest` | `TaskerContract.Command` ids | Ids live inside users' Tasker tasks; renaming one would silently break them |

`BedtimeParserTest` cases:

- Bedtime among active zen rules → `true`
- Only other active rules (e.g. Snoozer's own mode) → `false`
- No active rules, while Wellbeing's notification (a different top-level field) mentions `winddown` → `false`, so no false positive from the notification
- Dump without a zen section → `null` (unreadable)
- Plain text or garbage bytes → `null`

The tests build dumps with a small varint/length-delimited encoder inside the test file, so no protobuf dependency is needed.

`TaskerContractTest` checks that every command round-trips through its id, that the ids are exactly `snooze`, `unsnooze` and `toggle`, and that unknown, empty, null and wrong-case ids are rejected.

## Strategy

Logic that can run on the JVM is kept free of Android types and unit-tested:
- binary parsing;
- the plug-in contract.

Everything that depends on system services is verified manually on a device, because it can't be meaningfully faked: the notification listener, zen rules, tiles, and `dumpsys` permissions. The manual check is in [installation.md](installation.md#verify-it-works).

Not covered by automated tests:
- `SnoozeRepository` (DataStore transactions, including the Bedtime ownership rules in `onBedtimeReading`);
- the snooze/restore flow in `SnoozeListenerService`;
- the Compose UI. There are no instrumented (`androidTest`) or UI tests.

There is no coverage tool configured.

## Manual checks on the phone

For each pull request, install the debug APK from the CI run and follow the steps in the pull request description. Before a release, also check:

- [ ] Snooze and Unsnooze from the app, the Quick Settings tile and the launcher shortcut.
- [ ] Dark mode on and off: text stays readable.
- [ ] Language: Settings › System › Languages › App languages › Snoozer › Nederlands. All text is translated.
- [ ] Largest font size: nothing is cut off.
- [ ] After a release: update the app from the Play Store (internal testing). It updates and keeps the settings and notification access.

## Reading a crash from a release build

Release builds are minified, so stack traces show short names. Crashes reported through Google Play are decoded by Play (the workflow uploads the mapping file). For another trace, download `snoozer-<version>-mapping.txt` from the matching run of the Release workflow (**Actions › Release › Artifacts**, kept 90 days; older versions: the GitHub Release) and decode the trace with R8's `retrace` (Android SDK `cmdline-tools/latest/bin/retrace`):

```bash
retrace snoozer-<version>-mapping.txt stacktrace.txt
```
