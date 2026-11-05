# Architecture

## General design

Snoozer is a single-module Android app (`:app`) with no network, no database and no background service of its own. It is built around one idea: **every control writes a single boolean, and one component reacts to it.**

```
 App button ─┐
 QS tile ────┤
 Shortcut ───┤                 ┌─────────────────────┐       ┌──────────────────────────┐
 Tasker ─────┼──► write ──────►│  SnoozeRepository   │──────►│  SnoozeListenerService   │
 Unsnooze ───┤  snooze_enabled │  (DataStore prefs)  │ Flow  │  snooze / restore        │
 Bedtime ────┘                 └─────────┬───────────┘       └──────────────────────────┘
                                         │ Flow
                                         ▼
                               ┌─────────────────────┐
                               │     SnoozerApp      │──► tile refresh, listener rebind,
                               │  (app-wide scope)   │    persistent notification, mode
                               └─────────────────────┘
```

No component calls another to "snooze now". They change state, and the listener converges on it. That makes the order of events irrelevant and lets the tile, shortcut and Tasker work while the UI is not running.

## Components

| Component | File | Responsibility |
|---|---|---|
| `SnoozerApp` | `SnoozerApp.kt` | Owns the process-wide `appScope` and the lazily created repository. Observes the switch and settings to refresh the tile, rebind the listener, and show/hide the persistent notification and the mode. |
| `SnoozeRepository` | `data/SnoozeRepository.kt` | Single source of truth in a Preferences DataStore named `snoozer`. Exposes each setting as an eagerly started `StateFlow` and performs atomic edits (toggle, Bedtime transitions, snoozed-key bookkeeping). |
| `SnoozeListenerService` | `service/SnoozeListenerService.kt` | The `NotificationListenerService`. Snoozes active and incoming notifications while the switch is on, restores them when it turns off, reposts the indicator if swiped away, and runs the Follow Bedtime watch. |
| `SnoozeIndicator` / `UnsnoozeReceiver` | `service/SnoozeIndicator.kt` | Optional silent, ongoing "Snoozing notifications" notification with an **Unsnooze** action (a plain notification, not a foreground service). |
| `SnoozeMode` | `service/SnoozeMode.kt` | Optional Android 15+ `AutomaticZenRule` that is activated while snoozing, so users can attach system mode effects (dim wallpaper, dark theme). |
| `SnoozeToaster` | `service/SnoozeToaster.kt` | Optional "Notification from *app* snoozed" toasts, merged per 2-second burst. |
| `NotificationAccess` | `service/NotificationAccess.kt` | Checks the notification-listener grant and deep-links to the most specific settings screen available. |
| `BedtimeMonitor` / `BedtimeParser` / `ProtoReader` | `bedtime/` | Experimental: runs `dumpsys notification --proto` and checks whether Pixel's Bedtime ("winddown") rule is among the active zen rules. |
| `SnoozeTileService` | `tile/` | Toggleable Quick Settings tile; opens the access guide when access is missing. |
| `ShortcutActivity` | `shortcut/` | Invisible trampoline behind the launcher shortcut: toggles and shows a toast. |
| `TaskerEditActivity` / `TaskerFireReceiver` / `TaskerContract` | `tasker/` | Locale plug-in: configure and execute Snooze, Unsnooze or Toggle. |
| `LaterItems` / `NotificationSnapshot` | `data/` | Plain-Kotlin rules for the "Do later" list (add, remove, refresh, storage format) and for which notifications the sheet offers. |
| `CurrentNotifications` | `service/CurrentNotifications.kt` | In-memory view the listener publishes: the notifications in the shade (for the sheet) and the tap action of each saved notification (for the widget). Never stored. |
| `LaterWidgetProvider` / `LaterWidget` / `LaterWidgetService` | `later/` | "Do later" home screen widget built with plain `RemoteViews`. Android 12+ gets its rows as `RemoteCollectionItems`; Android 8-11 through `LaterWidgetService`. |
| `LaterPickerActivity` + `LaterSheet` | `later/`, `ui/later/` | Transparent activity with a Compose bottom sheet listing the shade; opened by the widget's + and the "Do later" launcher shortcut. |
| `LaterActionActivity` | `later/` | Invisible, translucent activity behind the widget's rows: opens a saved notification or marks it done. |
| `MainActivity` + `SnoozerNavigation` | `ui/` | Compose UI with an enum back stack (`Home`, `AccessGuide`, `Settings`). |

## System flow

### Snoozing

1. A control writes `snooze_enabled = true` through `SnoozeRepository` (`setSnoozeEnabled` or `toggleSnooze`, which also clears the Bedtime ownership flag).
2. `SnoozerApp` sees the change, asks the system to refresh the tile (`TileService.requestListeningState`) and calls `NotificationListenerService.requestRebind` in case the listener was dropped.
3. `SnoozeListenerService` collects the new value and snoozes every entry in `activeNotifications`.
4. Each later `onNotificationPosted` is snoozed too. A notification is skipped when it is not clearable (`isClearable == false`) or belongs to Snoozer itself.
5. Each snooze lasts 7 days. If the notification resurfaces while snoozing is still on, it is simply snoozed again.
6. The notification's key is persisted (`snoozed_keys`, capped at 1000) on `appScope`, so the write finishes even if the listener disconnects.

