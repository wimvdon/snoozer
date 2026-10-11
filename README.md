# Snoozer

This guide takes you from zero to a working Snoozer on your Android phone: you download the code to your Windows PC, turn it into an app with one double-click-and-type command, install it on your phone and set it up. At the end there is a chapter on preparing the app for the Google Play Store.

## What Snoozer does

Snoozer silences **all** notifications with one tap and keeps them silent until you tap again. Your notifications are not deleted: they are put aside ("snoozed") and come back the moment you stop snoozing. That makes it handy at night, in meetings, during focus time or in the cinema.

You can switch snoozing on and off in five ways:

- the big button in the app;
- a **Quick Settings tile** (the buttons you see when you swipe down from the top of the screen, like Wi-Fi and the flashlight);
- a **launcher shortcut** (the small menu that appears when you touch and hold an app icon);
- a gesture, for example double-tapping the back of a Pixel phone;
- the automation app **Tasker**.

Snoozer has no internet permission, shows no ads and collects no data. See [PRIVACY.md](PRIVACY.md).

## What you need

- A Windows PC (Windows 10 or 11) with about 10 GB of free disk space.
- An Android phone with Android 8.0 or newer.
- A USB cable that connects your phone to your PC.
- About one hour for the first time. Most of that is waiting for downloads.

## Chapter 1: Install Android Studio on your PC

