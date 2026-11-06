package io.github.impca201.snoozer.data

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences

/**
 * Deletes stored values whose key [names] are no longer used, whatever their type. Used for the
 * removed "Do later" feature (versions 1.5.1 to 1.5.3), whose saved items held notification text.
 */
class RemoveKeysMigration(
    private val names: Set<String>,
) : DataMigration<Preferences> {
    override suspend fun shouldMigrate(currentData: Preferences): Boolean = currentData.asMap().keys.any { it.name in names }

    override suspend fun migrate(currentData: Preferences): Preferences {
        val cleaned = currentData.toMutablePreferences()
        currentData.asMap().keys.filter { it.name in names }.forEach { cleaned.remove(it) }
        return cleaned.toPreferences()
    }

    override suspend fun cleanUp() = Unit

    companion object {
        /** Keys of the removed "Do later" feature, in the device-state file. */
        val DO_LATER_KEYS = setOf("later_items", "later_releases", "later_not_snoozed", "later_hidden")
    }
}
