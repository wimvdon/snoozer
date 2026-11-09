package io.github.impca201.snoozer.shortcut

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/**
 * Target of the launcher shortcut (res/xml/shortcuts.xml), which also works with Pixel Quick Tap.
 *
 * Android starts every manifest shortcut with FLAG_ACTIVITY_TASK_ON_HOME, so when its task closes
 * the home screen shows instead of the app the user was in. This invisible relay therefore does no
 * work itself: it starts [ShortcutActionActivity] in a task of its own, without that flag, and
 * closes. When the action closes, Android returns to the app the user was in.
 */
class ShortcutActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent?.action == ACTION_TOGGLE) {
            startActivity(
                Intent(ACTION_TOGGLE)
                    .setClass(this, ShortcutActionActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
        finish()
    }

    companion object {
        const val ACTION_TOGGLE = "io.github.impca201.snoozer.action.TOGGLE"
    }
}
