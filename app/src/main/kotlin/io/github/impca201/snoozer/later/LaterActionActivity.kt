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
import io.github.impca201.snoozer.data.LaterRelease
import io.github.impca201.snoozer.service.CurrentNotifications
import io.github.impca201.snoozer.snoozerApp
import kotlinx.coroutines.launch

/**
 * Invisible activity behind the widget's rows: opens a saved notification or marks it done.
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
        when (intent.getStringExtra(EXTRA_ACTION)) {
            ACTION_DONE -> release(key, LaterRelease.Dismiss)
            ACTION_OPEN ->
                if (sendContentIntent(key)) {
                    release(key, LaterRelease.Dismiss)
                } else {
                    // The link was lost (the app restarted) or no longer works: put the
                    // notification back in the shade and open its app.
                    release(key, LaterRelease.Restore)
                    openApp(intent.getStringExtra(EXTRA_PACKAGE))
                }
        }
    }

    private fun release(
        key: String,
        how: LaterRelease,
    ) {
        val app = snoozerApp
        app.appScope.launch { app.repository.releaseLater(key, how) }
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

    private fun openApp(packageName: String?) {
        val launch = packageName?.let { packageManager.getLaunchIntentForPackage(it) }
        try {
            if (launch != null) {
                startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            }
        } catch (_: ActivityNotFoundException) {
            // Uninstalled meanwhile; fall through to the message.
        } catch (_: SecurityException) {
            // Not startable from here; fall through to the message.
        }
        Toast.makeText(this, R.string.later_restored, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val ACTION_OPEN = "open"
        const val ACTION_DONE = "done"

        private const val EXTRA_KEY = "io.github.impca201.snoozer.extra.LATER_KEY"
        private const val EXTRA_PACKAGE = "io.github.impca201.snoozer.extra.LATER_PACKAGE"
        private const val EXTRA_ACTION = "io.github.impca201.snoozer.extra.LATER_ACTION"
        private const val STATE_HANDLED = "handled"

        /** Filled into the widget's click template for one row's tap or checkbox. */
        fun fillIn(
            item: LaterItem,
            action: String,
        ): Intent =
            Intent()
                .putExtra(EXTRA_KEY, item.key)
                .putExtra(EXTRA_PACKAGE, item.packageName)
                .putExtra(EXTRA_ACTION, action)
    }
}
