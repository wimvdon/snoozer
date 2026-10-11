package io.github.impca201.snoozer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
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

    /** Opt-in (Android 15+): turn on Snoozer's own mode (Settings › Modes) while snoozing. */
    val modeEnabled: StateFlow<Boolean> =
        preferences
            .map { it[MODE_ENABLED] ?: false }
            .distinctUntilChanged()
            .stateIn(scope, SharingStarted.Eagerly, false)

    suspend fun setModeEnabled(enabled: Boolean) {
        dataStore.edit { it[MODE_ENABLED] = enabled }
    }

    /** Custom name for Snoozer's mode; empty means the app name. */
    val modeName: StateFlow<String> =
        preferences
            .map { it[MODE_NAME].orEmpty() }
            .distinctUntilChanged()
            .stateIn(scope, SharingStarted.Eagerly, "")

    suspend fun setModeName(name: String) {
        dataStore.edit { it[MODE_NAME] = name.trim() }
    }

    /** Experimental: snooze while Pixel Bedtime is on (needs adb-granted permissions). */
    val followBedtime: StateFlow<Boolean> =
        preferences
            .map { it[FOLLOW_BEDTIME] ?: false }
            .distinctUntilChanged()
            .stateIn(scope, SharingStarted.Eagerly, false)

    suspend fun setFollowBedtime(enabled: Boolean) {
        dataStore.edit {
            it[FOLLOW_BEDTIME] = enabled
            // Start fresh: the first check after enabling counts as a change.
            it.remove(BEDTIME_LAST_ACTIVE)
            it.remove(BEDTIME_SNOOZED)
        }
    }

    /**
     * Applies a Bedtime reading. Acts only when Bedtime changes, so a manual snooze or
     * unsnooze during Bedtime is respected, and only undoes a snooze the sync started itself.
     */
    suspend fun onBedtimeReading(active: Boolean) {
        dataStore.edit {
            val last = it[BEDTIME_LAST_ACTIVE]
            it[BEDTIME_LAST_ACTIVE] = active
            if (last == active) return@edit
            val snoozed = it[SNOOZE_ENABLED] ?: false
            if (active && !snoozed) {
                it[SNOOZE_ENABLED] = true
                it[BEDTIME_SNOOZED] = true
            } else if (!active) {
                if (it[BEDTIME_SNOOZED] == true && snoozed) it[SNOOZE_ENABLED] = false
                it.remove(BEDTIME_SNOOZED)
            }
        }
    }

    /** A manual change (app, tile, shortcut, Tasker): the Bedtime sync no longer owns the snooze. */
    suspend fun setSnoozeEnabled(enabled: Boolean) {
        dataStore.edit {
            it[SNOOZE_ENABLED] = enabled
            it.remove(BEDTIME_SNOOZED)
        }
    }

    /** Atomically flips the switch and returns the new value. */
    suspend fun toggleSnooze(): Boolean {
        var newValue = false
        dataStore.edit {
            newValue = !(it[SNOOZE_ENABLED] ?: false)
            it[SNOOZE_ENABLED] = newValue
            it.remove(BEDTIME_SNOOZED)
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
        val MODE_ENABLED = booleanPreferencesKey("dim_enabled") // Historical key name.
        val MODE_NAME = stringPreferencesKey("mode_name")
        val FOLLOW_BEDTIME = booleanPreferencesKey("follow_bedtime")
        val BEDTIME_LAST_ACTIVE = booleanPreferencesKey("bedtime_last_active")
        val BEDTIME_SNOOZED = booleanPreferencesKey("bedtime_snoozed")
        val SNOOZED_KEYS = stringSetPreferencesKey("snoozed_keys")

        /** The platform caps concurrent snoozes well below this; it only bounds storage. */
        const val MAX_TRACKED_KEYS = 1000
    }
}
