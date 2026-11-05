package io.github.impca201.snoozer.later

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import io.github.impca201.snoozer.R
import io.github.impca201.snoozer.data.LaterItem
import java.io.File
import java.security.MessageDigest

/**
 * The small icon of each saved notification, kept as a white-on-transparent PNG so the widget and
 * Snoozer's own notifications can show it after the original is gone (also after a restart).
 * Stored in the no-backup folder: icons are meaningless without the notifications.
 */
object LaterIcons {
    private const val SIZE_PX = 96
    private const val FOLDER = "later_icons"

    /** Renders [icon] (the original notification's small icon) and stores it for [key]. Best effort. */
    fun save(
        context: Context,
        key: String,
        icon: Icon?,
    ) {
        val drawable =
            try {
                icon?.loadDrawable(context)
            } catch (e: RuntimeException) {
                Log.w("Snoozer", "Notification icon not readable", e)
                null
            } ?: return
        val bitmap = createBitmap(SIZE_PX, SIZE_PX)
        drawable.drawInto(Canvas(bitmap), 0, SIZE_PX, Color.WHITE)
        try {
            val file = file(context, key)
            file.parentFile?.mkdirs()
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } catch (e: java.io.IOException) {
            Log.w("Snoozer", "Notification icon not saved", e)
        }
    }

    /** The stored white glyph, or Snoozer's bell when there is none (items saved before icons were kept). */
    fun glyph(
        context: Context,
        key: String,
    ): Bitmap = stored(context, key) ?: fallback(context)

    /** The glyph on a circle in the notification's colour, as the shade shows it. For the widget. */
    fun badge(
        context: Context,
        item: LaterItem,
        fallbackColor: Int,
    ): Bitmap {
        val badge = createBitmap(SIZE_PX, SIZE_PX)
        val canvas = Canvas(badge)
        val circle = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = opaque(item.color, fallbackColor) }
        canvas.drawCircle(SIZE_PX / 2f, SIZE_PX / 2f, SIZE_PX / 2f, circle)
        val inset = SIZE_PX / 5f
        val glyphSize = (SIZE_PX - 2 * inset).toInt()
        val glyph = glyph(context, item.key).scale(glyphSize, glyphSize)
        canvas.drawBitmap(glyph, inset, inset, Paint(Paint.FILTER_BITMAP_FLAG))
        return badge
    }

    /** Deletes icons of notifications that are no longer saved. */
    fun prune(
        context: Context,
        keys: Collection<String>,
    ) {
        val keep = keys.mapTo(HashSet()) { file(context, it).name }
        File(context.noBackupFilesDir, FOLDER).listFiles()?.forEach { if (it.name !in keep) it.delete() }
    }

    private fun stored(
        context: Context,
        key: String,
    ): Bitmap? = file(context, key).takeIf { it.isFile }?.let { BitmapFactory.decodeFile(it.path) }

    private fun fallback(context: Context): Bitmap {
        val bitmap = createBitmap(SIZE_PX, SIZE_PX)
        ContextCompat.getDrawable(context, R.drawable.ic_notifications)?.drawInto(Canvas(bitmap), 0, SIZE_PX, Color.WHITE)
        return bitmap
    }

    private fun Drawable.drawInto(
        canvas: Canvas,
        start: Int,
        end: Int,
        tint: Int,
    ) {
        setBounds(start, start, end, end)
        colorFilter = PorterDuffColorFilter(tint, PorterDuff.Mode.SRC_IN)
        draw(canvas)
    }

    /** Apps often leave the colour unset (0) or translucent; the badge needs a solid circle. */
    private fun opaque(
        color: Int,
        fallback: Int,
    ): Int = if (Color.alpha(color) < 0x80) fallback else color or 0xFF000000.toInt()

    private fun file(
        context: Context,
        key: String,
    ): File {
        val digest = MessageDigest.getInstance("SHA-1").digest(key.toByteArray())
        return File(File(context.noBackupFilesDir, FOLDER), digest.joinToString("") { "%02x".format(it) } + ".png")
    }
}
