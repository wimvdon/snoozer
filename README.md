# Snoozer

Snooze all your Android notifications with one tap, and bring them all back with another.

Snoozer doesn't delete or mute notifications. It puts them aside, and they return when you stop snoozing. Calls, alarms, media controls, navigation and other notifications you can't swipe away are never snoozed.

- Toggle from the app, a Quick Settings tile, a launcher shortcut, Pixel Quick Tap or Tasker
- No internet permission, no ads, no tracking ([privacy policy](PRIVACY.md))
- Material 3 with dynamic colours
- English, Dutch, French, German, Spanish and Italian
- Android 8.0 or newer

## Install

1. Download the latest `snoozer-x.y.z.apk` from [Releases](https://github.com/impca201/snoozer/releases) on your phone and open it. Allow installing from your browser or file manager if Android asks.
2. Open Snoozer and tap **Enable notification access**, then follow the guide.

> **Can't turn on notification access (switch is greyed out)?** Android 13+ restricts this for apps installed outside the Play Store. Go to **App info › ⋮ › Allow restricted settings**, then try again. The in-app guide has a button for this.

Updates install over the previous version and keep your settings.

## Use

Tap the big **Snooze** button. Notifications disappear and new ones stay silent until you tap **Unsnooze**.

Everything else is under the cogwheel:

| Setting | What it does |
| --- | --- |
| **Toast notification** | Shows "Notification from *app* snoozed" each time something is snoozed. |
| **Persistent notification** | Shows a silent notification with an icon in the status bar while snoozing, with an **Unsnooze** button. Off by default. |
| **Mode** (Android 15+) | Turns on a mode called "Snoozer" while snoozing. Give it any effects you like (dim wallpaper, dark theme, …) in Android's mode settings. |
| **Follow Bedtime** (experimental) | Snoozes automatically during Bedtime mode. Needs a one-time setup from a computer, see below. |
| **Shortcuts** | Adds the Quick Settings tile, and explains the launcher shortcut and Pixel Quick Tap. |
| **Tasker plugin** | Use the "Snoozer" action in Tasker, MacroDroid, Automate and similar apps. Snoozer has no triggers of its own. |

### Follow Bedtime setup

Android only tells apps which mode is on if they have two permissions that can only be granted over USB. With [Platform-Tools](https://developer.android.com/tools/releases/platform-tools) installed and USB debugging enabled, run:

```
adb shell pm grant io.github.impca201.snoozer android.permission.DUMP
adb shell pm grant io.github.impca201.snoozer android.permission.PACKAGE_USAGE_STATS
```

Then tap **Check now** in Snoozer and switch on **Snooze during Bedtime**. You only need to do this again after a reinstall, not after updates. This has only been tested on Pixel phones; if it can't read the Bedtime status on yours, please [open an issue](https://github.com/impca201/snoozer/issues).

## Build it yourself

You need [Android Studio](https://developer.android.com/studio), which also installs Java and the Android SDK.

```
build.bat            # Windows: debug APK in app\build\outputs\apk\debug\
./gradlew assembleDebug   # macOS / Linux
```

Other `build.bat` targets: `install` (to a connected phone), `test`, `lint`, `all`, `clean`.

## Releases

Signed APKs are built and published by GitHub Actions only.

**One-time setup:** create an upload key (Android Studio › **Build › Generate Signed Bundle/APK › Create new**), keep it backed up, and add four repository secrets under **Settings › Secrets and variables › Actions**:

| Secret | Value |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | The `.jks` file as base64. In PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("snoozer-upload.jks")) \| Set-Clipboard` |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password |
| `ANDROID_KEY_ALIAS` | Key alias, e.g. `upload` |
| `ANDROID_KEY_PASSWORD` | Key password |

**To release:** open **Actions › Release › Run workflow**, set **Release** to `patch` (1.0.1 → 1.0.2), `minor` (1.0.1 → 1.1.0) or `major` (1.0.1 → 2.0.0), and run it. It picks the next version after the latest tag and publishes the APK on the [Releases](https://github.com/impca201/snoozer/releases) page a few minutes later. Pushing a tag like `v1.2.0` works too. The version code is `major × 10000 + minor × 100 + patch`, so each new tag must be higher than the last.

To try a build without publishing, leave **Release** at `none`; the APK appears under **Artifacts** on that run (a debug APK if the signing secrets are missing).

## How it works

A `NotificationListenerService` snoozes every clearable notification and re-snoozes anything that comes back. Android has no "unsnooze" call, so restoring re-snoozes each one for a second. The app, tile, shortcut and Tasker plugin only flip a DataStore switch that the listener watches.

Kotlin, Jetpack Compose, Material 3, DataStore. Compile/target SDK 37, min SDK 26.

```
app/src/main/java/io/github/impca201/snoozer/
├── data/       Settings and snooze state (DataStore)
├── service/    Notification listener, persistent notification, mode, toasts
├── bedtime/    Follow Bedtime (reads the mode state)
├── tile/       Quick Settings tile
├── shortcut/   Launcher shortcut
├── tasker/     Tasker/Locale plugin
└── ui/         Compose screens
```
