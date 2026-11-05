package io.github.impca201.snoozer.data

/**
 * A notification the user saved for later. It is removed from the shade when it is saved; title
 * and text are kept so the widget can show it until it is opened from there.
 */
data class LaterItem(
    val key: String,
    val packageName: String,
    val appLabel: String,
    val title: String,
    val text: String,
    /** When the posting app showed it (`StatusBarNotification.postTime`), in epoch millis. */
    val postedAt: Long,
    /** The notification's accent colour (`Notification.color`, ARGB); 0 when it has none. */
    val color: Int = 0,
)

/** A request from the repository to the listener about one notification key. Prefixes are stored: never change them. */
enum class LaterRelease(
    private val prefix: String,
) {
    /** Bring the notification back in the shade (frees keys version 1.5.1 snoozed). */
    Restore("r:"),

    /** Take the notification off the shade: it was just saved for later. */
    Remove("d:"),
    ;

    fun encode(key: String): String = prefix + key

    companion object {
        fun decode(token: String): Pair<String, LaterRelease>? {
            val release = entries.firstOrNull { token.startsWith(it.prefix) } ?: return null
            val key = token.removePrefix(release.prefix)
            return if (key.isEmpty()) null else key to release
        }
    }
}

/**
 * Plain-Kotlin list rules and storage format for [LaterItem]s, so they can be unit tested.
 *
 * Stored as one string: records separated by RS (U+001E), fields by US (U+001F). Those control
 * characters are removed from the text first, so content can never break the format.
 */
object LaterItems {
    /** Bounds storage and the widget's length; older items fall off the end. */
    const val MAX_ITEMS = 100

    private const val RECORD = '\u001E'
    private const val FIELD = '\u001F'
    private const val FIELD_COUNT = 7

    /** Records written by 1.5.1 and 1.5.2, before the colour was stored. */
    private const val OLD_FIELD_COUNT = 6

    /** Newest first; an item that is saved again moves to the top instead of appearing twice. */
    fun add(
        items: List<LaterItem>,
        item: LaterItem,
    ): List<LaterItem> = (listOf(item) + items.filterNot { it.key == item.key }).take(MAX_ITEMS)

    fun remove(
        items: List<LaterItem>,
        key: String,
    ): List<LaterItem> = items.filterNot { it.key == key }

    fun encode(items: List<LaterItem>): String =
        items.joinToString(RECORD.toString()) { item ->
            listOf(item.key, item.packageName, item.appLabel, item.title, item.text, item.postedAt.toString(), item.color.toString())
                .joinToString(FIELD.toString()) { it.clean() }
        }

    /** Unreadable records are skipped rather than failing the whole list. */
    fun decode(stored: String?): List<LaterItem> {
        if (stored.isNullOrEmpty()) return emptyList()
        return stored.split(RECORD).mapNotNull { record ->
            val fields = record.split(FIELD)
            if ((fields.size != FIELD_COUNT && fields.size != OLD_FIELD_COUNT) || fields[0].isEmpty()) return@mapNotNull null
            val postedAt = fields[5].toLongOrNull() ?: return@mapNotNull null
            val color = if (fields.size == FIELD_COUNT) fields[6].toIntOrNull() ?: return@mapNotNull null else 0
            LaterItem(fields[0], fields[1], fields[2], fields[3], fields[4], postedAt, color)
        }
    }

    /**
     * The saved notifications Snoozer shows in the shade: all of them except the ones the user
     * swiped away there. A swipe only hides it from the shade; it stays in the widget.
     */
    fun inShade(
        items: List<LaterItem>,
        hidden: Set<String>,
    ): List<LaterItem> = items.filterNot { it.key in hidden }

    private fun String.clean(): String = filterNot { it == RECORD || it == FIELD }
}
