package io.github.impca201.snoozer.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.impca201.snoozer.R
import io.github.impca201.snoozer.snoozerApp
import io.github.impca201.snoozer.ui.Destination
import io.github.impca201.snoozer.ui.MainActivity
import kotlinx.coroutines.launch

/**
 * The persistent, silent "Snoozing notifications" notification. It puts Snoozer's icon in the
 * status bar while snoozing and offers an Unsnooze button. It is a plain ongoing notification
 * (no foreground service); if the user swipes it away, [SnoozeListenerService] reposts it.
 */
object SnoozeIndicator {
    const val NOTIFICATION_ID = 1
    private const val CHANNEL_ID = "snooze_status"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.indicator_channel_name),
                // LOW: silent, but (unlike MIN) still shows the status bar icon.
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.indicator_channel_description)
                setShowBadge(false)
            }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    /** True when the app may post notifications (runtime permission on Android 13+). */
    fun canPost(context: Context): Boolean {
        val permitted =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        return permitted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** Shows or hides the indicator so it matches the current state. Safe to call often. */
    @SuppressLint("MissingPermission") // Checked in canPost(); SecurityException is caught too.
    fun update(
        context: Context,
        visible: Boolean,
    ) {
        val manager = NotificationManagerCompat.from(context)
        if (!visible) {
            manager.cancel(NOTIFICATION_ID)
            return
        }
        if (!canPost(context)) return
        try {
            manager.notify(NOTIFICATION_ID, build(context))
        } catch (_: SecurityException) {
            // Permission revoked between the check and the call.
        }
    }

    private fun build(context: Context): android.app.Notification {
        val openApp =
            PendingIntent.getActivity(
                context,
                0,
                MainActivity.intent(context, Destination.Home),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        val unsnooze =
            PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, UnsnoozeReceiver::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        return NotificationCompat
            .Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tile)
            .setContentTitle(context.getString(R.string.indicator_title))
            .setContentText(context.getString(R.string.indicator_text))
            .setContentIntent(openApp)
            .addAction(R.drawable.ic_notifications, context.getString(R.string.toggle_unsnooze), unsnooze)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setLocalOnly(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }
}

/** Handles the indicator's Unsnooze button. Not exported: only our PendingIntent can reach it. */
class UnsnoozeReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val app = context.snoozerApp
        val pendingResult = goAsync()
        app.appScope.launch {
            try {
                app.repository.setSnoozeEnabled(false)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
