package io.github.impca201.snoozer.tasker

import android.os.Bundle
import androidx.annotation.StringRes
import io.github.impca201.snoozer.R

/**
 * The Locale plug-in contract implemented by Tasker, MacroDroid, Automate and others.
 * See https://tasker.joaoapps.com/plugins.html.
 */
object TaskerContract {
    const val ACTION_EDIT_SETTING = "com.twofortyfouram.locale.intent.action.EDIT_SETTING"
    const val ACTION_FIRE_SETTING = "com.twofortyfouram.locale.intent.action.FIRE_SETTING"
    const val EXTRA_BUNDLE = "com.twofortyfouram.locale.intent.extra.BUNDLE"
    const val EXTRA_STRING_BLURB = "com.twofortyfouram.locale.intent.extra.BLURB"

    const val KEY_COMMAND = "io.github.impca201.snoozer.extra.COMMAND"

    enum class Command(
        val id: String,
        @param:StringRes val label: Int,
        @param:StringRes val description: Int,
    ) {
        SNOOZE("snooze", R.string.tasker_command_snooze, R.string.tasker_command_snooze_desc),
        UNSNOOZE("unsnooze", R.string.tasker_command_unsnooze, R.string.tasker_command_unsnooze_desc),
        TOGGLE("toggle", R.string.tasker_command_toggle, R.string.tasker_command_toggle_desc),
        ;

        companion object {
            fun fromId(id: String?): Command? = entries.firstOrNull { it.id == id }
        }
    }

    fun bundleFor(command: Command): Bundle = Bundle().apply { putString(KEY_COMMAND, command.id) }

    /** Parses a bundle coming from another app; returns null for anything unexpected. */
    fun commandFrom(bundle: Bundle?): Command? =
        try {
            Command.fromId(bundle?.getString(KEY_COMMAND))
        } catch (_: RuntimeException) {
            // A malformed or hostile bundle can throw while being unparcelled.
            null
        }
}
