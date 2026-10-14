# Installation

For developers who build the app. New versions are published through Google Play (internal testing, invite only); see [Publishing through Google Play](#publishing-through-google-play). Older APKs remain on the [Releases](https://github.com/impca201/snoozer/releases) page, but no new ones are added.

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
git clone https://github.com/impca201/snoozer.git
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

Open Snoozer and tap **Enable notification access**. On Android 13+ sideloaded installs can show the switch greyed out; enable **App info › ⋮ › Allow restricted settings** first. Debug builds installed from Android Studio or adb are usually not restricted.

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

Release builds are signed only in CI (`.github/workflows/release.yml`). The key is the Google Play **upload key**: Play re-signs the app with its own app signing key. The workflow decodes the keystore secret (`scripts/decode_keystore.py`), checks the keystore password, alias and key password before building, writes `keystore.properties`, runs the unit tests and lint, and builds with `bundleRelease -PappVersionName=… -PappVersionCode=…`. A local `./gradlew assembleRelease` without `keystore.properties` produces an unsigned APK.

One-time setup: add these repository secrets under **Settings › Secrets and variables › Actions**:

| Secret | Value |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | The `.jks` file as base64. PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("snoozer-upload.jks")) \| Set-Clipboard` |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password |
| `ANDROID_KEY_ALIAS` | Key alias |
| `ANDROID_KEY_PASSWORD` | Key password |
| `PLAY_SERVICE_ACCOUNT_JSON` | The Google Play service account's JSON key (step 6 below) |

The same names without the `ANDROID_` prefix are also accepted, because earlier releases used them.

## Making a release

**Actions › Release › Run workflow** on `main`, choose `patch`, `minor` or `major`. Pushing a tag such as `v1.2.3` also releases. The versionCode is `major × 10000 + minor × 100 + patch`.

The workflow refuses to publish when not on `main`, when the tag exists, when the version is not higher than the last release, or when nothing changed. Otherwise it runs the tests and lint, builds a signed bundle (AAB), uploads it to Google Play's **internal testing** track, and only then creates the `vX.Y.Z` tag. No GitHub Release and no APK are published: the repository is public, so anything attached there could be downloaded by anyone. Phones that installed the app from Play update automatically within a few hours (or at once from the app's page in the Play Store). The R8 mapping file is kept as an artifact of the run (**Actions › Release › the run › Artifacts**) and is also sent to Play, which uses it to decode crash reports.

If the upload to Play fails (for example an expired service account key), no tag is created. Fix the cause and choose **Re-run failed jobs** on the run, or start the release again.

Choosing `none` is a **dry run**: it builds a signed APK and AAB as artifacts of the run, without uploading or tagging anything. Their versionCode is 1, so they **cannot update an installed app**; they are only for checking the build and for the very first upload by hand (step 5 below). Once the repository is public, artifacts can be downloaded by anyone signed in to GitHub, so delete a dry run's artifacts after you have used them (the AAB is deleted automatically after 7 days).

## Publishing through Google Play

One-time setup, done in a web browser. You need the Google account you want to publish with and about an hour, plus waiting time for Google's identity check.

### 1. Create a Play developer account

1. Go to [play.google.com/console/signup](https://play.google.com/console/signup) and sign in with your Google account.
2. Choose **Yourself** (a personal account).
3. Pay the one-time registration fee of **US$25**.
4. Complete the **identity verification** (an ID document, and a phone number and address). This can take a few days; you get an email when it is done.

### 2. Create the app

1. In the [Play Console](https://play.google.com/console), click **Create app**.
2. App name: **Snoozer**. Default language: English. App or game: **App**. Free or paid: **Free**.

   Note: Google does not let you change a free app to paid later. If you may sell the app one day, read Play Console Help on free and paid apps before choosing (a paid app also needs a payments profile).
3. Tick the declarations and click **Create app**.

The app ID (package name) is fixed by the first file you upload in step 5. It must be exactly **`io.github.impca201.snoozer`**, which the bundles built by the workflow already have. Never upload the Dev build (`.debug`): that would claim the wrong ID for good.

### 3. Play App Signing: let Google create a new key

Google signs the app that phones download with an **app signing key** that it keeps. The key in the GitHub secrets becomes only the **upload key**: it proves to Google that an upload comes from you. If an upload key is ever lost or leaked, Google can replace it (**Test and release › App integrity › Upload key certificate › Request upload key reset**).

This app uses a **new app signing key created by Google**, not the key that also signed the owner's other apps (decision of 2026-10-14 in [decisions.md](decisions.md)). Nothing to do here: in step 5 the Play Console shows **Play App Signing** with **Use Google-generated key** (the default). Keep that choice.

Consequence: the app from Play has a different signature than the APKs from GitHub Releases. Android does not let one replace the other, so **uninstall the GitHub version once** before installing from Play. You lose only Snoozer's settings (toast message, persistent notification, mode, Follow Bedtime) and home-screen shortcuts: set them again and grant notification access again. Tasker tasks keep working, because the app ID stays the same. Switch snoozing off before uninstalling, so snoozed notifications come back first.

(The alternative, giving Google the existing key with Google's encryption tool so current installs keep updating, was not chosen: that key also signs other apps.)

### 4. Set up internal testing

1. In the app, go to **Test and release › Testing › Internal testing**.
2. Open the **Testers** tab, click **Create email list**, name it (for example "Me"), and add **your own Google account email address**: the account that is signed in to the Play Store on your phone. Save, and tick the list.
3. Keep this page open; the **Join on the web** link appears here after step 5.

Internal testing allows up to 100 testers, needs no review by Google beyond the automatic checks, and is available within minutes.

### 5. Upload the first build by hand

Google requires the very first bundle to be uploaded in the Play Console.

1. In GitHub: **Actions › Release › Run workflow**, branch `main`, bump **none**, **Run workflow**. Wait for it to finish (green).
2. Open the run and download `snoozer-<version>.aab` under **Artifacts**. (Its versionCode is 1, which is fine: every real release has a higher one, so it updates this build.)
3. In the Play Console: **Test and release › Testing › Internal testing › Create new release**.
4. Under **App integrity**, keep **Use Google-generated key** (step 3).
5. Under **App bundles**, upload the `.aab` file. Release name: `first upload`. Click **Next**, then **Save and publish** (sometimes **Start rollout to Internal testing**). Warnings about a missing privacy policy or app details may show; they do not block internal testing, errors do.
6. Back on the **Testers** tab, copy the **Join on the web** link, open it on your phone (signed in with the email from step 4), tap **Accept invitation**, then **Download it on Google Play**.
7. Uninstall the GitHub version of the app first if it is still on the phone (step 3), then install from the Play Store.
8. Delete the dry run's artifacts in GitHub (the run's page › **Artifacts** › bin icon).

### 6. Let the Release workflow upload to Play

The workflow uploads with a Google Cloud **service account**: a robot account with only the rights you give it.

1. Open [console.cloud.google.com](https://console.cloud.google.com) with the same Google account. Create a project (top bar › project picker › **New project**, for example "snoozer-release").
2. In that project, enable the API: search for **Google Play Android Developer API** and click **Enable**.
3. Go to **IAM & Admin › Service accounts › Create service account**. Name: `github-release`. Click **Create and continue**, skip the optional roles, click **Done**.
4. Click the new service account › **Keys › Add key › Create new key › JSON › Create**. A `.json` file downloads. It is a password: do not email it or put it in the repository.
5. Copy the service account's email address (it ends in `.iam.gserviceaccount.com`).
6. In the Play Console (the main page with all your apps, not inside the app): **Users and permissions › Invite new users**. Paste the service account email.
7. Under **App permissions**, click **Add app**, choose **Snoozer**, and tick at least **Release apps to testing tracks** (and **View app information and download bulk reports (read-only)**). Do not give it account-wide admin rights. Click **Invite user** (it is accepted automatically).
8. In GitHub: the repository › **Settings › Secrets and variables › Actions › New repository secret**. Name: `PLAY_SERVICE_ACCOUNT_JSON`. Value: open the `.json` file in a text editor and paste its entire contents. **Add secret**.
9. Delete the `.json` file from your computer (or keep it only in a password manager).

From now on, **Actions › Release › Run workflow** with `patch`, `minor` or `major` puts the new version on your phone through the Play Store.

### Later: a public release on Google Play

Not needed for your own use. Before the app can go to **Production** (anyone can find and install it), Google requires:

- **A closed test first** for personal developer accounts created after November 2023: as of 2026, at least **12 testers** who stay opted in for **14 days in a row**, after which you apply for production access. Google has changed these numbers before; check the current rule in the Play Console (**Dashboard**) or [Play Console Help](https://support.google.com/googleplay/android-developer/answer/14151465).
- **A privacy policy URL.** Once the repository is public, [PRIVACY.md](../PRIVACY.md) is reachable at `https://github.com/impca201/snoozer/blob/main/PRIVACY.md`.
- **The Data safety form** (**Policy and programs › App content**): fill it in from PRIVACY.md (Snoozer collects and shares no data: it has no internet permission; no ads, analytics or tracking).
- **The content rating questionnaire** and the other items under **App content** (target audience, ads: none).
- A store listing: short and full description, icon, feature graphic and screenshots.
