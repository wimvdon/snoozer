package io.github.impca201.snoozer.bedtime

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Reads whether Pixel's Bedtime mode is on, using `dumpsys notification --proto`. That needs
 * two permissions only adb can grant (see the Settings screen). Experimental: the dump format is
 * internal to Android, so an unexpected layout is reported as [Status.Unreadable], never guessed.
 */
object BedtimeMonitor {
    enum class Status { NeedsSetup, Unreadable, On, Off }

    val ADB_COMMANDS: List<String> =
        listOf(
            "adb shell pm grant io.github.impca201.snoozer android.permission.DUMP",
            "adb shell pm grant io.github.impca201.snoozer android.permission.PACKAGE_USAGE_STATS",
        )

    private const val DUMPSYS_TIMEOUT_SECONDS = 8L

    private val mutableStatus = MutableStateFlow<Status?>(null)

    /** Last reading, for the Settings screen; null until the first check. */
    val status: StateFlow<Status?> = mutableStatus.asStateFlow()

    fun hasPermissions(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.DUMP) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.PACKAGE_USAGE_STATS) ==
            PackageManager.PERMISSION_GRANTED

    /** Takes one reading (runs dumpsys off the main thread) and publishes it to [status]. */
    suspend fun read(context: Context): Status {
        val result =
            if (!hasPermissions(context)) {
                Status.NeedsSetup
            } else {
                when (runDumpsys()?.let(BedtimeParser::isBedtimeActive)) {
                    true -> Status.On
                    false -> Status.Off
                    null -> Status.Unreadable
                }
            }
        mutableStatus.value = result
        return result
    }

    private suspend fun runDumpsys(): ByteArray? =
        withContext(Dispatchers.IO) {
            try {
                val process = ProcessBuilder("dumpsys", "notification", "--proto").start()
                coroutineScope {
                    // Read stdout concurrently so a large dump can't fill the pipe and stall.
                    val output = async { process.inputStream.use { it.readBytes() } }
                    val finished = process.waitFor(DUMPSYS_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    if (!finished) {
                        process.destroy()
                        null
                    } else {
                        output.await().takeIf { process.exitValue() == 0 && it.isNotEmpty() }
                    }
                }
            } catch (_: IOException) {
                null
            } catch (_: SecurityException) {
                null
            }
        }
}