Android Studio is Google's free program for making Android apps. You won't write any code in it, but it installs the two things Snoozer needs to be built: **Java** (the engine that runs the build tools) and the **Android SDK** (Google's toolbox for making Android apps).

1. On your PC, open your web browser and go to **https://developer.android.com/studio**.
2. Click the big **Download Android Studio** button. Tick the box to accept the terms and click **Download**. A file whose name starts with `android-studio` and ends with `.exe` is saved to your **Downloads** folder.
3. Open File Explorer (the yellow folder icon on the taskbar), click **Downloads** on the left, and double-click the `android-studio-….exe` file.
4. If Windows asks **Do you want to allow this app to make changes to your device?**, click **Yes**.
5. The installer opens. Keep clicking **Next** without changing anything, then click **Install**. When it says **Completing Android Studio Setup**, leave **Start Android Studio** ticked and click **Finish**.
6. Android Studio starts and shows a **Setup Wizard**. If it asks whether to import settings, choose **Do not import settings** and click **OK**.
7. In the wizard, click **Next**. When asked for the **Install Type**, choose **Standard** and click **Next**. Click **Next** on the remaining screens, accept every license (click each item in the list, then select **Accept**), and click **Finish**.
8. The wizard now downloads the Android SDK. This can take 10 to 30 minutes. When the button at the bottom changes to **Finish**, click it.
9. You see the **Welcome to Android Studio** window. You are done with Android Studio. Close it.

## Chapter 2: Get the Snoozer code onto your PC

1. In your web browser, go to **https://github.com/impca201/snoozer**.
2. Click the green **Code** button above the list of files, then click **Download ZIP**. A file called `snoozer-main.zip` is saved to your **Downloads** folder.
3. Open File Explorer, click **Downloads**, right-click `snoozer-main.zip` and click **Extract All…**.
4. In the window that opens, click **Extract**. A new folder `snoozer-main` opens. Inside it is a second folder, also called `snoozer-main`. Double-click it. You now see files such as `build.bat`, `README.md` and a folder called `app`. This is the **project folder**. Keep this window open.

## Chapter 3: Build the app

"Building" turns the code into an **APK**: the installation file for an Android app, like an `.exe` file on Windows.

1. In the File Explorer window with the project folder (the one that contains `build.bat`), click once in the **address bar** at the top, where the folder path is shown. The path turns blue.
2. Type `cmd` and press **Enter**. A black **Command Prompt** window opens. It is already "standing" in the project folder, so you don't need to type any folder paths.
3. In the black window, type `build.bat` and press **Enter**.
4. Lines of text start to scroll. The first time, the build downloads its tools and libraries, which takes 5 to 15 minutes. Later builds take about one minute.
5. When it finishes, the last lines say `[OK] Done.` and `APK: app\build\outputs\apk\debug\`. That means it worked.
6. Go back to File Explorer and open the folders **app** › **build** › **outputs** › **apk** › **debug**. Inside is the file **app-debug.apk**: your app.

If you see `[FAILED]` or `[ERROR]` instead, go to [Chapter 8: Troubleshooting](#chapter-8-troubleshooting).

## Chapter 4: Install the app on your phone

1. Connect your phone to the PC with the USB cable and unlock the phone.
2. On the phone, swipe down from the top of the screen. Tap the notification that says something like **Charging this device via USB**, and choose **File transfer**. (On some phones this option is called **Transfer files** or **MTP**.)
3. On the PC, open a new File Explorer window. Your phone appears on the left under **This PC**, with its model name. Click it, then open **Internal shared storage** (or **Internal storage**) and then the **Download** folder.
4. Drag **app-debug.apk** from the `debug` folder (Chapter 3, step 6) into the phone's **Download** folder. Wait until the copy is finished.
5. On the phone, open the **Files** app (on Pixel phones: **Files by Google**; on Samsung phones: **My Files**). Tap **Downloads**, then tap **app-debug.apk**.
6. Android warns that your phone isn't allowed to install apps from this source. Tap **Settings**, switch on **Allow from this source**, then tap the back arrow.
7. Tap **Install**. If Google Play Protect asks about an unknown app, tap **More details** and then **Install anyway**. This is expected: the app is yours and hasn't been through the Play Store.
8. When it says **App installed**, tap **Open**.

## Chapter 5: Set up Snoozer on your phone

Snoozer needs **notification access**: Android's permission that lets an app see and manage the notifications of other apps. Without it, the big button is grey.

1. In Snoozer, tap the red banner **Enable notification access**. A guide opens.
2. Tap **Open settings**. Android's settings open on the notification access page for Snoozer.
3. Switch on **Allow notification access**.
4. A confirmation appears. Tap **Allow**.
5. Tap the back arrow until you are back in Snoozer. The guide now says **You're all set**. Tap **Back to Snoozer**. The red banner is gone and the big button is in colour.

**If the switch in step 3 is grey and can't be turned on:** Android 13 and newer block this setting for apps installed outside an app store until you unlock it.

1. Go back to Snoozer's notification access guide and tap **Open App info**.
2. On the **App info** page, tap the **⋮** (three dots) menu in the top-right corner.
3. Tap **Allow restricted settings** and confirm with your PIN, pattern or fingerprint.
4. Return to Snoozer and repeat steps 2 to 5 above. The switch now works.

## Chapter 6: Use Snoozer

### The big button

1. Open Snoozer and tap the big button labelled **Snooze**. It changes shape, turns colour, and the background starts to glow softly. All notifications on your phone disappear from the notification shade and new ones stay silent.
2. To get everything back, tap the button again (it now says **Unsnooze**). All snoozed notifications return within a second or two.

Calls, alarms, music controls, navigation and other notifications you can't swipe away are never snoozed, so you can't miss anything that matters right now.

### The Settings page

Everything else lives on one page. In Snoozer, tap the **cogwheel** icon at the top right. You see six sections; tap a section's title to open or close it.

#### Toast notification

1. Open **Toast notification** and switch on **Enable toast notification**.
2. From now on, a short message such as "Notification from WhatsApp snoozed" appears at the bottom of the screen each time something is snoozed.

#### Persistent notification

While snoozing, Snoozer shows a silent notification called **Snoozing notifications**. Its bell-with-zz icon appears in the status bar (the strip at the very top of the screen with the clock), so you can see that snoozing is on. Swipe down and tap **Unsnooze** in that notification to stop snoozing without opening the app. This is on by default.

1. The first time you tap **Snooze**, Android asks **Allow Snoozer to send you notifications?** Tap **Allow**. (Android 13 and newer only. If you tap **Don't allow**, snoozing still works, you just won't see the icon.)
2. If you swipe the notification away, it comes straight back while snoozing is on. **Clear all** doesn't remove it either.
3. Don't want it? Open **Persistent notification** and switch off **Show persistent notification**.

If you see **Notifications are off for Snoozer. Tap to allow.**, tap it. Android either asks for permission again or opens Snoozer's notification settings: switch on **All Snoozer notifications** there, then go back.

Note: if you use a Do Not Disturb profile that hides notifications, it hides this icon too. As an alternative signal, use the Mode below and give it an effect such as **Dim the wallpaper** in Android's Settings.

#### Mode (Android 15 and newer)

Snoozer can switch on a mode of its own while snoozing, so you can attach Android's mode effects to snoozing: for example dim the wallpaper, dark theme, or who can still reach you. This is off by default.

1. Open **Mode** and switch on **Turn on a mode while snoozing**.
2. Snoozer adds a mode called **Snoozer** under **Settings** › **Modes**. Snoozer never changes what it does; that's up to you.
3. Tap **Open mode settings** and choose what the mode should do. It can be combined with your other modes.
4. Want a different name? Type it in the **Mode name** field in Snoozer and tap **Save**.
5. If you see **Needs Do Not Disturb access. Tap to allow.**, tap it, switch on **Snoozer** in the list and go back.
6. Switch the setting off again and Snoozer removes the mode, including your changes to it.

On Android 14 and older this section says **Requires Android 15 or newer**.

#### Follow Bedtime

Snoozer can snooze automatically while Bedtime mode is on, and unsnooze when it ends. If you snooze or unsnooze yourself during Bedtime, Snoozer leaves it that way. This is experimental: it is only tested on Pixel phones and may change in future versions.

Android only shares which mode is on with apps that have two special permissions, and you can only grant those from a computer. You do this once:

1. On your PC, download **SDK Platform-Tools** from **https://developer.android.com/tools/releases/platform-tools** and extract the ZIP. You get a folder called `platform-tools`.
2. On your phone, turn on **Developer options**: open **Settings** › **About phone** and tap **Build number** seven times until it says you are a developer.
3. Open **Settings** › **System** › **Developer options** and switch on **USB debugging**.
4. Connect the phone to the PC with the USB cable. On the phone, tap **Allow** when it asks **Allow USB debugging?**
5. On the PC, open the `platform-tools` folder in File Explorer, click the address bar, type `cmd` and press **Enter**.
6. In Snoozer, open **Settings** › **Follow Bedtime** › **One-time setup** and tap **Copy commands**. Send the two commands to your PC (for example by email), or type them over:

   ```
   adb shell pm grant io.github.impca201.snoozer android.permission.DUMP
   adb shell pm grant io.github.impca201.snoozer android.permission.PACKAGE_USAGE_STATS
   ```

7. Paste or type each command in the black window and press **Enter**. If nothing is printed, it worked.
8. In Snoozer, tap **Check now**, then switch on **Snooze during Bedtime** (the switch stays grey until the commands have run). It should say **Bedtime is off right now** (or on, if it is).

If it says **Can't read the Bedtime status on this phone.**, this Android version stores the information differently. Please [open an issue](https://github.com/impca201/snoozer/issues) and mention your phone and Android version. You need to repeat the setup after reinstalling Snoozer, but not after updates.

#### Shortcuts

Open **Shortcuts**. It has three parts:

- **Quick Settings tile**: on Android 13 and newer, tap **Add tile** and then **Add tile** again in the confirmation. On older phones, follow the four steps on screen.
- **Launcher shortcut**: touch and hold the Snoozer icon on your home screen and tap **Snooze on/off**.
- **Gesture: tap the back of your phone** (Pixel only): go to **Settings** › **System** › **Gestures** › **Quick Tap to start actions**, switch it on, choose **Open app**, tap the settings icon next to **Snoozer** and pick **Snooze on/off**. Now double-tapping the back of the phone switches snoozing on or off.

#### Tasker plugin

Open **Tasker plugin**. Snoozer has no schedules or triggers of its own, so it needs no background service; the automation app Tasker does that part. The section explains how to add the Snoozer action to a Tasker task. Snoozer also works with MacroDroid, Automate and other apps that support Tasker plug-ins.

## Chapter 7: Prepare the app for the Google Play Store

Skip this chapter if you only want Snoozer on your own phone.

Apps on Google Play must be signed with your personal **upload key**: a small file that proves updates come from you. You create it once and keep it safe for the lifetime of the app.

### Create your upload key

1. Start **Android Studio**. On the **Welcome to Android Studio** window, click **Open**, select the project folder (the one that contains `build.bat`) and click **OK**. If asked whether you trust the project, click **Trust Project**. Wait until the progress bar at the bottom disappears (the first time this takes a few minutes).
2. In the menu bar at the top, click **Build** › **Generate Signed Bundle/APK**.
3. Select **Android App Bundle** and click **Next**.
4. Below the **Key store path** field, click **Create new**.
5. In the **New Key Store** window:
   - **Key store path**: click the folder icon, choose a safe folder **outside** the project folder (for example `Documents`), type the file name `snoozer-upload.jks` and click **OK**.
   - **Password** and **Confirm**: choose a strong password and write it down.
   - **Alias**: type `upload`.
   - Key **Password** and **Confirm**: enter the same password again.
   - **Validity (years)**: leave it at 25 or higher.
   - **First and Last Name**: type your name. The other certificate fields are optional.
6. Click **OK**, then click **Cancel** to close the wizard. Your key file now exists. Back it up (for example to a USB stick). If you lose it, you can't publish updates without contacting Google.
7. Close Android Studio.

### Tell the build where your key is

1. In the project folder, right-click an empty area and click **New** › **Text Document**. Name it `keystore.properties` (replace the `.txt` part too) and confirm the warning about changing the extension with **Yes**. If you can't see `.txt` at the end of file names, first click **View** › **Show** › **File name extensions** in File Explorer.
2. Right-click `keystore.properties`, click **Open with** › **Notepad**, and type these four lines. Use your own folder and password, and write the path with forward slashes:

   ```properties
   storeFile=C:/Users/YourName/Documents/snoozer-upload.jks
   storePassword=your-password
   keyAlias=upload
   keyPassword=your-password
   ```

3. Click **File** › **Save** and close Notepad. This file stays on your PC only and is never uploaded to GitHub.

### Build the Play Store file

1. Open a Command Prompt in the project folder as in [Chapter 3](#chapter-3-build-the-app), steps 1 and 2.
2. Type `build.bat bundle` and press **Enter**.
3. When you see `[OK] Done.`, open **app** › **build** › **outputs** › **bundle** › **release**. The file **app-release.aab** is what you upload to Google Play Console.

Before publishing, host [PRIVACY.md](PRIVACY.md) somewhere public (Google Play asks for its web address) and prepare the store listing.

## Chapter 8: Troubleshooting

**`[ERROR] Java not found.`**
Android Studio isn't installed, or was installed to a different folder. Repeat [Chapter 1](#chapter-1-install-android-studio-on-your-pc) with the default settings, then try again in a **new** Command Prompt window.

**`[ERROR] Android SDK not found.`**
The Android Studio Setup Wizard didn't finish. Start Android Studio and complete the wizard (Chapter 1, steps 6 to 9), then try again.

**`[FAILED] Build failed.`**
Scroll up in the black window to the first line that starts with `* What went wrong:` and read the lines below it. A common cause is no internet connection during the first build: check your connection and type `build.bat` again. If it keeps failing, copy the text from `* What went wrong:` and [open an issue](https://github.com/impca201/snoozer/issues).

**The big button stays grey.**
Notification access is off. Follow [Chapter 5](#chapter-5-set-up-snoozer-on-your-phone), including the part about the grey switch.

**My phone isn't shown in File Explorer.**
Unlock the phone, then swipe down and choose **File transfer** again (Chapter 4, step 2). Try another USB cable if needed: some cables can only charge.

## For developers

<details>
<summary>Architecture, tech stack and project layout</summary>

- **Stack**: Kotlin, Jetpack Compose, Material 3 (dynamic colour, Expressive shapes via `androidx.graphics:graphics-shapes`), DataStore. AGP 9.4.1 with built-in Kotlin 2.4, Gradle 9.8, compile/target SDK 37, min SDK 26.
- **How snoozing works**: a `NotificationListenerService` snoozes every clearable notification in 7-day chunks and re-snoozes anything that resurfaces. Android has no public "unsnooze", so restoring re-snoozes each stored key for one second. All entry points (UI, tile, shortcut, Tasker) only write the DataStore switch; the listener observes it.

```
app/src/main/java/io/github/impca201/snoozer/
├── SnoozerApp.kt              Application: app scope, keeps tile and listener in sync
├── data/SnoozeRepository.kt   DataStore: snooze switch, toast setting, snoozed keys
├── service/                   Notification listener, toast helper, access helpers
├── tile/                      Quick Settings tile
├── shortcut/                  Invisible activity behind the launcher shortcut
├── tasker/                    Tasker/Locale plug-in (edit screen and fire receiver)
└── ui/                        Compose UI: home, guides, theme
```

Other `build.bat` targets: `release`, `install` (needs USB debugging), `test`, `lint`, `all`, `clean`. On macOS or Linux, use `./gradlew assembleDebug`.

</details>

## Sources

- Install Android Studio: https://developer.android.com/studio/install
- Download Android Studio: https://developer.android.com/studio
- Sign your app and create an upload key: https://developer.android.com/studio/publish/app-signing
- Notification listener (how Snoozer snoozes): https://developer.android.com/reference/android/service/notification/NotificationListenerService
- Quick Settings tiles: https://developer.android.com/develop/ui/views/quicksettings-tiles
- App shortcuts: https://developer.android.com/develop/ui/views/launch/shortcuts
- Tasker plug-ins: https://tasker.joaoapps.com/plugins.html
- Google Play Console: https://play.google.com/console
