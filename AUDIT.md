# Pre-publication audit

_Audit of the whole codebase before publishing to Google Play, 11 October 2026._

**Verdict: ready to publish**, once the items under [Before you publish](#before-you-publish)
are done. CI builds debug and release (R8-minified) APKs, runs the unit tests and Android lint
on every push.

## Scope

All Kotlin sources, the manifest, resources and translations, the Gradle build, the CI
workflow and `build.bat`. Checks covered correctness, security, privacy, Play policy,
performance and battery use, accessibility, localization and build hygiene.

## Fixed during the audit

| Area | Finding | Fix |
|---|---|---|
| Robustness | An I/O error writing preferences (for example a full disk) in a background coroutine would crash the app. | App, listener and tile scopes now have a logging `CoroutineExceptionHandler`. |
| Robustness | `snoozeNotification` can throw more than `SecurityException` on some OEM builds. | Any `RuntimeException` is caught; that notification is left alone. |
| Battery / performance | The toggle's rotation and breathing and the glow's drift were read during composition, so the toggle and glow recomposed every frame while snoozing. | The animated values are read in `graphicsLayer {}` and `Canvas {}` (layer and draw phases), so only redraws happen. Animations only exist while snoozing. |
| Correctness | Unsnoozing could briefly hide and re-alert a notification that was visible again (re-posted with the same key, or after a reboot). | Keys of currently visible notifications are skipped when restoring. |
| Correctness | A snoozed key written after a fast "off" could be lost if the listener disconnected mid-write. | Writes run on the app scope. |
| Tile accuracy | As an `ACTIVE_TILE`, the tile was only refreshed on request and could show a stale state. | Standard tile: the system refreshes it every time Quick Settings opens. Taps render the new state immediately. |
| Lint | `state_on` / `state_off` overrode private Compose strings used for accessibility. | Renamed. |
| Lint | `startActivityAndCollapse(Intent)` flagged on the pre-Android-14 path. | Suppressed on that path only; Android 14+ uses the `PendingIntent` overload. |
| Build | AGP 9.4.0 → 9.4.1. | Updated. |

## Security

- **Permissions.** `POST_NOTIFICATIONS` (Android 13+, only for the optional persistent
  "Snoozing notifications" notification; snoozing works without it), plus
  `ACCESS_NOTIFICATION_POLICY` (only for the opt-in Android 15+ "Snoozer" mode, created with
  Android's defaults and removed again when switched off), `DUMP` and `PACKAGE_USAGE_STATS`
  (only for the experimental "Follow Bedtime"; declared with `tools:ignore="ProtectedPermissions"`
  because only the user can grant them, via adb; without them the feature reports "setup needed"
  and does nothing), plus
  `BIND_NOTIFICATION_LISTENER_SERVICE` and `BIND_QUICK_SETTINGS_TILE` as service guards.
  There's no `INTERNET` permission, so the app cannot send data anywhere.
- **Exported components.**
  - `MainActivity` is the launcher. Its only extra picks which screen opens; it is validated
    against an enum.
  - The listener and tile services are protected by system-only bind permissions.
  - `TaskerEditActivity` only returns a result to its caller.
  - `TaskerFireReceiver` must be exported for the Tasker/Locale protocol. Any installed app
    could therefore send it Snooze/Unsnooze/Toggle. The impact is limited to switching
    snoozing; no data is exposed. The bundle is parsed defensively (allow-list of three ids,
    unparcelling errors caught). **Accepted risk**, standard for Locale plug-ins.
  - `UnsnoozeReceiver` (the persistent notification's Unsnooze button) is not exported; only
    the app's own immutable `PendingIntent` can reach it.
  - `ShortcutActivity` is not exported. Launcher shortcuts start it with the app's own
    identity.
- **PendingIntents** are `FLAG_IMMUTABLE` with explicit components.
- **Backups.** `allowBackup=false` and data-extraction rules exclude everything. Stored
  notification keys are device-specific and never leave the device.
- **Signing.** Release keys come from an untracked `keystore.properties`; `*.jks`,
  `*.keystore` and `keystore.properties` are git-ignored.
- **Wrapper.** `gradle-wrapper.jar` matches Gradle's published SHA-256, and CI validates it.
  The distribution is pinned by checksum.

## Privacy and Play policy

- Notification access is the app's core function, explained in-app before the user is sent
  to Settings, with a privacy note. Notification content is never read or stored, only keys
  and, for the optional toast, the posting app's name.
- `PRIVACY.md` is ready to host. Play needs a public URL to it. The data safety form answer
  is **"No data collected, no data shared."**
- `<queries>` names only Tasker's package (for "Open Tasker"). No `QUERY_ALL_PACKAGES`.
- targetSdk 37 meets Play's target-API requirement.

## Behaviour and platform limits (by design)

- Android has no indefinite snooze and no public "unsnooze". Snoozer snoozes in 7-day chunks
  and re-snoozes anything that resurfaces while on. It restores by re-snoozing each key for
  1 second.
- If notification access is revoked, or the app is uninstalled while snoozing is on,
  already-snoozed notifications come back when their current chunk expires (at most 7 days).
  The listener cannot act once access is gone. Worth a line in the store description or FAQ.
- When an app cancels a snoozed notification, Android drops the snoozed copy; it does not
  come back.
- Notifications don't survive a reboot. After a restart, apps that re-post are snoozed again
  if snoozing is on.
- Android caps concurrent snoozes (about 500 on recent versions). Beyond that, notifications
  simply show.
- Ongoing / non-dismissible notifications (calls, media, navigation, alarms, foreground
  services) are intentionally never snoozed.

## Follow Bedtime (experimental)

- Reads `dumpsys notification --proto` and only looks at the active-modes list
  (`ZenModeProto.enabled_active_conditions`), so notifications that mention Bedtime can't cause
  false readings. An unexpected layout is reported as unreadable, never guessed.
- Triggered only by Do Not Disturb/mode change broadcasts, `onInterruptionFilterChanged` and
  screen-on, registered inside the already-running listener; reads twice 1.5 s apart and acts
  only when both agree. It acts on changes only, and only undoes a snooze it started itself.
- The dump format is internal to Android and may change in future versions.

## Battery

- No foreground service, no wake locks, no alarms, no polling. The persistent notification is a
  plain ongoing notification; when it's swiped away, the already-running listener reposts it. Work happens only in
  callbacks: the system-bound notification listener, tile clicks, the shortcut, Tasker
  broadcasts.
- UI animations exist only while snoozing and the screen is visible, and run in the
  layer/draw phase. With "Remove animations" on, everything is static.

## Accessibility

- The toggle is a `Role.Switch` with a state description; the toast setting is a switch item.
- Every icon-only button has a content description; decorative icons are `null`.
- Guide headings are marked as headings; the tile-add result is a polite live region.
- The tile exposes a subtitle and state description (Android 10+/11+).
- Touch targets are ≥ 48dp (the toggle is 232dp). Colours come from Material 3 roles, so
  dynamic colour and dark theme keep contrast.

## Localization

English, Dutch, French, German, Spanish and Italian. All keys are present in every
language. Plurals cover the `one`/`many`/`other` categories each language needs. Apostrophes
and quotes are escaped. Per-app language is supported (`locales_config.xml`), and
`localeFilters` strips other languages pulled in by libraries.

## Before you publish

1. **Create an upload key** and `keystore.properties`, then build with `build.bat bundle`.
   Enrol in Play App Signing.
2. **Host `PRIVACY.md`** (for example with GitHub Pages) and enter its URL in Play Console.
3. **Choose a licence** if the repository will be public (none is included).
4. **Store listing:** screenshots, short and full description in the six languages. Mention
   the "notification access" requirement and the revoke/uninstall note above.
5. **Test on a device** before release: grant access, snooze and unsnooze from all four entry
   points, toast on and off, add the tile, Pixel Quick Tap, and check the morphing toggle and
   glow look right in light, dark and dynamic colour.

## Optional ideas (not required)

- A Tasker *condition* (state) plug-in, so profiles can react to "snoozing on".
- An allow-list of apps that are never snoozed (for example the phone or a pager app).
- Instrumented UI tests (Compose test rule) once a device or emulator is available in CI.
