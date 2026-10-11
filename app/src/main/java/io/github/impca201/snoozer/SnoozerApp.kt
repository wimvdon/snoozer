package io.github.impca201.snoozer

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.quicksettings.TileService
import android.util.Log
import io.github.impca201.snoozer.data.SnoozeRepository
import io.github.impca201.snoozer.service.NotificationAccess
import io.github.impca201.snoozer.service.SnoozeIndicator
import io.github.impca201.snoozer.service.SnoozeListenerService
import io.github.impca201.snoozer.tile.SnoozeTileService
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class SnoozerApp : Application() {
    /** Process-wide scope for work that must outlive a single screen, receiver or tile click. */
    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default + LoggingExceptionHandler)

    val repository: SnoozeRepository by lazy { SnoozeRepository(this, appScope) }

    override fun onCreate() {
        super.onCreate()
        SnoozeIndicator.createChannel(this)
        appScope.launch {
            repository.snoozeEnabled.filterNotNull().collect { onSnoozeStateChanged() }
        }
        appScope.launch {
            combine(repository.snoozeEnabled.filterNotNull(), repository.indicatorEnabled) { _, _ -> }
                .collect { refreshIndicator() }
        }
    }

    /** True while the persistent "Snoozing notifications" notification should be visible. */
    fun indicatorWanted(): Boolean =
        repository.snoozeEnabled.value == true &&
            repository.indicatorEnabled.value &&
            NotificationAccess.isGranted(this)

    /**
     * Shows or hides the persistent notification. Also called when the app resumes, because
     * notification permission or access may have changed in Settings meanwhile.
     */
    fun refreshIndicator() {
        SnoozeIndicator.update(this, indicatorWanted())
    }

    /** Keeps every surface (tile, listener) in sync no matter where the toggle came from. */
    private fun onSnoozeStateChanged() {
        runCatching {
            TileService.requestListeningState(
                this,
                ComponentName(this, SnoozeTileService::class.java),
            )
        }
        if (NotificationAccess.isGranted(this)) {
            // No-op when the listener is already bound; revives it if the system dropped it.
            runCatching {
                NotificationListenerService.requestRebind(
                    ComponentName(this, SnoozeListenerService::class.java),
                )
            }
        }
    }
}

/**
 * Background work here is best effort (a failed preference write must not crash the app, e.g.
 * when storage is full), so failures are logged instead of propagated.
 */
val LoggingExceptionHandler =
    CoroutineExceptionHandler { _, error ->
        Log.w("Snoozer", "Background task failed", error)
    }

val Context.snoozerApp: SnoozerApp
    get() = applicationContext as SnoozerApp
