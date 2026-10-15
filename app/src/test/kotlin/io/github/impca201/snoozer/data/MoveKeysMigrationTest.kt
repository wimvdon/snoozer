package io.github.impca201.snoozer.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MoveKeysMigrationTest {
    @get:Rule val folder = TemporaryFolder()

    private val theme = stringPreferencesKey("theme_mode")
    private val toast = booleanPreferencesKey("toast_enabled")
    private val snoozing = booleanPreferencesKey("snooze_enabled")

    private val old = PreferenceDataStoreFactory.create { folder.root.resolve("old.preferences_pb") }

    private fun target(): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(migrations = listOf(MoveKeysMigration(old, setOf(theme, toast)))) {
            folder.root.resolve("new.preferences_pb")
        }

    @Test
    fun `moves the listed keys and leaves the rest`() =
        runBlocking {
            old.edit {
                it[theme] = "Dark"
                it[toast] = true
                it[snoozing] = true
            }

            val moved = target().data.first()

            assertEquals("Dark", moved[theme])
            assertEquals(true, moved[toast])
            assertNull(moved[snoozing])
            val left = old.data.first()
            assertNull(left[theme])
            assertNull(left[toast])
            assertEquals(true, left[snoozing])
        }

    @Test
    fun `a fresh install starts empty`() =
        runBlocking {
            assertEquals(0, target().data.first().asMap().size)
        }

    @Test
    fun `a value already in the new file is kept`() =
        runBlocking {
            old.edit { it[theme] = "Dark" }

            val result = MoveKeysMigration(old, setOf(theme)).migrate(preferencesOf(theme to "Light"))

            assertEquals("Light", result[theme])
        }

    @Test
    fun `nothing to move after the first time`() =
        runBlocking {
            old.edit { it[snoozing] = true }

            assertFalse(MoveKeysMigration(old, setOf(theme, toast)).shouldMigrate(preferencesOf()))
        }
}
