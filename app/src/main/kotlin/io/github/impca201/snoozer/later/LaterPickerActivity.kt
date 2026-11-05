package io.github.impca201.snoozer.later

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.impca201.snoozer.R
import io.github.impca201.snoozer.data.NotificationSnapshot
import io.github.impca201.snoozer.service.CurrentNotifications
import io.github.impca201.snoozer.service.NotificationAccess
import io.github.impca201.snoozer.service.SnoozeIndicator
import io.github.impca201.snoozer.snoozerApp
import io.github.impca201.snoozer.ui.Destination
import io.github.impca201.snoozer.ui.MainActivity
import io.github.impca201.snoozer.ui.later.LaterSheet
import io.github.impca201.snoozer.ui.theme.SnoozerAppTheme
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * The "Save for later" bottom sheet over whatever is on screen, opened from the widget's + and
 * from the launcher shortcut. Picking a notification puts it on the list; the listener takes it off the shade.
 */
class LaterPickerActivity : ComponentActivity() {
    private val hasAccess = mutableStateOf(false)

    // Android 13+: Snoozer's notifications for saved items need the notification permission.
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            snoozerApp.refreshLaterViews()
            finish()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        hasAccess.value = NotificationAccess.isGranted(this)
        val repository = snoozerApp.repository
        val candidates =
            CurrentNotifications.active.map(NotificationSnapshot::candidates)
        setContent {
            SnoozerAppTheme {
                val list by candidates.collectAsStateWithLifecycle(emptyList())
                val snoozing by repository.snoozeEnabled.collectAsStateWithLifecycle()
                LaterSheet(
                    hasAccess = hasAccess.value,
                    snoozing = snoozing == true,
                    candidates = list,
                    onPick = ::save,
                    onAllowAccess = {
                        startActivity(MainActivity.intent(this, Destination.AccessGuide).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        finish()
                    },
                    onDismiss = ::finish,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Access may have been granted or revoked in Settings meanwhile.
        hasAccess.value = NotificationAccess.isGranted(this)
    }

    private fun save(notification: NotificationSnapshot) {
        val app = snoozerApp
        app.appScope.launch { app.repository.addLater(notification.toLaterItem()) }
        Toast.makeText(app, R.string.later_saved, Toast.LENGTH_SHORT).show()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !SnoozeIndicator.canPost(this)) {
            // Asked once per save; after two refusals Android stops showing the dialog by itself.
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            finish()
        }
    }

    companion object {
        /** Used by the launcher shortcut in `res/xml/shortcuts.xml`; never rename it. */
        const val ACTION_SAVE_FOR_LATER = "io.github.impca201.snoozer.action.SAVE_FOR_LATER"

        fun intent(context: Context): Intent =
            Intent(context, LaterPickerActivity::class.java)
                .setAction(ACTION_SAVE_FOR_LATER)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
}
