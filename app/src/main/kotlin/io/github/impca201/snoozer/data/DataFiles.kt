package io.github.impca201.snoozer.data

/**
 * Names of the DataStore files. Only [BACKED_UP_SETTINGS] goes into Android's backup and
 * phone-to-phone transfer (`res/xml/data_extraction_rules.xml`, `res/xml/backup_rules.xml`,
 * checked by `BackupRulesTest`).
 */
object DataFiles {
    /** Settings the user chose: theme, cues, the mode and its name. Backed up. */
    const val BACKED_UP_SETTINGS = "backed_up_settings"

    /** The switch, Follow Bedtime and the snoozed keys: only meaningful on this phone. */
    const val DEVICE_STATE = "snoozer"

    /** Where DataStore keeps a file, relative to the app's files folder (backup rules use this). */
    fun path(name: String) = "datastore/$name.preferences_pb"
}
