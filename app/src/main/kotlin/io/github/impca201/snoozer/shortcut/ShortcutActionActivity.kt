package io.github.impca201.snoozer.shortcut

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import io.github.impca201.snoozer.R
import io.github.impca201.snoozer.service.NotificationAccess
import io.github.impca201.snoozer.snoozerApp
import io.github.impca201.snoozer.ui.Destination
import io.github.impca201.snoozer.ui.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Second step of the launcher shortcut (see [ShortcutActivity]). It toggles snoozing, confirms
 * with a toast and finishes immediately (it uses Theme.NoDisplay, so it must finish in onCreate).
 * It runs in its own task, started without FLAG_ACTIVITY_TASK_ON_HOME, so when it closes Android
 * returns to the app the user was in.
 */
class ShortcutActionActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent?.action == ShortcutActivity.ACTION_TOGGLE) {
            if (NotificationAccess.isGranted(this)) {
                toggle()
            } else {
                startActivity(
                    MainActivity.intent(this, Destination.AccessGuide).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
        finish()
    }

    private fun toggle() {
        val app = snoozerApp
        app.appScope.launch {
            val snoozed = app.repository.toggleSnooze()
            withContext(Dispatchers.Main) {
                Toast
                    .makeText(
                        app,
                        if (snoozed) R.string.feedback_snoozed else R.string.feedback_unsnoozed,
                        Toast.LENGTH_SHORT,
                    ).show()
            }
        }
    }
}
