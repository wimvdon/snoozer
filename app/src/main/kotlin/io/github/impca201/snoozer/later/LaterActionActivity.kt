package io.github.impca201.snoozer.later

import android.app.Activity
import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import io.github.impca201.snoozer.R
import io.github.impca201.snoozer.data.LaterItem
import io.github.impca201.snoozer.service.CurrentNotifications
import io.github.impca201.snoozer.snoozerApp
import kotlinx.coroutines.launch

/**
 * Invisible activity behind the widget's rows: opens a saved notification and takes it off the list.
 *
 * It is a (transparent) activity rather than a broadcast because opening a notification starts
 * another app's screen, which Android only allows from an app that is in the foreground.
 */
class LaterActionActivity : Activity() {
    private var handled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handled = savedInstanceState?.getBoolean(STATE_HANDLED) ?: false
    }

    // Resumed, the activity counts as visible, so it may start the notification's own screen.
    override fun onResume() {
        super.onResume()
        if (!handled) {
            handled = true
            handle(intent)
        }
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_HANDLED, handled)
    }

    private fun handle(intent: Intent?) {
        val key = intent?.getStringExtra(EXTRA_KEY) ?: return
        // The link is lost after the app restarted, or no longer works: open the notification's app.
        val opened = sendContentIntent(key) || openApp(intent.getStringExtra(EXTRA_PACKAGE))
        if (!opened) {
            Toast.makeText(this, R.string.later_open_failed, Toast.LENGTH_SHORT).show()
            return // Keep it on the list.
        }
        CurrentNotifications.forget(key)
        val app = snoozerApp
        app.appScope.launch { app.repository.removeLater(key) }
    }

    private fun sendContentIntent(key: String): Boolean {
        val pendingIntent = CurrentNotifications.contentIntent(key) ?: return false
        return try {
            pendingIntent.send(this, 0, null, null, null, null, backgroundStartOptions())
            true
        } catch (_: PendingIntent.CanceledException) {
            false
        }
    }

    /** Android 14+: lend this visible activity's right to start screens to the notification's app. */
    private fun backgroundStartOptions(): Bundle? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return null
        val mode =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_IF_VISIBLE
            } else {
                @Suppress("DEPRECATION")
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
            }
        return ActivityOptions.makeBasic().setPendingIntentBackgroundActivityStartMode(mode).toBundle()
    }

    private fun openApp(packageName: String?): Boolean {
        val launch = packageName?.let { packageManager.getLaunchIntentForPackage(it) } ?: return false
        return try {
            startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (_: ActivityNotFoundException) {
            false // Uninstalled meanwhile.
        } catch (_: SecurityException) {
            false
        }
    }

    companion object {
        private const val EXTRA_KEY = "io.github.impca201.snoozer.extra.LATER_KEY"
        private const val EXTRA_PACKAGE = "io.github.impca201.snoozer.extra.LATER_PACKAGE"
        private const val STATE_HANDLED = "handled"

        /** Filled into the widget's click template for one row. */
        fun fillIn(item: LaterItem): Intent =
            Intent()
                .putExtra(EXTRA_KEY, item.key)
                .putExtra(EXTRA_PACKAGE, item.packageName)
    }
}
