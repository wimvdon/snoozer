package io.github.impca201.snoozer.shortcut

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.util.Log
import io.github.impca201.snoozer.R

/**
 * Publishes the long-press shortcut from code (a dynamic shortcut) instead of res/xml.
 *
 * Android adds FLAG_ACTIVITY_TASK_ON_HOME to every shortcut declared in the manifest, which puts
 * the home screen behind it: after a Quick Tap the app you were in was gone. A shortcut published
 * here keeps only the flags given below, so you stay in the app you were using.
 */
object Shortcuts {
    /**
     * [force] re-publishes (for a new label after a language change); without it, only when the
     * shortcut is missing, because Android limits how often an app in the background may do this.
     */
    fun publish(
        context: Context,
        force: Boolean = false,
    ) {
        val manager = context.getSystemService(ShortcutManager::class.java) ?: return
        if (!force && ToggleShortcut.isPublished(manager.dynamicShortcuts.map { it.id })) return
        val shortcut =
            ShortcutInfo
                .Builder(context, ToggleShortcut.ID)
                .setShortLabel(context.getString(R.string.shortcut_toggle_short))
                .setLongLabel(context.getString(R.string.shortcut_toggle_long))
                .setIcon(Icon.createWithResource(context, R.drawable.ic_shortcut_toggle))
                .setIntent(
                    Intent(ShortcutActivity.ACTION_TOGGLE)
                        .setClass(context, ShortcutActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                ).build()
        runCatching { manager.dynamicShortcuts = listOf(shortcut) }
            .onFailure { Log.w("Snoozer", "Could not publish the shortcut", it) }
    }
}
