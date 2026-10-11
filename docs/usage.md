# Usage guide

## First launch

1. Open Snoozer and tap **Enable notification access**.
2. In the system screen, switch Snoozer on and confirm.
3. If the switch is greyed out (Android 13+, apps installed outside a store), tap **Open App info** in the guide. Then choose **⋮ › Allow restricted settings** and try again.
4. Back in Snoozer, the guide shows **You're all set**.

Without access, the tile and shortcut open this guide instead of toggling.

## Snooze and unsnooze

Tap the large **Snooze** button. All clearable notifications leave the shade, and new ones are snoozed as soon as they arrive. Tap **Unsnooze** to bring everything back within about a second.

What is never snoozed:
- notifications you can't swipe away: calls, alarms, media players, navigation, foreground services;
- Snoozer's own notifications.

Snoozing survives app restarts and phone reboots. After a reboot, unsnoozing still restores what was snoozed before.

## Other ways to toggle

| Surface | How to set it up | Behaviour |
|---|---|---|
| **Quick Settings tile** | Settings › Shortcuts › **Add tile** (Android 13+), or edit Quick Settings manually | Tap toggles; subtitle shows the state; long-press opens the app |
| **Launcher shortcut** | Long-press the Snoozer icon; drag **Toggle** to the home screen if wanted | Toggles and shows a short toast |
| **Pixel Quick Tap** | Settings › System › Gestures › **Quick Tap to start actions** › **Open app**, then the settings icon next to Snoozer › pick the toggle shortcut | Double-tap the back of the phone to toggle |
| **Persistent notification** | Settings › **Persistent notification** | **Unsnooze** button while snoozing |
| **Tasker and similar apps** | See [Automation](#automation) | Snooze, Unsnooze or Toggle from any task |

## Settings

Open the cogwheel on the home screen.

| Setting | Default | Effect |
|---|---|---|
| **Toast notification** | Off | "Notification from *app* snoozed" each time something is snoozed. Bursts are merged ("Notifications from 3 apps snoozed"). |
| **Persistent notification** | Off | Silent ongoing notification with status-bar icon and **Unsnooze** while snoozing. Asks for notification permission on Android 13+. If swiped away, it comes back. |
| **Mode** (Android 15+) | Off | Turns on a system mode named "Snoozer" (or a custom name) while snoozing. Needs Do Not Disturb access. Configure its effects under Android **Settings › Modes**. Switching it off deletes the mode. |
| **Follow Bedtime** (experimental) | Off | Snoozes when Pixel Bedtime starts and unsnoozes when it ends. Needs the one-time adb setup below. |
| **Shortcuts** | — | Add the tile, and guides for the launcher shortcut and gestures |
| **Tasker plugin** | — | Setup steps and links |

### Follow Bedtime setup

From a computer with [Platform-Tools](https://developer.android.com/tools/releases/platform-tools) and USB debugging on (the screen has a **Copy commands** button):

```bash
adb shell pm grant io.github.impca201.snoozer android.permission.DUMP
adb shell pm grant io.github.impca201.snoozer android.permission.PACKAGE_USAGE_STATS
```

Then tap **Check now** and switch on **Snooze during Bedtime**. The status line reads one of:
- **Bedtime is on right now.** or **Bedtime is off right now.** — working.
- **Setup needed** — the permissions are missing (for example after a reinstall).
- **Can't read the Bedtime status on this phone.** — the system dump has an unexpected layout; the feature can't work on this device.

Edge cases:
- If you unsnooze manually during Bedtime, Snoozer won't re-snooze until the next Bedtime start.
- If you were already snoozing when Bedtime started, Bedtime ending won't unsnooze you.

## Automation

Snoozer is a Locale/Tasker plug-in (also MacroDroid, Automate and others). In Tasker:

1. On the **Tasks** tab, tap **+** to create a task, then **+** › **Plugin** › **Snoozer**.
2. Tap the edit button next to **Configuration**, pick **Snooze**, **Unsnooze** or **Toggle**, tap **Save**, then go back.
3. Use the task from any profile: a time range, calendar event, Wi-Fi network, driving, and so on.

Example: a profile *Time 22:30–07:00* with entry task **Snooze** and exit task **Unsnooze**.

Contract details: [api.md](api.md).

## Common problems

| Problem | Fix |
|---|---|
| Notification access switch greyed out | **App info › ⋮ › Allow restricted settings**, then retry |
| Some notifications stay | They are non-clearable (calls, media, navigation) and are left alone by design |
| Tile says "Setup needed" or opens the guide | Notification access was revoked; grant it again |
| Persistent notification doesn't appear | Allow notifications for Snoozer (Android 13+); the setting opens the right screen if they are blocked |
| Mode setting has no effect | Grant Do Not Disturb access when asked; requires Android 15+ |
| Follow Bedtime says "Setup needed" after reinstall | Re-run the two adb commands |
