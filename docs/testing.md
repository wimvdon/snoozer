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

Android lint:

```bash
./gradlew lintDebug   # report: app/build/reports/lint-results-debug.html
```

The release workflow does not run tests or lint. Run them locally before tagging a release.

## What is covered

| Test | Subject | Why it matters |
|---|---|---|
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
