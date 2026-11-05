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
        val result = NotificationSnapshot.candidates(listOf(snapshot("old", 1), snapshot("new", 2)), emptySet())
        assertEquals(listOf("new", "old"), result.map { it.key })
    }

    @Test
    fun `skips what can't or shouldn't be put aside`() {
        val active =
            listOf(
                snapshot("ongoing", isClearable = false),
                snapshot("summary", isGroupSummary = true),
                snapshot("own", isOwn = true),
                snapshot("blank", title = " ", text = ""),
                snapshot("saved"),
                snapshot("ok"),
            )
        assertEquals(listOf("ok"), NotificationSnapshot.candidates(active, setOf("saved")).map { it.key })
    }

    @Test
    fun `a notification with only text is offered, and duplicates appear once`() {
        val active = listOf(snapshot("a", title = "", text = "Only text"), snapshot("a", title = "", text = "Only text"))
        assertEquals(listOf("a"), NotificationSnapshot.candidates(active, emptySet()).map { it.key })
    }

    @Test
    fun `becomes a later item with the same details`() {
        val item = snapshot("k", postedAt = 5, title = "T", text = "X").toLaterItem()
        assertEquals(LaterItem("k", "com.example", "Example", "T", "X", 5), item)
    }
}
