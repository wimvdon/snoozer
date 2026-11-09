package io.github.impca201.snoozer.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import io.github.impca201.snoozer.ui.theme.SnoozerAppTheme

class MainActivity : ComponentActivity() {
    /** A screen requested through an intent (tile, shortcut), consumed by the UI once shown. */
    private val requestedDestination = mutableStateOf<Destination?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) requestedDestination.value = destinationFrom(intent)

        setContent {
            SnoozerAppTheme {
                SnoozerNavigation(
                    requested = requestedDestination.value,
                    onRequestHandled = { requestedDestination.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestedDestination.value = destinationFrom(intent)
    }

    companion object {
        private const val EXTRA_DESTINATION = "io.github.impca201.snoozer.extra.DESTINATION"

        /** NotificationManager.ACTION_AUTOMATIC_ZEN_RULE (constant inlined; class is API 24+). */
        private const val ACTION_AUTOMATIC_ZEN_RULE = "android.app.action.AUTOMATIC_ZEN_RULE"

        fun intent(
            context: Context,
            destination: Destination,
        ): Intent =
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_DESTINATION, destination.name)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)

        private fun destinationFrom(intent: Intent?): Destination? {
            // Settings › Modes › Snoozer opens the app through this action.
            if (intent?.action == ACTION_AUTOMATIC_ZEN_RULE) return Destination.Settings
            val name = intent?.getStringExtra(EXTRA_DESTINATION) ?: return null
            return Destination.entries.firstOrNull { it.name == name }
        }
    }
}
