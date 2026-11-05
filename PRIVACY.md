# Snoozer privacy policy

_Last updated: 5 November 2026 (notifications you save for later)_

Snoozer is designed to collect nothing.

## What the app accesses

Snoozer asks for **notification access** (Android's notification listener permission). It uses
this access only to:

- snooze notifications while snoozing is switched on, and bring them back when it is switched off;
- read the name of the app that posted a notification, only to show the optional
  "Notification from {app} snoozed" message;
- read the app name, title and text of the notifications in your notification shade, only to
  show them in the "Save for later" list when you open it, and to show the ones you saved in the
  "Do later" widget.

## What the app stores

On your device only, Snoozer stores:

- whether snoozing is switched on;
- your settings: the toast message, the persistent notification, the optional mode and the
  name you gave it, and Follow Bedtime;
- for Follow Bedtime, whether Bedtime was on at the last check and whether Snoozer started the
  current snooze because of it;
- the system identifiers ("keys") of notifications it snoozed, so they can be restored. These
  keys contain the posting app's package name and a notification ID, never notification content;
- for each notification you save for later ("Do later" widget): its key, the app's package name
  and name, its title and text, and the time it arrived. Only the notifications you pick yourself
  are stored, at most 100, and an item is deleted as soon as you open it or mark it done.

Your settings (theme, toast message, persistent notification, mode and its name) are included in
Android's own backup to your Google account, if you turned that on, and in a transfer to a new
phone, so they come back after a reset or on a new phone. Everything else above (whether snoozing
is on, Follow Bedtime, the notification keys and the notifications saved for later) is excluded from backups and transfers. All of it
is deleted from the phone when you uninstall the app.

## What the app shares

Nothing. Snoozer does not request the internet permission, contains no analytics, advertising or
crash-reporting SDKs, and cannot send data anywhere.

## Follow Bedtime (optional)

If you grant the two permissions for **Follow Bedtime** with adb and switch it on, Snoozer asks
Android, on your device, which modes are currently active. It only looks at that list to see
whether Bedtime is on; nothing from it is stored or sent anywhere.

## Automation apps

If you use the Tasker plug-in, the automation app you choose stores and runs the action you
configured (Snooze, Unsnooze or Toggle). No other data is exchanged.

## Contact

Questions? Open an issue at https://github.com/wimvdon/snoozer/issues.
