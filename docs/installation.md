# Installation

## Prerequisites

| Requirement | Version | Notes |
|---|---|---|
| JDK | 17 or newer | Android Studio's bundled JBR works. CI uses Temurin 21. |
| Android SDK | Platform 37 | `compileSdk` and `targetSdk` are 37. Install it via Android Studio › **SDK Manager**. |
| Android device or emulator | Android 8.0+ (API 26) | Some features need newer versions: tile subtitle (10+), add-tile prompt (13+), mode (15+). |
| Git | any | |
| adb (Platform-Tools) | optional | Only for `installDebug` and the Follow Bedtime permissions |

Gradle 9.8.1 is downloaded by the wrapper. No environment variables, database or external services are needed.

## 1. Clone the repo

```bash
git clone https://github.com/wimvdon/snoozer.git
cd snoozer
```

## 2. Point Gradle at the SDK

Opening the project in Android Studio creates `local.properties` automatically. From the command line:

```bash
echo "sdk.dir=$HOME/Android/Sdk" > local.properties        # Linux; macOS: $HOME/Library/Android/sdk
```

On Windows, `build.bat` writes `local.properties` from `ANDROID_HOME`, `ANDROID_SDK_ROOT` or `%LOCALAPPDATA%\Android\Sdk`, and finds Android Studio's JDK if `JAVA_HOME` is unset.

## 3. Build

```bash
./gradlew assembleDebug          # APK: app/build/outputs/apk/debug/app-debug.apk (ID io.github.impca201.snoozer.debug)
```

```bat
build.bat                        :: same, on Windows
```

Dependencies are resolved from Google Maven and Maven Central on the first build.

## 4. Run on a device

With USB debugging enabled:

```bash
./gradlew installDebug           # Windows: build.bat install
```

Or open the project in Android Studio and press **Run**.

Debug builds use the application ID `io.github.impca201.snoozer.debug` and the name **Snoozer Dev**, so they install **next to** a released Snoozer with their own data and their own notification access (grant it to "Snoozer Dev"). Uninstall it when you are done.

To test a pull request without building, open it on GitHub › **Checks** › **CI** › **Artifacts** and install `snoozer-<version>-debug.apk`.

## 5. Grant notification access

Open Snoozer and tap **Allow access** under the toggle. On Android 13+ sideloaded installs can show the switch greyed out; enable **App info › ⋮ › Allow restricted settings** first. Debug builds installed from Android Studio or adb are usually not restricted.

## Verify it works

1. Post any test notification (for example a message to yourself).
2. Tap **Snooze** in the app: the notification disappears from the shade.
3. Tap **Unsnooze**: it returns within about a second.

From a shell you can confirm the listener is bound:

```bash
adb shell cmd notification allowed_listeners | grep snoozer
```

## Optional: Follow Bedtime permissions

```bash
adb shell pm grant io.github.impca201.snoozer android.permission.DUMP
adb shell pm grant io.github.impca201.snoozer android.permission.PACKAGE_USAGE_STATS
```

For a debug build, use `io.github.impca201.snoozer.debug`; the Settings screen shows the right commands for the installed build. These survive updates but not reinstalls.

## Release builds

Release APKs are signed only in CI (`.github/workflows/release.yml`). The workflow decodes the keystore secret (`scripts/decode_keystore.py`), checks the keystore password, alias and key password before building, writes `keystore.properties`, runs the unit tests and lint, and builds with `assembleRelease -PappVersionName=… -PappVersionCode=…`. A local `./gradlew assembleRelease` without `keystore.properties` produces an unsigned APK.

One-time setup: add these repository secrets under **Settings › Secrets and variables › Actions**:

| Secret | Value |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | The `.jks` file as base64. PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("snoozer-upload.jks")) \| Set-Clipboard` |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password |
| `ANDROID_KEY_ALIAS` | Key alias |
| `ANDROID_KEY_PASSWORD` | Key password |

The same names without the `ANDROID_` prefix are also accepted, because earlier releases used them.

## Making a release

Open **Actions › Release › Run workflow** on `main` and pick `patch`, `minor` or `major`, or push a tag such as `v1.2.0`. With `none`, the run only uploads an APK as an artifact (a debug APK if the secrets are missing). The version code is `major × 10000 + minor × 100 + patch`, so minor and patch stay below 100.

The workflow refuses to publish when it is not on `main`, when the tag or release already exists, when the version is not higher than the previous release, or when nothing changed since the previous release. Each release has `snoozer-<version>.apk` and `snoozer-<version>-mapping.txt` (R8's name map, to read crash stack traces from release builds).
