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

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = DataFiles.DEVICE_STATE)

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(
    name = DataFiles.BACKED_UP_SETTINGS,
    // Settings used to live in the device-state file; move them over once.
    produceMigrations = { context -> listOf(MoveKeysMigration(context.dataStore, SnoozeRepository.SETTINGS_KEYS)) },
)

/**
 * Single source of truth for the snooze switch, user settings and the keys of notifications
 * this app has snoozed (needed to bring them back after the process was killed).
 *
 * The settings live in their own file, which Android backs up; the rest stays on this phone
 * (see [DataFiles]).
 */
class SnoozeRepository(
    context: Context,
    scope: CoroutineScope,
) {
    private val dataStore = context.applicationContext.dataStore
    private val settingsStore = context.applicationContext.settingsStore

    private val preferences: Flow<Preferences> = dataStore.data.ignoringReadErrors()
    private val settings: Flow<Preferences> = settingsStore.data.ignoringReadErrors()

    private fun Flow<Preferences>.ignoringReadErrors() =
        catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }

    /** `null` until the stored value has been read for the first time. */
    val snoozeEnabled: StateFlow<Boolean?> =
        preferences
            .map { it[SNOOZE_ENABLED] ?: false }
            .distinctUntilChanged()
            .stateIn(scope, SharingStarted.Eagerly, null)

    val toastEnabled: StateFlow<Boolean> =
        settings
            .map { it[TOAST_ENABLED] ?: false }
            .distinctUntilChanged()
            .stateIn(scope, SharingStarted.Eagerly, false)

    /** Whether to show the persistent "Snoozing notifications" notification while snoozing. */
    val indicatorEnabled: StateFlow<Boolean> =
        settings
            .map { it[INDICATOR_ENABLED] ?: false }
            .distinctUntilChanged()
            .stateIn(scope, SharingStarted.Eagerly, false)

    suspend fun setIndicatorEnabled(enabled: Boolean) {
        settingsStore.edit { it[INDICATOR_ENABLED] = enabled }
    }

    /** Opt-in (Android 15+): turn on Snoozer's own mode (Settings › Modes) while snoozing. */
    val modeEnabled: StateFlow<Boolean> =
        settings
            .map { it[MODE_ENABLED] ?: false }
            .distinctUntilChanged()
            .stateIn(scope, SharingStarted.Eagerly, false)

    suspend fun setModeEnabled(enabled: Boolean) {
        settingsStore.edit { it[MODE_ENABLED] = enabled }
    }

    /** Custom name for Snoozer's mode; empty means the app name. */
    val modeName: StateFlow<String> =
        settings
            .map { it[MODE_NAME].orEmpty() }
            .distinctUntilChanged()
            .stateIn(scope, SharingStarted.Eagerly, "")

    suspend fun setModeName(name: String) {
        settingsStore.edit { it[MODE_NAME] = name.trim() }
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

    /** Theme settings; `null` until read, so the first frame never shows the wrong theme. */
    val appearance: StateFlow<Appearance?> =
        settings
            .map { p ->
                Appearance(
                    themeMode = ThemeMode.entries.firstOrNull { it.name == p[THEME_MODE] } ?: ThemeMode.System,
                    dynamicColor = p[DYNAMIC_COLOR] ?: false,
                )
            }.distinctUntilChanged()
            .stateIn(scope, SharingStarted.Eagerly, null)

    suspend fun setThemeMode(mode: ThemeMode) {
        settingsStore.edit { it[THEME_MODE] = mode.name }
    }

    /** Wallpaper colours (Android 12+) instead of the app's own palette. */
    suspend fun setDynamicColor(enabled: Boolean) {
        settingsStore.edit { it[DYNAMIC_COLOR] = enabled }
    }

    suspend fun setToastEnabled(enabled: Boolean) {
        settingsStore.edit { it[TOAST_ENABLED] = enabled }
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

    /** Notifications saved for later, newest first; `null` until read. Device-only (not backed up). */
    val laterItems: StateFlow<List<LaterItem>?> =
        preferences
            .map { LaterItems.decode(it[LATER_ITEMS]) }
            .distinctUntilChanged()
            .stateIn(scope, SharingStarted.Eagerly, null)

    /** Keys of the saved notifications, for the listener's quick checks. */
    fun laterKeys(): Set<String> = laterItems.value?.mapTo(HashSet()) { it.key }.orEmpty()

    /**
     * Saves a notification for later and, in the same transaction, asks the listener to take it
     * off the shade, so the request survives the listener being disconnected right now.
     */
    suspend fun addLater(item: LaterItem) {
        dataStore.edit {
            it[LATER_ITEMS] = LaterItems.encode(LaterItems.add(LaterItems.decode(it[LATER_ITEMS]), item))
            it[LATER_RELEASES] = it[LATER_RELEASES].orEmpty() + LaterRelease.Remove.encode(item.key)
            it[LATER_HIDDEN] = it[LATER_HIDDEN].orEmpty() - item.key // Saved again: show it again.
        }
    }

    /** Opened from the widget or the shade: it leaves the list. */
    suspend fun removeLater(key: String) {
        dataStore.edit {
            it[LATER_ITEMS] = LaterItems.encode(LaterItems.remove(LaterItems.decode(it[LATER_ITEMS]), key))
            it[LATER_HIDDEN] = it[LATER_HIDDEN].orEmpty() - key
        }
    }

    /** Keys of saved notifications the user swiped away in the shade; they stay in the widget. */
    val laterHidden: StateFlow<Set<String>> =
        preferences
            .map { it[LATER_HIDDEN].orEmpty() }
            .distinctUntilChanged()
            .stateIn(scope, SharingStarted.Eagerly, emptySet())

    suspend fun hideLater(key: String) {
        dataStore.edit {
            // Only keys still on the list, so the set can't grow without bound.
            val saved = LaterItems.decode(it[LATER_ITEMS]).mapTo(HashSet()) { item -> item.key }
            it[LATER_HIDDEN] = (it[LATER_HIDDEN].orEmpty() + key).filterTo(HashSet()) { hidden -> hidden in saved }
        }
    }

    /**
     * Once per install: the keys of notifications version 1.5.1 snoozed for a year, which keeps
     * the app's new notifications under the same key hidden too. `null` when already done.
     */
    suspend fun takeLegacyLaterKeys(): Set<String>? {
        var keys: Set<String>? = null
        dataStore.edit {
            if (it[LATER_NOT_SNOOZED] == true) return@edit
            it[LATER_NOT_SNOOZED] = true
            keys = LaterItems.decode(it[LATER_ITEMS]).mapTo(HashSet()) { item -> item.key }
        }
        return keys
    }

    /** Pending release requests; the listener applies them with [takeLaterReleases]. */
    val laterReleasesPending: Flow<Boolean> =
        preferences.map { !it[LATER_RELEASES].isNullOrEmpty() }.distinctUntilChanged()

    suspend fun takeLaterReleases(): List<Pair<String, LaterRelease>> {
        var taken: Set<String> = emptySet()
        dataStore.edit {
            taken = it[LATER_RELEASES].orEmpty()
            it.remove(LATER_RELEASES)
        }
        return taken.mapNotNull(LaterRelease::decode)
    }

    companion object {
        private val LATER_ITEMS = stringPreferencesKey("later_items")
        private val LATER_RELEASES = stringSetPreferencesKey("later_releases")
        private val LATER_NOT_SNOOZED = booleanPreferencesKey("later_not_snoozed")
        private val LATER_HIDDEN = stringSetPreferencesKey("later_hidden")
        private val SNOOZE_ENABLED = booleanPreferencesKey("snooze_enabled")
        private val TOAST_ENABLED = booleanPreferencesKey("toast_enabled")
        private val INDICATOR_ENABLED = booleanPreferencesKey("indicator_enabled")
        private val MODE_ENABLED = booleanPreferencesKey("dim_enabled") // Historical key name.
        private val MODE_NAME = stringPreferencesKey("mode_name")
        private val FOLLOW_BEDTIME = booleanPreferencesKey("follow_bedtime")
        private val BEDTIME_LAST_ACTIVE = booleanPreferencesKey("bedtime_last_active")
        private val BEDTIME_SNOOZED = booleanPreferencesKey("bedtime_snoozed")
        private val SNOOZED_KEYS = stringSetPreferencesKey("snoozed_keys")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")

        /** The platform caps concurrent snoozes well below this; it only bounds storage. */
        private const val MAX_TRACKED_KEYS = 1000

        /** Moved to the backed-up settings file. Follow Bedtime stays: it needs adb set-up per phone. */
        internal val SETTINGS_KEYS: Set<Preferences.Key<*>> =
            setOf(TOAST_ENABLED, INDICATOR_ENABLED, MODE_ENABLED, MODE_NAME, THEME_MODE, DYNAMIC_COLOR)
    }
}
