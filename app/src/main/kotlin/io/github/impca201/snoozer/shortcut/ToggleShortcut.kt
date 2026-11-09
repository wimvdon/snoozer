package io.github.impca201.snoozer.shortcut

/**
 * The long-press shortcut's id. It differs from the old manifest shortcut (`toggle_snooze`) on
 * purpose: Android keeps a pinned manifest shortcut locked under its id, and publishing that id
 * again would fail. The action ([ShortcutActivity.ACTION_TOGGLE]) stays the same.
 *
 * Plain Kotlin, tested on the JVM (see ToggleShortcutTest).
 */
object ToggleShortcut {
    const val ID = "snooze_toggle"
    const val OLD_MANIFEST_ID = "toggle_snooze"

    /** True when exactly this shortcut is published. */
    fun isPublished(publishedIds: List<String>): Boolean = publishedIds == listOf(ID)
}