### Restoring

Android has no public "unsnooze" call. When the switch turns off, the listener:

1. Atomically takes and clears all persisted keys (plus its in-memory set).
2. Drops keys whose notification is visible again (re-snoozing those would hide and re-alert them).
3. Re-snoozes the rest for 1 second, so the system reposts them almost immediately.

Persisting the keys is what lets a restore work after the process was killed or the phone restarted while snoozing.

### Do later

Saving one notification works like snoozing all of them, but per notification and without a time limit:

1. The widget's **+** (or the **Do later** shortcut) opens `LaterPickerActivity`. Its sheet lists `CurrentNotifications.active` filtered by `NotificationSnapshot.candidates` (clearable, not Snoozer's own, not a group summary, not already saved).
2. Tapping one calls `SnoozeRepository.addLater`, which stores the app name, title, text and time in `later_items`. Nothing else talks to the listener.
3. The listener sees the new key, remembers the notification's tap action (`contentIntent`) in memory and snoozes it for a year. If it ever resurfaces (snooze expired, the app re-posted it), it is snoozed again; an update from the app also refreshes its text in the list. Unsnoozing all notifications leaves saved ones hidden.
4. `SnoozerApp` pushes every change of `later_items` to the widget.
5. Tapping a row opens `LaterActionActivity`. If the tap action is still in memory, it is sent with this visible activity's permission to start screens (Android 14+ needs that opt-in), and the item is released as **Dismiss**: the snoozed notification is cancelled. Otherwise (the process restarted, or the link no longer works) it is released as **Restore**: the notification is put back in the shade and its app is opened.
6. The round button on a row releases the item as **Dismiss**.

A release is written in the same transaction that removes the item (`later_releases`), and the listener takes and applies the requests when it is connected, so nothing is lost if the listener is disconnected at that moment.

### Persistent notification and mode

`SnoozerApp.refreshSnoozeCues()` is the single place that reconciles the optional cues with current state. It runs when the switch or the relevant settings change, and on every `MainActivity` resume because permissions may have changed in system Settings meanwhile. `SnoozeMode` caches the last rule id and state so it writes to the zen configuration only when something actually changes.

### Follow Bedtime

There is no polling. While Follow Bedtime is on, the listener registers a non-exported receiver for interruption-filter and notification-policy changes plus `SCREEN_ON`, and checks once on start. A check waits 1.5 s, reads, waits again, reads again, and applies the result only when both readings agree, because modes change in several quick steps.

`SnoozeRepository.onBedtimeReading` acts only on transitions:

- Bedtime starts and Snoozer is not snoozing: snooze, and mark the snooze as owned by Bedtime.
- Bedtime ends: unsnooze only if Bedtime owns the snooze.
- Any manual change in between clears ownership, so a manual unsnooze during Bedtime is respected.

## Patterns used

- **Single source of truth / unidirectional data flow:** DataStore → `StateFlow` → observers. UI and surfaces never hold their own copy of the switch.
- **Repository:** `SnoozeRepository` hides DataStore keys and provides intent-level operations (`toggleSnooze`, `onBedtimeReading`, `takeSnoozedKeys`) as single transactions.
- **Manual dependency access:** `Context.snoozerApp` gives access to the repository and scope. No DI framework for a dozen classes.
- **Graceful degradation:** every settings deep link tries the most specific intent first and falls back to more general screens. OEM `RuntimeException`s from zen and listener APIs are caught and logged.

## Data storage

There is no database. Two Preferences DataStore files (names in `data/DataFiles.kt`):

| File | Key | Type | Purpose |
|---|---|---|---|
| `backed_up_settings` | `theme_mode`, `dynamic_color` | String, Boolean | Appearance |
| `backed_up_settings` | `toast_enabled`, `indicator_enabled` | Boolean | Optional cues |
| `backed_up_settings` | `dim_enabled` | Boolean | Mode on/off (historical name kept for compatibility) |
| `backed_up_settings` | `mode_name` | String | Custom mode name |
| `snoozer` | `snooze_enabled` | Boolean | The switch |
| `snoozer` | `follow_bedtime`, `bedtime_last_active`, `bedtime_snoozed` | Boolean | Bedtime sync state and ownership |
| `snoozer` | `snoozed_keys` | Set<String> | Keys to restore |
| `snoozer` | `later_items` | String | Notifications saved for later (encoded by `LaterItems`, at most 100) |
| `snoozer` | `later_releases` | Set<String> | Pending restore (`r:key`) or dismiss (`d:key`) requests from the widget |

**Backup.** Android's own backup (to the Google account) and phone-to-phone transfer copy only
`backed_up_settings` (`res/xml/data_extraction_rules.xml` for Android 12+, `res/xml/backup_rules.xml`
for Android 8-11, guarded by `BackupRulesTest`). The `snoozer` file stays on the phone: notification
keys and saved notifications are meaningless on another device, a restored "snoozing on" state would be surprising, and
Follow Bedtime needs adb-granted permissions that a new phone doesn't have. Settings that need a
permission on the new phone (the persistent notification, the mode) come back switched on but show
as blocked until the permission is granted, as they already do when it is revoked.

Up to version 1.3.0 the settings lived in the `snoozer` file. `MoveKeysMigration` moves them to
`backed_up_settings` the first time the settings are read after the update.
