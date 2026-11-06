package io.github.impca201.snoozer.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RemoveKeysMigrationTest {
    @get:Rule val folder = TemporaryFolder()

    private val snoozing = booleanPreferencesKey("snooze_enabled")
    private val snoozedKeys = stringSetPreferencesKey("snoozed_keys")

    @Test
    fun `deletes every Do later key, whatever its type, and keeps the rest`() =
        runBlocking {
            val file = folder.root.resolve("snoozer.preferences_pb")
            // The old version's DataStore; closed before the "updated app" opens the same file.
            val oldScope = CoroutineScope(Dispatchers.IO + Job())
            PreferenceDataStoreFactory.create(scope = oldScope) { file }.edit {
                it[snoozing] = true
                it[snoozedKeys] = setOf("0|com.example|1|null|10")
                it[stringPreferencesKey("later_items")] = "saved notification text"
                it[stringSetPreferencesKey("later_releases")] = setOf("d:key")
                it[booleanPreferencesKey("later_not_snoozed")] = true
                it[stringSetPreferencesKey("later_hidden")] = setOf("key")
            }
            oldScope.coroutineContext[Job]!!.cancelAndJoin()
            // A fresh DataStore for the same file, as after the app update.
            val migrated =
                PreferenceDataStoreFactory
                    .create(migrations = listOf(RemoveKeysMigration(RemoveKeysMigration.DO_LATER_KEYS))) { file }
                    .data
                    .first()
            assertEquals(setOf("snooze_enabled", "snoozed_keys"), migrated.asMap().keys.map { it.name }.toSet())
            assertEquals(true, migrated[snoozing])
        }

    @Test
    fun `nothing to do without Do later keys`() =
        runBlocking {
            val migration = RemoveKeysMigration(RemoveKeysMigration.DO_LATER_KEYS)
            val store = PreferenceDataStoreFactory.create { folder.root.resolve("clean.preferences_pb") }
            store.edit { it[snoozing] = false }
            assertFalse(migration.shouldMigrate(store.data.first()))
        }

    @Test
    fun `no key the app still uses is deleted`() {
        val inUse = setOf("snooze_enabled", "follow_bedtime", "bedtime_last_active", "bedtime_snoozed", "snoozed_keys")
        assertEquals(emptySet<String>(), RemoveKeysMigration.DO_LATER_KEYS intersect inUse)
    }
}
