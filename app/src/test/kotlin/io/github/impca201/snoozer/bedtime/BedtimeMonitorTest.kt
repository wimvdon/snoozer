package io.github.impca201.snoozer.bedtime

import org.junit.Assert.assertEquals
import org.junit.Test

class BedtimeMonitorTest {
    @Test
    fun `adb commands grant both permissions to the installed package`() {
        assertEquals(
            listOf(
                "adb shell pm grant io.github.impca201.snoozer.debug android.permission.DUMP",
                "adb shell pm grant io.github.impca201.snoozer.debug android.permission.PACKAGE_USAGE_STATS",
            ),
            BedtimeMonitor.adbCommands("io.github.impca201.snoozer.debug"),
        )
    }
}
