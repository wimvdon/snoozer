package io.github.impca201.snoozer.shortcut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToggleShortcutTest {
    @Test
    fun `the id doesn't reuse the old manifest shortcut id, which Android may keep locked`() {
        assertNotEquals(ToggleShortcut.OLD_MANIFEST_ID, ToggleShortcut.ID)
    }

    @Test
    fun `the shortcut action stays the same, because launchers and gesture apps use it`() {
        assertEquals("io.github.impca201.snoozer.action.TOGGLE", ShortcutActivity.ACTION_TOGGLE)
    }

    @Test
    fun `published only when exactly the toggle shortcut is there`() {
        assertTrue(ToggleShortcut.isPublished(listOf(ToggleShortcut.ID)))
        assertFalse(ToggleShortcut.isPublished(emptyList()))
        assertFalse(ToggleShortcut.isPublished(listOf(ToggleShortcut.OLD_MANIFEST_ID)))
    }
}
