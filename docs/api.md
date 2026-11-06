# Public interface

Snoozer has no HTTP API or CLI. Its public interface is the set of Android components other apps or the system can reach. All of them only change the snooze switch or open a screen; none returns data.

## Tasker / Locale plug-in

Implements the [Locale plug-in contract](https://tasker.joaoapps.com/plugins.html), supported by Tasker, MacroDroid, Automate and others.

### Edit: `TaskerEditActivity`

| | |
|---|---|
| Intent action | `com.twofortyfouram.locale.intent.action.EDIT_SETTING` |
| Exported | yes |
| Input | Optional `com.twofortyfouram.locale.intent.extra.BUNDLE` (previous configuration, used to preselect the command; defaults to Snooze) |
| Output (`RESULT_OK`) | `…extra.BUNDLE` = `{ "io.github.impca201.snoozer.extra.COMMAND": "<id>" }` and `…extra.BLURB` = localized command label |
| Cancel | `RESULT_CANCELED`, nothing stored |

### Fire: `TaskerFireReceiver`

| | |
|---|---|
| Intent action | `com.twofortyfouram.locale.intent.action.FIRE_SETTING` |
| Exported | yes (hosts must be able to deliver the broadcast) |
| Input | `com.twofortyfouram.locale.intent.extra.BUNDLE` containing `io.github.impca201.snoozer.extra.COMMAND` |
| Effect | Writes the switch on `appScope` using `goAsync()`, so the write completes after `onReceive` returns |

### Commands

| Id | Effect | Repository call |
|---|---|---|
| `snooze` | Turn snoozing on | `setSnoozeEnabled(true)` |
| `unsnooze` | Turn snoozing off and restore | `setSnoozeEnabled(false)` |
| `toggle` | Flip the switch atomically | `toggleSnooze()` |

The ids are stored inside users' tasks and are part of the contract. They are case-sensitive and must never be renamed (`TaskerContractTest` guards this).

Example bundle a host stores:

```
com.twofortyfouram.locale.intent.extra.BUNDLE = Bundle {
    "io.github.impca201.snoozer.extra.COMMAND" = "toggle"
}
```

### Error handling

| Situation | Behaviour |
|---|---|
| Unknown or missing id (`"SNOOZE"`, `""`, absent) | Ignored silently |
| Malformed bundle that throws while unparcelling | Caught; ignored |
| Wrong intent action | Ignored |
| Notification access not granted | The switch is still written; nothing is snoozed until access is granted and the listener binds |

There is no authentication: any app can fire the receiver, as with every Locale plug-in. The worst case is toggling snoozing, which is fully reversible.

## Launcher shortcut

Static shortcut `toggle_snooze` (`res/xml/shortcuts.xml`):

| | |
|---|---|
| Target | `io.github.impca201.snoozer.shortcut.ShortcutActivity` |
| Action | `io.github.impca201.snoozer.action.TOGGLE` |
| Exported | no; only the launcher (with Snoozer's identity) or gesture features such as Pixel Quick Tap can start it |
| Effect | `toggleSnooze()`, then a toast "snoozed" / "unsnoozed". Without notification access it opens the access guide instead. |

## Quick Settings tile

`SnoozeTileService`, declared as `TOGGLEABLE_TILE`:

- **Click:** toggles. Without notification access it opens the access guide.
- **Long-press:** opens `MainActivity` (`QS_TILE_PREFERENCES`).
- **State:** active while snoozing; subtitle **On**, **Off** or **Setup needed**.

## `MainActivity` entry points

| Intent | Opens |
|---|---|
| `MAIN` / `LAUNCHER` | Home |
| `android.app.action.AUTOMATIC_ZEN_RULE` (from Settings › Modes › Snoozer) | Settings |
| `android.service.quicksettings.action.QS_TILE_PREFERENCES` | Home |
| Internal extra `io.github.impca201.snoozer.extra.DESTINATION` = `Home` / `AccessGuide` / `Settings` | That screen (used by tile, shortcut and notification) |

## Persistent notification action

`UnsnoozeReceiver` is not exported. It is reachable only through the immutable `PendingIntent` on the persistent notification's **Unsnooze** button and calls `setSnoozeEnabled(false)`.
