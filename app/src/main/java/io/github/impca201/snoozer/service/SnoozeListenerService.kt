package io.github.impca201.snoozer.service

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.os.BundleCompat
import io.github.impca201.snoozer.LoggingExceptionHandler
import io.github.impca201.snoozer.snoozerApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/**
 * Snoozes every incoming (and already visible) notification while snoozing is switched on, and
 * brings them all back as soon as it is switched off.
 *
 * The platform has no "snooze forever" or public "unsnooze" call, so:
 *  - notifications are snoozed for [SNOOZE_DURATION_MS] and simply snoozed again if they come
 *    back while snoozing is still on;
 *  - to restore them, each snoozed key is re-snoozed for [RESTORE_DELAY_MS], which makes the
 *    system repost it almost immediately.
 */
class SnoozeListenerService : NotificationListenerService() {
    private val repository get() = snoozerApp.repository

    private var scope: CoroutineScope? = null
    private lateinit var toaster: SnoozeToaster

    /** In-memory mirror of the persisted keys; used to avoid repeat toasts on re-snooze. */
    private val trackedKeys = HashSet<String>()

    override fun onCreate() {
        super.onCreate()
        toaster = SnoozeToaster(this)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        scope?.cancel()
        val connectedScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate + LoggingExceptionHandler)
        scope = connectedScope
        connectedScope.launch {
            trackedKeys.clear()
            trackedKeys += repository.snoozedKeys()
            repository.snoozeEnabled.filterNotNull().collect { enabled ->
                if (enabled) snoozeAllActive() else restoreSnoozed()
            }
        }
    }

    override fun onListenerDisconnected() {
        scope?.cancel()
        scope = null
        super.onListenerDisconnected()
    }

    override fun onDestroy() {
        scope?.cancel()
        scope = null
        toaster.cancel()
        super.onDestroy()
    }

    override fun onNotificationPosted(
        sbn: StatusBarNotification?,
        rankingMap: RankingMap?,
    ) {
        if (sbn == null || scope == null) return
        if (repository.snoozeEnabled.value == true) snooze(sbn)
    }

    private fun snoozeAllActive() {
        val active: Array<StatusBarNotification>? =
            try {
                activeNotifications
            } catch (_: SecurityException) {
                null // Listener not (or no longer) connected.
            }
        active?.forEach(::snooze)
    }

    private fun snooze(sbn: StatusBarNotification) {
        if (!shouldSnooze(sbn)) return
        val key = sbn.key
        try {
            snoozeNotification(key, SNOOZE_DURATION_MS)
        } catch (_: RuntimeException) {
            // SecurityException when access was just revoked; some OEM builds throw others.
            return
        }
        val firstTime = trackedKeys.add(key)
        if (firstTime) {
            // App scope: the write must finish even if the listener disconnects right now.
            snoozerApp.appScope.launch { repository.addSnoozedKey(key) }
            if (repository.toastEnabled.value) toaster.announce(appLabel(sbn))
        }
    }

    private suspend fun restoreSnoozed() {
        val keys = repository.takeSnoozedKeys() + trackedKeys
        trackedKeys.clear()
        // A key can belong to a notification that is visible again (the app cancelled and
        // re-posted it, or the phone restarted). Re-snoozing that one would hide and re-alert it.
        val visible =
            try {
                activeNotifications?.mapTo(HashSet()) { it.key }.orEmpty()
            } catch (_: SecurityException) {
                emptySet()
            }
        for (key in keys - visible) {
            try {
                snoozeNotification(key, RESTORE_DELAY_MS)
            } catch (_: SecurityException) {
                return // Lost access; the system will repost them when their snooze expires.
            } catch (_: RuntimeException) {
                // The notification no longer exists; nothing to restore.
            }
        }
    }

    /**
     * Ongoing / non-dismissible notifications (calls, media, navigation, alarms, foreground
     * services) are never snoozed: hiding them could hide important controls and the system
     * often refuses anyway. Our own notifications are left alone too.
     */
    private fun shouldSnooze(sbn: StatusBarNotification): Boolean = sbn.packageName != packageName && sbn.isClearable

    private fun appLabel(sbn: StatusBarNotification): CharSequence {
        val pm = packageManager
        return try {
            val info =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getApplicationInfo(sbn.packageName, PackageManager.ApplicationInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getApplicationInfo(sbn.packageName, 0)
                }
            pm.getApplicationLabel(info)
        } catch (_: PackageManager.NameNotFoundException) {
            // Package visibility may hide the app; the posting app's info travels with the notification.
            BundleCompat
                .getParcelable(sbn.notification.extras, EXTRA_APP_INFO, ApplicationInfo::class.java)
                ?.loadLabel(pm)
                ?: sbn.packageName
        }
    }

    private companion object {
        /** Re-applied whenever a notification resurfaces while snoozing is on. */
        const val SNOOZE_DURATION_MS = 7L * 24 * 60 * 60 * 1000

        /** Re-snoozing for this short period is how notifications are brought back. */
        const val RESTORE_DELAY_MS = 1_000L

        /** Notification.EXTRA_BUILDER_APPLICATION_INFO (hidden constant). */
        const val EXTRA_APP_INFO = "android.appInfo"
    }
}
