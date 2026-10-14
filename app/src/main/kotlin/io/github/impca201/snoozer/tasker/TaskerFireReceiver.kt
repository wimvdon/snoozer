package io.github.impca201.snoozer.tasker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.impca201.snoozer.snoozerApp
import io.github.impca201.snoozer.tasker.TaskerContract.Command
import kotlinx.coroutines.launch

/** Runs the action configured in [TaskerEditActivity] when Tasker fires the plug-in. */
class TaskerFireReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != TaskerContract.ACTION_FIRE_SETTING) return
        val bundle =
            try {
                intent.getBundleExtra(TaskerContract.EXTRA_BUNDLE)
            } catch (_: RuntimeException) {
                null
            }
        val command = TaskerContract.commandFrom(bundle) ?: return

        val app = context.snoozerApp
        val pendingResult = goAsync()
        app.appScope.launch {
            try {
                when (command) {
                    Command.SNOOZE -> app.repository.setSnoozeEnabled(true)
                    Command.UNSNOOZE -> app.repository.setSnoozeEnabled(false)
                    Command.TOGGLE -> app.repository.toggleSnooze()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
