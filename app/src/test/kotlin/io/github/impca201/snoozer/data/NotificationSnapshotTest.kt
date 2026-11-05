package io.github.impca201.snoozer.data

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationSnapshotTest {
    private fun snapshot(
        key: String,
        postedAt: Long = 0L,
        title: String = "Title",
        text: String = "Text",
        isClearable: Boolean = true,
        isGroupSummary: Boolean = false,
        isOwn: Boolean = false,
    ) = NotificationSnapshot(key, "com.example", "Example", title, text, postedAt, isClearable, isGroupSummary, isOwn)

    @Test
    fun `offers clearable notifications from other apps, newest first`() {
        val result = NotificationSnapshot.candidates(listOf(snapshot("old", 1), snapshot("new", 2)))
        assertEquals(listOf("new", "old"), result.map { it.key })
    }

    @Test
    fun `skips what can't be put aside`() {
        val active =
            listOf(
                snapshot("ongoing", isClearable = false),
                snapshot("summary", isGroupSummary = true),
                snapshot("own", isOwn = true),
                snapshot("blank", title = " ", text = ""),
                snapshot("ok"),
            )
        assertEquals(listOf("ok"), NotificationSnapshot.candidates(active).map { it.key })
    }

    @Test
    fun `a notification with only text is offered, and duplicates appear once`() {
        val active = listOf(snapshot("a", title = "", text = "Only text"), snapshot("a", title = "", text = "Only text"))
        assertEquals(listOf("a"), NotificationSnapshot.candidates(active).map { it.key })
    }

    @Test
    fun `a newer notification under a saved key is offered again`() {
        // Apps reuse keys (one per chat, or one for everything); saving it again replaces the item.
        val result = NotificationSnapshot.candidates(listOf(snapshot("chat", title = "Anna", text = "New message")))
        assertEquals(listOf("chat"), result.map { it.key })
    }

    @Test
    fun `becomes a later item with the same details`() {
        val item = snapshot("k", postedAt = 5, title = "T", text = "X").toLaterItem()
        assertEquals(LaterItem("k", "com.example", "Example", "T", "X", 5), item)
    }
}
