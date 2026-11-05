package io.github.impca201.snoozer.data

/** What Snoozer needs to know about a notification in the shade, without Android types. */
data class NotificationSnapshot(
    val key: String,
    val packageName: String,
    val appLabel: String,
    val title: String,
    val text: String,
    val postedAt: Long,
    val isClearable: Boolean,
    val isGroupSummary: Boolean,
    val isOwn: Boolean,
    /** `Notification.color` (ARGB); 0 when the app set none. */
    val color: Int = 0,
) {
    fun toLaterItem(): LaterItem = LaterItem(key, packageName, appLabel, title, text, postedAt, color)

    companion object {
        /**
         * The notifications the "Save for later" sheet offers: ones the user could swipe away,
         * not Snoozer's own, not invisible group summaries, with something to show. Newest first.
         * One with the key of a saved item is a newer notification; saving it replaces the item.
         */
        fun candidates(active: List<NotificationSnapshot>): List<NotificationSnapshot> =
            active
                .asSequence()
                .filter { it.isClearable && !it.isOwn && !it.isGroupSummary }
                .filter { it.title.isNotBlank() || it.text.isNotBlank() }
                .distinctBy { it.key }
                .sortedByDescending { it.postedAt }
                .toList()
    }
}
