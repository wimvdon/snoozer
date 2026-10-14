package io.github.impca201.snoozer.bedtime

/**
 * Just enough of the protobuf wire format to walk `dumpsys --proto` output without a library
 * or generated classes. Unknown fields are skipped; malformed input throws [ProtoException].
 */
internal class ProtoReader(
    private val bytes: ByteArray,
    private var position: Int = 0,
    private val end: Int = bytes.size,
) {
    class Field(
        val number: Int,
        val wireType: Int,
        /** Set for length-delimited fields (strings, bytes, nested messages). */
        val start: Int = 0,
        val length: Int = 0,
    )

    fun hasMore(): Boolean = position < end

    fun next(): Field {
        val tag = readVarint()
        val number = (tag ushr 3).toInt()
        val wireType = (tag and 0x7).toInt()
        if (number <= 0) throw ProtoException("bad field number")
        when (wireType) {
            WIRE_VARINT -> {
                readVarint()
            }

            WIRE_FIXED64 -> {
                skip(8)
            }

            WIRE_LENGTH_DELIMITED -> {
                val length = readVarint()
                if (length < 0 || length > end - position) throw ProtoException("bad length")
                val start = position
                skip(length.toInt())
                return Field(number, wireType, start, length.toInt())
            }

            WIRE_FIXED32 -> {
                skip(4)
            }

            else -> {
                throw ProtoException("unsupported wire type $wireType")
            }
        }
        return Field(number, wireType)
    }

    /** A reader over the contents of a length-delimited [field]. */
    fun nested(field: Field): ProtoReader = ProtoReader(bytes, field.start, field.start + field.length)

    fun string(field: Field): String = String(bytes, field.start, field.length, Charsets.UTF_8)

    private fun readVarint(): Long {
        var result = 0L
        var shift = 0
        while (shift < 64) {
            if (position >= end) throw ProtoException("truncated varint")
            val b = bytes[position++].toInt()
            result = result or ((b and 0x7F).toLong() shl shift)
            if (b and 0x80 == 0) return result
            shift += 7
        }
        throw ProtoException("varint too long")
    }

    private fun skip(count: Int) {
        if (count > end - position) throw ProtoException("truncated field")
        position += count
    }

    companion object {
        const val WIRE_VARINT = 0
        const val WIRE_FIXED64 = 1
        const val WIRE_LENGTH_DELIMITED = 2
        const val WIRE_FIXED32 = 5
    }
}

internal class ProtoException(
    message: String,
) : Exception(message)
