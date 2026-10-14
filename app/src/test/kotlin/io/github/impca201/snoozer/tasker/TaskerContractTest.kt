package io.github.impca201.snoozer.tasker

import io.github.impca201.snoozer.tasker.TaskerContract.Command
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TaskerContractTest {
    @Test
    fun `every command round-trips through its id`() {
        Command.entries.forEach { command ->
            assertEquals(command, Command.fromId(command.id))
        }
    }

    @Test
    fun `ids are stable because Tasker stores them in user tasks`() {
        assertEquals("snooze", Command.SNOOZE.id)
        assertEquals("unsnooze", Command.UNSNOOZE.id)
        assertEquals("toggle", Command.TOGGLE.id)
    }

    @Test
    fun `unknown or missing ids are rejected`() {
        assertNull(Command.fromId(null))
        assertNull(Command.fromId(""))
        assertNull(Command.fromId("SNOOZE"))
        assertNull(Command.fromId("delete-everything"))
    }
}
