package io.github.impca201.snoozer.bedtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayOutputStream

class BedtimeParserTest {
    // Minimal protobuf encoder for building test dumps.
    private fun varint(
        out: ByteArrayOutputStream,
        value: Long,
    ) {
        var v = value
        while (v and 0x7F.inv().toLong() != 0L) {
            out.write(((v and 0x7F) or 0x80).toInt())
            v = v ushr 7
        }
        out.write(v.toInt())
    }

    private fun message(vararg fields: ByteArray): ByteArray = fields.fold(ByteArray(0)) { acc, f -> acc + f }

    private fun bytesField(
        number: Int,
        payload: ByteArray,
    ): ByteArray {
        val out = ByteArrayOutputStream()
        varint(out, (number.toLong() shl 3) or 2)
        varint(out, payload.size.toLong())
        out.write(payload)
        return out.toByteArray()
    }

    private fun stringField(
        number: Int,
        value: String,
    ) = bytesField(number, value.toByteArray())

    private fun varintField(
        number: Int,
        value: Long,
    ): ByteArray {
        val out = ByteArrayOutputStream()
        varint(out, number.toLong() shl 3)
        varint(out, value)
        return out.toByteArray()
    }

    private val bedtimeRule =
        message(
            stringField(1, "rule-id-1"),
            stringField(2, "Bedtijd"),
            stringField(8, "condition://com.google.android.apps.wellbeing/winddown"),
        )
    private val otherRule = message(stringField(1, "rule-id-2"), stringField(2, "Snoozer"), stringField(8, "snoozer://mode/dim"))

    /** Wellbeing's own notification, which also mentions "winddown", lives in field 1 (records). */
    private val wellbeingNotification = bytesField(1, message(stringField(1, "0|com.google.android.apps.wellbeing|winddown_channel")))

    private fun dump(vararg activeRules: ByteArray): ByteArray =
        message(
            wellbeingNotification,
            bytesField(2, message(varintField(1, 1), *activeRules.map { bytesField(2, it) }.toTypedArray())),
        )

    @Test
    fun `bedtime among the active rules is on`() {
        assertEquals(true, BedtimeParser.isBedtimeActive(dump(otherRule, bedtimeRule)))
    }

    @Test
    fun `other active rules only means bedtime is off`() {
        assertEquals(false, BedtimeParser.isBedtimeActive(dump(otherRule)))
    }

    @Test
    fun `no active rules means bedtime is off even though its notification mentions winddown`() {
        assertEquals(false, BedtimeParser.isBedtimeActive(dump()))
    }

    @Test
    fun `a dump without a zen section is unreadable`() {
        assertNull(BedtimeParser.isBedtimeActive(message(wellbeingNotification)))
    }

    @Test
    fun `text or garbage is unreadable`() {
        assertNull(BedtimeParser.isBedtimeActive("Permission Denial: can't dump".toByteArray()))
        assertNull(BedtimeParser.isBedtimeActive(byteArrayOf(0x12, 0x7F, 0x01)))
    }
}
