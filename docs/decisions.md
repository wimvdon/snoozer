# Technical decisions

## 2026-10-14: Aligned with the android-app template

Snoozer now follows the same rules, layout and workflows as [impca201/android-app](https://github.com/impca201/android-app), so every app is built, checked and released the same way.

- **Rules for Claude Code sessions** in `CLAUDE.md`; a plain-language pull request template; weekly Dependabot updates for Gradle and GitHub Actions.
- **CI** now also runs lint (errors fail the build) and the release-script tests, cancels outdated runs, and uploads `snoozer-<version>-debug.apk` unzipped so it installs straight from the download.
- **Release workflow** from the template: version maths in `scripts/release_version.py` (unit tested), the signing key checked before the build (`scripts/VerifySigningKey.java`), tests and lint before a signed build, and the R8 mapping file attached to each release. It publishes with `gh release create` instead of a third-party action, one fewer dependency with write access. The old secret names without `ANDROID_` keep working.
- **Debug builds get `applicationIdSuffix = ".debug"`**, so a pull request's APK installs next to the released app. Two things name the package and follow it: the static launcher shortcut (a copy in `src/debug/res/xml/`, because `shortcuts.xml` cannot use placeholders) and the Follow Bedtime adb commands (now built from the installed package name).
- **Sources moved from `src/*/java/` to `src/*/kotlin/`**, the template's layout. No code changed.
- **Kept on purpose, unlike the template:** `minSdk` 26 (Android 8.0) instead of 31. Snoozer is released and supports Android 8.0+; raising it would stop updates for users on older phones. Also kept: the ktlint settings in `.editorconfig` (140 columns), `build.bat`, six languages instead of two, and the existing `docs/` set.


## Snooze instead of cancel or Do Not Disturb

**Decision:** use `NotificationListenerService.snoozeNotification` rather than `cancelNotification` or an interruption filter.

- Cancelling is destructive: the notification and its actions are gone for good.
- Do Not Disturb only suppresses sound and peeking; the shade still fills up.
- Snoozing hides the notification and lets the system repost it intact, with its original actions and content.

**Trade-off:** the platform has no "snooze until further notice" and no public unsnooze. Snoozer works around both:
- It snoozes for 7 days and re-snoozes anything that resurfaces.
- It "unsnoozes" by re-snoozing each key for 1 second.

This forces it to persist the keys it snoozed (`snoozed_keys`) so a restore still works after process death.

## Never snooze non-clearable notifications

`shouldSnooze` skips anything with `isClearable == false`: calls, alarms, media, navigation and foreground services. Hiding those can hide critical controls, and the system often refuses anyway. Snoozer's own notifications are skipped so the indicator can't hide itself.

## One switch, many writers

Every surface (app, tile, shortcut, Tasker, indicator action, Bedtime) writes a single DataStore boolean. Only the listener acts on it.

- **Discarded alternative:** having the tile, shortcut or receiver call into the listener directly. That would fail whenever the listener is unbound, and it would duplicate the snooze/restore logic per surface.
- **Benefit:** surfaces work without the UI, and conflicting inputs resolve to the last write.

## DataStore, not Room or SharedPreferences

The state is a handful of flags and one string set. DataStore gives atomic `edit` transactions (needed for toggle and the Bedtime ownership rules) and a `Flow` API without a database schema. SharedPreferences lacks safe read-modify-write, and Room would be oversized.

## No foreground service

The listener service is kept alive by the system as long as notification access is granted, so Snoozer needs no background service of its own.

- The persistent indicator is a plain ongoing notification. On Android 14+ users can swipe it away; the listener reposts it after 400 ms while snoozing.
- The tile is declared `TOGGLEABLE_TILE` but deliberately **not** `ACTIVE_TILE`. The system then queries fresh state each time Quick Settings opens, so no service has to push updates.

## Tasker plug-in instead of built-in schedules

Snoozer has no triggers, schedules or location rules. It exposes Snooze, Unsnooze and Toggle through the Locale plug-in contract, which Tasker, MacroDroid and Automate all support. This avoids background work and permissions in Snoozer, and leaves scheduling to apps built for it.

Command ids (`snooze`, `unsnooze`, `toggle`) are stored inside users' Tasker tasks and must never change. `TaskerContractTest` enforces this.

## Optional system mode, configured by the user

On Android 15+ Snoozer can own one `AutomaticZenRule` and activate it while snoozing.

- It is created with Android's defaults and no interruption filter or device effects of its own. Users choose effects (dim wallpaper, dark theme, who may interrupt) in **Settings › Modes**, so Snoozer never fights their configuration.
- It is created with `setManualInvocationAllowed(false)` so Snoozer stays the only thing that turns it on.
- Only the name is managed from the app.
- The condition id `snoozer://mode/dim` and the preference key `dim_enabled` are leftovers from an earlier "dim" feature. They are kept so existing rules and settings keep working.
- State is written only when it changes. Every write alters the zen configuration and wakes automation apps that watch it.

## Follow Bedtime via `dumpsys --proto`

Android exposes no API for "which mode is active" to third-party apps. The only reliable source found is the notification service dump. Reading it needs `DUMP` and `PACKAGE_USAGE_STATS`, which only adb can grant.

- **Parsing:** a tiny hand-written protobuf reader (`ProtoReader`) instead of the protobuf library or generated classes. Only two field numbers are needed, and it keeps the APK small.
- **Detection:** the parser only looks inside `ZenModeProto.enabled_active_conditions` for the `winddown` marker. Notifications live in a different top-level field, so Bedtime's own "Bedtime is on" notification can't cause a false positive. This is covered by tests.
- **Unexpected layouts:** reported as **Unreadable** rather than guessed. The feature is marked experimental and only tested on Pixels.
- **Change detection:** event-driven (policy broadcasts plus `SCREEN_ON`) with a double reading, instead of polling.
- **Ownership:** a snooze started by Bedtime is only undone by Bedtime ending, and any manual change takes ownership away from it.

## Privacy by construction

There is no `INTERNET` permission, no analytics SDK, and backups and device transfers are fully excluded. `dependenciesInfo.includeInApk = false` drops the Google Play dependency blob. Notification content is never read; only the posting app's label is read, for the optional toast.

## Release signing only in CI

Signed APKs come only from GitHub Actions:

- **Signing key:** `keystore.properties` is written at build time from secrets and never committed.
- **Versioning:** versions come from tags (`major × 10000 + minor × 100 + patch`), so version codes always increase with semver.
- **Secret decoding:** `scripts/decode_keystore.py` tolerates common copy-paste damage (line breaks, quotes, URL-safe alphabet, double encoding) and reports what's wrong without printing the secret, because a broken secret was the most common release failure.

## Navigation without a library

Three screens don't justify Navigation Compose. A saveable enum back stack plus `AnimatedContent` handles transitions, back and deep links (tile, shortcut and mode settings route via an intent extra or the `AUTOMATIC_ZEN_RULE` action).
