package io.github.impca201.snoozer.service

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/** Helpers around the "Notification access" special permission. */
object NotificationAccess {
    // Undocumented but long-standing Settings extras that scroll to and highlight our entry.
    private const val EXTRA_FRAGMENT_ARG_KEY = ":settings:fragment_args_key"
    private const val EXTRA_SHOW_FRAGMENT_ARGS = ":settings:show_fragment_args"

    fun isGranted(context: Context): Boolean = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    fun listenerComponent(context: Context): ComponentName = ComponentName(context, SnoozeListenerService::class.java)

    /** Opens the most specific settings page available, falling back gracefully. */
    fun openSettings(context: Context) {
        val component = listenerComponent(context).flattenToString()
        val candidates =
            buildList {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    add(
                        Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                            .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, component),
                    )
                }
                add(
                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        .putExtra(EXTRA_FRAGMENT_ARG_KEY, component)
                        .putExtra(EXTRA_SHOW_FRAGMENT_ARGS, Bundle().apply { putString(EXTRA_FRAGMENT_ARG_KEY, component) }),
                )
                add(Intent(Settings.ACTION_SETTINGS))
            }
        startFirstResolvable(context, candidates)
    }

    /** App info page; needed for "Allow restricted settings" on sideloaded installs (API 33+). */
    fun openAppInfo(context: Context) {
        startFirstResolvable(
            context,
            listOf(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                Intent(Settings.ACTION_SETTINGS),
            ),
        )
    }

    private fun startFirstResolvable(
        context: Context,
        intents: List<Intent>,
    ) {
        for (intent in intents) {
            try {
                if (context !is android.app.Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            } catch (_: ActivityNotFoundException) {
                // Try the next, less specific, screen.
            } catch (_: SecurityException) {
                // Some OEM builds protect the detail screen; try the next one.
            }
        }
    }
}
