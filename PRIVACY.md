# Snoozer privacy policy

_Last updated: 11 October 2026 (stored settings listed in full)_

Snoozer is designed to collect nothing.

## What the app accesses

Snoozer asks for **notification access** (Android's notification listener permission). It uses
this access only to:

- snooze notifications while snoozing is switched on, and bring them back when it is switched off;
- read the name of the app that posted a notification, only to show the optional
  "Notification from {app} snoozed" message.

## What the app stores

On your device only, Snoozer stores:

- whether snoozing is switched on;
- your settings: the toast message, the persistent notification, the optional mode and the
  name you gave it, and Follow Bedtime;
- for Follow Bedtime, whether Bedtime was on at the last check and whether Snoozer started the
  current snooze because of it;
- the system identifiers ("keys") of notifications it snoozed, so they can be restored. These
  keys contain the posting app's package name and a notification ID, never notification content.

This data is excluded from cloud backups and device-to-device transfers and is deleted when you
uninstall the app.

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

Questions? Open an issue at https://github.com/impca201/snoozer/issues.
