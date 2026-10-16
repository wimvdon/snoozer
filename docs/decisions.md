# Technical decisions

## 2026-10-16: Settings as one list; guide pages without a card per section

A Material 3 audit (owner's request) found that every setting sat in its own collapsed card, so a
single switch took two taps to reach. Settings are now one list grouped by section headers, from
the shared `SettingsKit.kt` (see the android-app template's decision log): the toast and the
persistent notification share a "While snoozing" section, "Open mode settings" and "Open Tasker"
are list rows that open another app, and only setup instructions stay folded away. The access
guide keeps cards for the status and the restricted-settings tip, but its steps and privacy note
are plain page content, as Material 3 advises against a card around every section. Rejected:
keeping a section per switch (two section headers with one row each).

## 2026-10-15: Settings screens make room for the keyboard

The app targets Android 15+ and draws edge to edge, so the system no longer shrinks the screen when the keyboard opens. `SettingsScaffold` (shared design kit, same change in every app) now adds the keyboard to its content insets, so a focused text field scrolls into view instead of hiding behind the keyboard. Rejected: opting out of edge-to-edge (not possible for apps targeting Android 15+).

## 2026-10-15: Settings are backed up, device state is not

Backups were switched off completely, so a new or reset phone started Snoozer from scratch. Now Android's own backup (to the Google account) and phone-to-phone transfer copy the settings: theme, wallpaper colours, the toast message, the persistent notification, the mode and its name. They live in their own DataStore file, `backed_up_settings`, and the backup rules list only that file, so anything added to another file is never backed up by accident. The switch, the snoozed notification keys and Follow Bedtime stay on the phone (meaningless or surprising on another device; Bedtime needs adb set-up per phone). Existing settings are moved over once by a DataStore migration. Rejected: backing up the whole old file (would restore "snoozing on" and stale keys) and excluding single keys (Android's rules work per file, not per key).

## 2026-10-15: Phone testing doesn't block merging

The owner can't review code and can't always test a pull request on the device right away, so pull requests waited for a manual test that adds little over CI. Green CI (unit tests, lint, builds) is now enough to merge; the owner tests afterwards with the steps from the pull request and always before a release, and problems are fixed in a new pull request. Merging to `main` does not reach users: releases stay manual. Rejected: keeping the phone test as a merge gate (pull requests pile up and go stale).

## 2026-10-14: Back to GitHub Releases instead of Google Play

The owner chose to publish through GitHub Releases again, as before the Google Play entry below. The Release workflow once more builds a signed APK and attaches it (with the R8 mapping file) to a GitHub Release with the `vX.Y.Z` tag; the Play upload job, the AAB and the `PLAY_SERVICE_ACCOUNT_JSON` secret are no longer used. The signing key stays the one already in the GitHub secrets, so phones that installed a GitHub APK keep updating. Trade-off: the repository is public, so anyone can download the APK. Rejected: keeping Play internal testing (the developer account and its setup are not wanted for now).

## 2026-10-14: Shared design rules have their master copy in the template

`DESIGN.md` was kept identical in the three sibling apps by hand, with no single place to start a change, and the copies had drifted (Snoozer was missing the Loading section; the Tracker for Simkl icon row was out of date in two apps). The android-app template (`wimvdon/android-app`) now holds the master copy; each app keeps a full copy so Claude sessions can read the rules without opening another repository, and only its "This app" section is its own. This change also brings Snoozer's copy up to date. Rejected: keeping the file only in the template (sessions in an app would have to fetch it every time) and a shared library for the design kit (a new dependency and more setup than three small apps need). The design kit files (`ui/theme/`) stay in the apps.

## 2026-10-14: Publish through Google Play instead of GitHub Releases

The repository becomes public (free GitHub Actions minutes), and the app should not be freely downloadable as an APK. The Release workflow now builds a signed AAB and uploads it to Google Play's **internal testing** track; the owner installs from Play with automatic updates and no longer needs Obtainium. No GitHub Release is created any more; the `vX.Y.Z` tag is still created (after a successful upload), and the version rules (`scripts/release_version.py`) are unchanged. Same setup as the sibling apps Tracker for Simkl and Time Tracker for Notion.

- **Upload with `r0adkll/upload-google-play`** (approved by the owner), a widely used action, pinned to the v1.1.5 commit. It runs in its own job with read-only GitHub access and only the Play service account secret; the tag is created by a separate job afterwards, so a failed upload leaves no tag and can be re-run. Rejected: a hand-written upload script against the Play Developer API (more code to maintain).
- **A new Google-generated app signing key** (Play App Signing), not the key shared with the owner's other apps; the existing key is only the upload key. Cost: the GitHub-installed app must be uninstalled once, losing its settings and shortcuts. Rejected: giving Google the shared key, which keeps current installs updating but ties the three apps' signing together.
- **The dry run (`none`) still produces an APK and an AAB as artifacts**, for checking the build and for the first upload by hand, with versionCode 1 so they can never update an installed app. The R8 mapping file stays an artifact and is also sent to Play.
- Selling the app later would need Google's billing library (a new dependency); not part of this change.

## 2026-10-14: All rights reserved, no open-source licence

The repository becomes public so GitHub Actions minutes are free, but the code is not meant to be reused: the app may become paid later. `LICENSE` is therefore a plain copyright notice ("all rights reserved"): the source can be read, but not copied, modified, distributed or sold without written permission. Without any licence the default is the same under copyright law; the explicit notice makes it clear to readers. Rejected: MIT, Apache 2.0 and GPL, which would let anyone reuse the code or publish their own build. Note that by GitHub's terms of service, other GitHub users may still view and fork a public repository on GitHub itself; that grants no other rights.

## 2026-10-14: Aligned with the android-app template

Snoozer now follows the same rules, layout and workflows as the private `wimvdon/android-app` template, so every app is built, checked and released the same way.

- **Rules for Claude Code sessions** in `CLAUDE.md`; a plain-language pull request template; weekly Dependabot updates for Gradle and GitHub Actions.
- **CI** now also runs lint (errors fail the build) and the release-script tests, cancels outdated runs, and uploads `snoozer-<version>-debug.apk` unzipped so it installs straight from the download.
- **Release workflow** from the template: version maths in `scripts/release_version.py` (unit tested), the signing key checked before the build (`scripts/VerifySigningKey.java`), tests and lint before a signed build, and the R8 mapping file attached to each release. It publishes with `gh release create` instead of a third-party action, one fewer dependency with write access. The old secret names without `ANDROID_` keep working.
- **Debug builds get `applicationIdSuffix = ".debug"` and the name "Snoozer Dev"** (`src/debug/res/values/strings.xml`), so a pull request's APK installs next to the released app and can be told apart in the launcher and the notification access list. Two things name the package and follow it: the static launcher shortcut (a copy in `src/debug/res/xml/`, because `shortcuts.xml` cannot use placeholders) and the Follow Bedtime adb commands (now built from the installed package name).
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

There is no `INTERNET` permission and no analytics SDK. Backups and device transfers copy only the settings (see "Settings are backed up" above). `dependenciesInfo.includeInApk = false` drops the Google Play dependency blob. Notification content is never read; only the posting app's label is read, for the optional toast.

## Release signing only in CI

Signed APKs come only from GitHub Actions:

- **Signing key:** `keystore.properties` is written at build time from secrets and never committed.
- **Versioning:** versions come from tags (`major × 10000 + minor × 100 + patch`), so version codes always increase with semver.
- **Secret decoding:** `scripts/decode_keystore.py` tolerates common copy-paste damage (line breaks, quotes, URL-safe alphabet, double encoding) and reports what's wrong without printing the secret, because a broken secret was the most common release failure.

## Navigation without a library

Three screens don't justify Navigation Compose. A saveable enum back stack plus `AnimatedContent` handles transitions, back and deep links (tile, shortcut and mode settings route via an intent extra or the `AUTOMATIC_ZEN_RULE` action).
