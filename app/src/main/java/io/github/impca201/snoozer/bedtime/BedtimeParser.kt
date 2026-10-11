package io.github.impca201.snoozer.bedtime

/**
 * Finds out from `dumpsys notification --proto` whether Pixel's Bedtime mode is active.
 *
 * Layout (AOSP notificationmanagerservice.proto / zen proto):
 *   NotificationServiceDumpProto.zen (2) -> ZenModeProto
 *   ZenModeProto.enabled_active_conditions (2, repeated) -> ZenRuleProto, one per ACTIVE mode
 * Each active rule is searched for the Digital Wellbeing Bedtime identifier ("winddown", which
 * also appears in its condition id). Notifications are stored in a different top-level field,
 * so Bedtime's own "Bedtime is on" notification can never cause a false reading.
 */
object BedtimeParser {
    private const val FIELD_DUMP_ZEN = 2
    private const val FIELD_ZEN_ACTIVE_RULES = 2
    private const val BEDTIME_MARKER = "winddown"

    /** True/false when the dump could be read, null when its layout is not what we expect. */
    fun isBedtimeActive(dump: ByteArray): Boolean? =
        try {
            val zen = findMessage(ProtoReader(dump), FIELD_DUMP_ZEN) ?: return null
            var active = false
            while (zen.hasMore()) {
                val field = zen.next()
                if (field.number == FIELD_ZEN_ACTIVE_RULES && field.wireType == ProtoReader.WIRE_LENGTH_DELIMITED) {
                    // A rule's bytes include all of its (nested) strings, such as the condition id.
                    if (zen.string(field).contains(BEDTIME_MARKER, ignoreCase = true)) active = true
                }
            }
            active
        } catch (_: ProtoException) {
            null
        }

    private fun findMessage(
        reader: ProtoReader,
        number: Int,
    ): ProtoReader? {
        while (reader.hasMore()) {
            val field = reader.next()
            if (field.number == number && field.wireType == ProtoReader.WIRE_LENGTH_DELIMITED) return reader.nested(field)
        }
        return null
    }
}
