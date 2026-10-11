package io.github.impca201.snoozer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "snoozer")

/**
 * Single source of truth for the snooze switch, user settings and the keys of notifications
 * this app has snoozed (needed to bring them back after the process was killed).
 */
class SnoozeRepository(
    context: Context,
    scope: CoroutineScope,
) {
    private val dataStore = context.applicationContext.dataStore

    private val preferences: Flow<Preferences> =
        dataStore.data.catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }

    /** `null` until the stored value has been read for the first time. */
    val snoozeEnabled: StateFlow<Boolean?> =
        preferences
            .map { it[SNOOZE_ENABLED] ?: false }
            .distinctUntilChanged()
            .stateIn(scope, SharingStarted.Eagerly, null)

    val toastEnabled: StateFlow<Boolean> =
        preferences
            .map { it[TOAST_ENABLED] ?: false }
            .distinctUntilChanged()
            .stateIn(scope, SharingStarted.Eagerly, false)

    /** Whether to show the persistent "Snoozing notifications" notification while snoozing. */
    val indicatorEnabled: StateFlow<Boolean> =
        preferences
            .map { it[INDICATOR_ENABLED] ?: true }
            .distinctUntilChanged()
            .stateIn(scope, SharingStarted.Eagerly, true)

    suspend fun setIndicatorEnabled(enabled: Boolean) {
        dataStore.edit { it[INDICATOR_ENABLED] = enabled }
    }

    /** Opt-in (Android 15+): run a Snoozer "mode" that only dims the wallpaper while snoozing. */
    val dimEnabled: StateFlow<Boolean> =
        preferences
            .map { it[DIM_ENABLED] ?: false }
            .distinctUntilChanged()
            .stateIn(scope, SharingStarted.Eagerly, false)

    suspend fun setDimEnabled(enabled: Boolean) {
        dataStore.edit { it[DIM_ENABLED] = enabled }
    }

    suspend fun setSnoozeEnabled(enabled: Boolean) {
        dataStore.edit { it[SNOOZE_ENABLED] = enabled }
    }

    /** Atomically flips the switch and returns the new value. */
    suspend fun toggleSnooze(): Boolean {
        var newValue = false
        dataStore.edit {
            newValue = !(it[SNOOZE_ENABLED] ?: false)
            it[SNOOZE_ENABLED] = newValue
        }
        return newValue
    }

    suspend fun setToastEnabled(enabled: Boolean) {
        dataStore.edit { it[TOAST_ENABLED] = enabled }
    }

    suspend fun snoozedKeys(): Set<String> = preferences.first()[SNOOZED_KEYS].orEmpty()

    suspend fun addSnoozedKey(key: String) {
        dataStore.edit {
            val current = it[SNOOZED_KEYS].orEmpty()
            if (key !in current && current.size < MAX_TRACKED_KEYS) {
                it[SNOOZED_KEYS] = current + key
            }
        }
    }

    /** Returns every tracked key and forgets them in the same transaction. */
    suspend fun takeSnoozedKeys(): Set<String> {
        var taken: Set<String> = emptySet()
        dataStore.edit {
            taken = it[SNOOZED_KEYS].orEmpty()
            it.remove(SNOOZED_KEYS)
        }
        return taken
    }

    private companion object {
        val SNOOZE_ENABLED = booleanPreferencesKey("snooze_enabled")
        val TOAST_ENABLED = booleanPreferencesKey("toast_enabled")
        val INDICATOR_ENABLED = booleanPreferencesKey("indicator_enabled")
        val DIM_ENABLED = booleanPreferencesKey("dim_enabled")
        val SNOOZED_KEYS = stringSetPreferencesKey("snoozed_keys")

        /** The platform caps concurrent snoozes well below this; it only bounds storage. */
        const val MAX_TRACKED_KEYS = 1000
    }
}
