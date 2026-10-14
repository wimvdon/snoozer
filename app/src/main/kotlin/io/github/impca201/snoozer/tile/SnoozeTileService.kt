package io.github.impca201.snoozer.tile

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import io.github.impca201.snoozer.LoggingExceptionHandler
import io.github.impca201.snoozer.R
import io.github.impca201.snoozer.service.NotificationAccess
import io.github.impca201.snoozer.snoozerApp
import io.github.impca201.snoozer.ui.Destination
import io.github.impca201.snoozer.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class SnoozeTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate + LoggingExceptionHandler)
    private var listeningJob: Job? = null

    override fun onTileAdded() {
        super.onTileAdded()
        snoozerApp.repository.snoozeEnabled.value
            ?.let(::render)
    }

    override fun onStartListening() {
        super.onStartListening()
        // Render the cached value immediately, then keep following changes while visible.
        snoozerApp.repository.snoozeEnabled.value
            ?.let(::render)
        listeningJob?.cancel()
        listeningJob =
            scope.launch {
                snoozerApp.repository.snoozeEnabled
                    .filterNotNull()
                    .collect { render(it) }
            }
    }

    override fun onStopListening() {
        listeningJob?.cancel()
        listeningJob = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        if (!NotificationAccess.isGranted(this)) {
            openApp(Destination.AccessGuide)
            return
        }
        // Show the new state right away instead of waiting for the write to round-trip.
        val app = snoozerApp
        app.repository.snoozeEnabled.value
            ?.let { render(!it) }
        // Use the app scope so the write completes even if the tile is unbound right away.
        app.appScope.launch { app.repository.toggleSnooze() }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun render(snoozed: Boolean) {
        val tile = qsTile ?: return
        val hasAccess = NotificationAccess.isGranted(this)
        tile.label = getString(R.string.tile_label)
        // Unavailable tiles cannot be clicked, so without access keep it inactive and let a
        // click open the setup guide instead.
        tile.state = if (hasAccess && snoozed) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        val status =
            getString(
                when {
                    !hasAccess -> R.string.tile_subtitle_setup
                    snoozed -> R.string.tile_subtitle_on
                    else -> R.string.tile_subtitle_off
                },
            )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) tile.subtitle = status
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) tile.stateDescription = status
        tile.updateTile()
    }

    // The Intent overload is only reached below Android 14, where it is still supported.
    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openApp(destination: Destination) {
        val intent = MainActivity.intent(this, destination).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent =
                PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
