package io.github.impca201.snoozer.data

/**
 * A notification the user saved for later. It stays snoozed until it is opened or marked done
 * from the widget. Title and text are kept so the widget can show it while it is hidden.
 */
data class LaterItem(
    val key: String,
    val packageName: String,
    val appLabel: String,
    val title: String,
    val text: String,
    /** When the posting app showed it (`StatusBarNotification.postTime`), in epoch millis. */
    val postedAt: Long,
)

/** What to do with a saved notification once it leaves the list. */
enum class LaterRelease(
    private val prefix: String,
) {
    /** Put it back in the notification shade (it was opened but its link could not be used). */
    Restore("r:"),

    /** Remove it for good (opened through its own link, or marked done). */
    Dismiss("d:"),
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
    private const val FIELD_COUNT = 6

    /** Newest first; an item that is saved again moves to the top instead of appearing twice. */
    fun add(
        items: List<LaterItem>,
        item: LaterItem,
    ): List<LaterItem> = (listOf(item) + items.filterNot { it.key == item.key }).take(MAX_ITEMS)

    fun remove(
        items: List<LaterItem>,
        key: String,
    ): List<LaterItem> = items.filterNot { it.key == key }

    /** The app updated a saved notification (a new message in the same chat): keep its place, show the new text. */
    fun refresh(
        items: List<LaterItem>,
        update: LaterItem,
    ): List<LaterItem> =
        items.map { item ->
            if (item.key != update.key) {
                item
            } else {
                item.copy(
                    title = update.title.ifEmpty { item.title },
                    text = update.text.ifEmpty { item.text },
                    appLabel = update.appLabel.ifEmpty { item.appLabel },
                    postedAt = update.postedAt,
                )
            }
        }

    fun encode(items: List<LaterItem>): String =
        items.joinToString(RECORD.toString()) { item ->
            listOf(item.key, item.packageName, item.appLabel, item.title, item.text, item.postedAt.toString())
                .joinToString(FIELD.toString()) { it.clean() }
        }

    /** Unreadable records are skipped rather than failing the whole list. */
    fun decode(stored: String?): List<LaterItem> {
        if (stored.isNullOrEmpty()) return emptyList()
        return stored.split(RECORD).mapNotNull { record ->
            val fields = record.split(FIELD)
            if (fields.size != FIELD_COUNT || fields[0].isEmpty()) return@mapNotNull null
            val postedAt = fields[5].toLongOrNull() ?: return@mapNotNull null
            LaterItem(fields[0], fields[1], fields[2], fields[3], fields[4], postedAt)
        }
    }

    private fun String.clean(): String = filterNot { it == RECORD || it == FIELD }
}
