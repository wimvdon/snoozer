package io.github.impca201.snoozer.service

import android.app.PendingIntent
import io.github.impca201.snoozer.data.NotificationSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory view the listener publishes for the "Save for later" sheet and the widget's links.
 * Only the listener writes it. Nothing here is stored: after a restart the listener fills it again.
 */
object CurrentNotifications {
    private val mutableActive = MutableStateFlow<List<NotificationSnapshot>>(emptyList())

    /** The notifications in the shade right now (empty while the listener is not connected). */
    val active: StateFlow<List<NotificationSnapshot>> = mutableActive.asStateFlow()

    /**
     * The tap action of each saved notification. A PendingIntent can't be written to disk, so
     * after the app process restarts a saved notification is opened by restoring it instead.
     */
    private val contentIntents = ConcurrentHashMap<String, PendingIntent>()

    internal fun publish(snapshots: List<NotificationSnapshot>) {
        mutableActive.value = snapshots
    }

    internal fun rememberContentIntent(
        key: String,
        intent: PendingIntent?,
    ) {
        if (intent != null) contentIntents[key] = intent
    }

    fun contentIntent(key: String): PendingIntent? = contentIntents[key]

    fun forget(key: String) {
        contentIntents.remove(key)
    }
}
