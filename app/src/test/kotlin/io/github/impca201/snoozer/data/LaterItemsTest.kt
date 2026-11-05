package io.github.impca201.snoozer.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LaterItemsTest {
    private fun item(
        key: String,
        title: String = "Title $key",
        text: String = "Text $key",
        postedAt: Long = 1_000L,
    ) = LaterItem(key, "com.example", "Example", title, text, postedAt)

    @Test
    fun `a saved item goes to the top`() {
        val list = LaterItems.add(listOf(item("a"), item("b")), item("c"))
        assertEquals(listOf("c", "a", "b"), list.map { it.key })
    }

    @Test
    fun `saving the same notification again moves it instead of adding a duplicate`() {
        val list = LaterItems.add(listOf(item("a"), item("b")), item("b", title = "New"))
        assertEquals(listOf("b", "a"), list.map { it.key })
        assertEquals("New", list.first().title)
    }

    @Test
    fun `the list is capped and drops the oldest`() {
        var list = emptyList<LaterItem>()
        repeat(LaterItems.MAX_ITEMS + 5) { list = LaterItems.add(list, item("k$it")) }
        assertEquals(LaterItems.MAX_ITEMS, list.size)
        assertEquals("k${LaterItems.MAX_ITEMS + 4}", list.first().key)
        assertTrue(list.none { it.key == "k0" })
    }

    @Test
    fun `remove takes out only that key`() {
        val list = LaterItems.remove(listOf(item("a"), item("b")), "a")
        assertEquals(listOf("b"), list.map { it.key })
        assertEquals(list, LaterItems.remove(list, "missing"))
    }

    @Test
    fun `items survive storage, including line breaks and separators in the text`() {
        val list =
            listOf(
                item("0|com.whatsapp|123|null|0", title = "Anna", text = "Line one\nline two"),
                item("b", title = "Bad\u001Etitle", text = "Bad\u001Ftext", postedAt = Long.MAX_VALUE),
                item("c", title = "", text = ""),
            )
        val decoded = LaterItems.decode(LaterItems.encode(list))
        assertEquals(3, decoded.size)
        assertEquals(list[0], decoded[0])
        assertEquals("Badtitle", decoded[1].title)
        assertEquals("Badtext", decoded[1].text)
        assertEquals(Long.MAX_VALUE, decoded[1].postedAt)
        assertEquals(list[2], decoded[2])
    }

    @Test
    fun `nothing stored or damaged records give an empty or shorter list`() {
        assertEquals(emptyList<LaterItem>(), LaterItems.decode(null))
        assertEquals(emptyList<LaterItem>(), LaterItems.decode(""))
        assertEquals(emptyList<LaterItem>(), LaterItems.decode("garbage"))
        val good = LaterItems.encode(listOf(item("a")))
        val decoded = LaterItems.decode(good + "\u001Ebroken\u001Fx" + "\u001E" + good.replace("1000", "notanumber"))
        assertEquals(listOf("a"), decoded.map { it.key })
    }

    @Test
    fun `release requests round-trip and reject junk`() {
        LaterRelease.entries.forEach { release ->
            assertEquals("key|1" to release, LaterRelease.decode(release.encode("key|1")))
        }
        assertNull(LaterRelease.decode("r:"))
        assertNull(LaterRelease.decode("x:key"))
        assertNull(LaterRelease.decode(""))
    }

    @Test
    fun `release prefixes are stable because pending requests are stored`() {
        assertEquals("r:k", LaterRelease.Restore.encode("k"))
        assertEquals("d:k", LaterRelease.Remove.encode("k"))
    }

    @Test
    fun `the notification colour is stored, including negative ARGB values`() {
        val list = listOf(item("a").copy(color = 0xFF25D366.toInt()), item("b").copy(color = 0))
        assertEquals(list, LaterItems.decode(LaterItems.encode(list)))
    }

    @Test
    fun `records from before the colour was stored still load, without a colour`() {
        val old = listOf("k", "com.example", "Example", "Title", "Text", "1000").joinToString("\u001F")
        assertEquals(listOf(item("k", title = "Title", text = "Text")), LaterItems.decode(old))
    }

    @Test
    fun `a record with a damaged colour is skipped`() {
        val bad = listOf("k", "com.example", "Example", "Title", "Text", "1000", "red").joinToString("\u001F")
        assertEquals(emptyList<LaterItem>(), LaterItems.decode(bad))
    }

    @Test
    fun `swiped-away items stay on the list but are not shown in the shade`() {
        val list = listOf(item("a"), item("b"), item("c"))
        assertEquals(listOf("a", "c"), LaterItems.inShade(list, setOf("b", "gone")).map { it.key })
        assertEquals(list, LaterItems.inShade(list, emptySet()))
    }
}
