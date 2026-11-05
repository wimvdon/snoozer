package io.github.impca201.snoozer.later

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.IconCompat
import io.github.impca201.snoozer.R
import io.github.impca201.snoozer.data.LaterItem
import io.github.impca201.snoozer.service.SnoozeIndicator
import io.github.impca201.snoozer.ui.Destination
import io.github.impca201.snoozer.ui.MainActivity

/**
 * Shows each saved notification in the shade as Snoozer's own silent, collapsed notification,
 * with the original's icon and colour. Tapping one opens the original. Swiping one away only
 * hides it there ([io.github.impca201.snoozer.service.SnoozeListenerService] records that); it
 * stays in the widget.
 */
object LaterNotifications {
    /** All saved notifications share this id; the tag is the original's key. */
    const val NOTIFICATION_ID = 2
    private const val SUMMARY_ID = 3
    private const val CHANNEL_ID = "later"
    private const val GROUP = "io.github.impca201.snoozer.LATER"

    fun createChannel(context: Context) {
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.later_channel_name),
                // MIN: silent, collapsed at the bottom of the shade, no status bar icon.
                NotificationManager.IMPORTANCE_MIN,
            ).apply {
                description = context.getString(R.string.later_channel_description)
                setShowBadge(false)
            }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    /** Makes the shade match [items] minus the ones the user swiped away. Safe to call often. */
    @SuppressLint("MissingPermission") // canPost() checks it; SecurityException is caught too.
    fun sync(
        context: Context,
        items: List<LaterItem>,
        hidden: Set<String>,
    ) {
        val manager = NotificationManagerCompat.from(context)
        val shown = if (SnoozeIndicator.canPost(context)) items.filterNot { it.key in hidden } else emptyList()
        val shownKeys = shown.mapTo(HashSet()) { it.key }
        val posted =
            try {
                context
                    .getSystemService(NotificationManager::class.java)
                    ?.activeNotifications
                    ?.filter { it.id == NOTIFICATION_ID && it.tag != null }
                    ?.mapTo(HashSet()) { it.tag }
                    .orEmpty()
            } catch (_: RuntimeException) {
                emptySet()
            }
        (posted - shownKeys).forEach { manager.cancel(it, NOTIFICATION_ID) }
        if (shown.size < 2) manager.cancel(SUMMARY_ID)
        if (shown.isEmpty()) return
        try {
            // Oldest first, so the newest ends up on top.
            shown.asReversed().forEach { item ->
                try {
                    manager.notify(item.key, NOTIFICATION_ID, item(context, item, ownIcon = true))
                } catch (_: IllegalArgumentException) {
                    // A system that refuses a bitmap small icon: use Snoozer's bell instead.
                    manager.notify(item.key, NOTIFICATION_ID, item(context, item, ownIcon = false))
                }
            }
            if (shown.size >= 2) manager.notify(SUMMARY_ID, summary(context, shown.size))
        } catch (_: SecurityException) {
            // Permission revoked between the check and the call.
        }
    }

    private fun item(
        context: Context,
        item: LaterItem,
        ownIcon: Boolean,
    ): Notification {
        val headline = item.title.ifBlank { item.text }
        val supporting = if (item.title.isBlank()) "" else item.text
        return NotificationCompat
            .Builder(context, CHANNEL_ID)
            .setSmallIcon(
                if (ownIcon) IconCompat.createWithBitmap(LaterIcons.glyph(context, item.key)) else fallbackIcon(context),
            )
            .apply { if (item.color != 0) color = item.color }
            .setContentTitle(headline)
            .setContentText(supporting)
            .setStyle(NotificationCompat.BigTextStyle().bigText(supporting))
            .setSubText(item.appLabel)
            .setWhen(item.postedAt)
            .setShowWhen(true)
            .setContentIntent(openIntent(context, item))
            .setAutoCancel(true)
            .setGroup(GROUP)
            .setSortKey(Long.MAX_VALUE.minus(item.postedAt).toString().padStart(19, '0'))
            .common()
            .build()
    }

    private fun summary(
        context: Context,
        count: Int,
    ): Notification =
        NotificationCompat
            .Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tile)
            .setContentTitle(context.resources.getQuantityString(R.plurals.later_summary, count, count))
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    MainActivity.intent(context, Destination.Home),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            ).setGroup(GROUP)
            .setGroupSummary(true)
            .common()
            .build()

    private fun NotificationCompat.Builder.common(): NotificationCompat.Builder =
        setSilent(true)
            .setOnlyAlertOnce(true)
            .setLocalOnly(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)

    /** Through [LaterActionActivity], which opens the original and takes it off the list. */
    private fun openIntent(
        context: Context,
        item: LaterItem,
    ): PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            LaterActionActivity
                .fillIn(item)
                .setClass(context, LaterActionActivity::class.java)
                // A distinct URI per item, or every notification would share one PendingIntent.
                .setData(Uri.fromParts("snoozer-later", item.key, null))
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun fallbackIcon(context: Context): IconCompat = IconCompat.createWithResource(context, R.drawable.ic_notifications)
}
