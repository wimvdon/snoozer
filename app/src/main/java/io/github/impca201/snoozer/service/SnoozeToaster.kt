package io.github.impca201.snoozer.service

import android.content.Context
import android.os.SystemClock
import android.widget.Toast
import io.github.impca201.snoozer.R

/**
 * Shows "Notification from {app} snoozed" toasts. Bursts (for example when snoozing is switched
 * on with many notifications present) are merged into a single counting toast, which also keeps
 * us below the platform's toast rate limit.
 *
 * Must be used from the main thread.
 */
class SnoozeToaster(
    private val context: Context,
) {
    private var toast: Toast? = null
    private var burstCount = 0
    private var lastShownAt = 0L

    fun announce(appName: CharSequence) {
        val now = SystemClock.elapsedRealtime()
        burstCount = if (now - lastShownAt < BURST_WINDOW_MS) burstCount + 1 else 1
        lastShownAt = now

        val text =
            if (burstCount == 1) {
                context.getString(R.string.toast_snoozed_from_app, appName)
            } else {
                context.resources.getQuantityString(R.plurals.toast_snoozed_count, burstCount, burstCount)
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
