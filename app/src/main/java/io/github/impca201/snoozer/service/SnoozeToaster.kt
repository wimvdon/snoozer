package io.github.impca201.snoozer.service

import android.content.Context
import android.os.SystemClock
import android.widget.Toast
import io.github.impca201.snoozer.R

/**
 * Shows "Notification from {app} snoozed" toasts. Bursts (for example when snoozing is switched
 * on with many notifications present) are merged into one toast, which also keeps us below the
 * platform's toast rate limit: several notifications from one app still name that app, and
 * notifications from several apps become "Notifications from N apps snoozed".
 *
 * Must be used from the main thread.
 */
class SnoozeToaster(
    private val context: Context,
) {
    private var toast: Toast? = null
    private val burstPackages = LinkedHashSet<String>()
    private var lastShownAt = 0L

    fun announce(
        packageName: String,
        appName: CharSequence,
    ) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastShownAt >= BURST_WINDOW_MS) burstPackages.clear()
        burstPackages += packageName
        lastShownAt = now

        val appCount = burstPackages.size
        val text =
            if (appCount == 1) {
                context.getString(R.string.toast_snoozed_from_app, appName)
            } else {
                context.resources.getQuantityString(R.plurals.toast_snoozed_apps, appCount, appCount)
            }
        toast?.cancel()
        toast = Toast.makeText(context, text, Toast.LENGTH_SHORT).also { it.show() }
    }

    fun cancel() {
        toast?.cancel()
        toast = null
    }

    private companion object {
        const val BURST_WINDOW_MS = 2_000L
    }
}
