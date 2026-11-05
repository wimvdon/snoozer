package io.github.impca201.snoozer.later

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.format.DateUtils
import android.view.View
import android.widget.RemoteViews
import io.github.impca201.snoozer.R
import io.github.impca201.snoozer.data.LaterItem
import io.github.impca201.snoozer.ui.Destination
import io.github.impca201.snoozer.ui.MainActivity
import java.text.DateFormat

/** Builds the "Do later" home screen widget. Plain RemoteViews: no widget library needed. */
object LaterWidget {
    fun update(
        context: Context,
        items: List<LaterItem>,
    ) {
        val manager = AppWidgetManager.getInstance(context)
        val ids =
            try {
                manager.getAppWidgetIds(ComponentName(context, LaterWidgetProvider::class.java))
            } catch (_: RuntimeException) {
                return // Some devices have no widget host service.
            }
        if (ids.isEmpty()) return
        for (id in ids) manager.updateAppWidget(id, views(context, id, items))
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            @Suppress("DEPRECATION") // Only reached below Android 12, where the list comes from LaterWidgetService.
            manager.notifyAppWidgetViewDataChanged(ids, R.id.later_list)
        }
    }

    private fun views(
        context: Context,
        appWidgetId: Int,
        items: List<LaterItem>,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_later)
        views.setOnClickPendingIntent(R.id.later_add, pickerIntent(context))
        views.setOnClickPendingIntent(R.id.later_title, appIntent(context))
        views.setPendingIntentTemplate(R.id.later_list, actionTemplate(context))
        views.setEmptyView(R.id.later_list, R.id.later_empty)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val collection =
                RemoteViews.RemoteCollectionItems
                    .Builder()
                    .setHasStableIds(true)
                    .setViewTypeCount(1)
            items.forEach { collection.addItem(stableId(it), itemViews(context, it)) }
            views.setRemoteAdapter(R.id.later_list, collection.build())
        } else {
            val adapter =
                Intent(context, LaterWidgetService::class.java)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            // A unique data URI per widget, or the system reuses one adapter for every widget.
            adapter.data = Uri.parse(adapter.toUri(Intent.URI_INTENT_SCHEME))
            @Suppress("DEPRECATION")
            views.setRemoteAdapter(R.id.later_list, adapter)
        }
        return views
    }

    fun stableId(item: LaterItem): Long = item.key.hashCode().toLong()

    fun itemViews(
        context: Context,
        item: LaterItem,
    ): RemoteViews {
        val row = RemoteViews(context.packageName, R.layout.widget_later_item)
        val headline = item.title.ifBlank { item.text }
        row.setTextViewText(R.id.later_item_title, headline)
        val supporting = if (item.title.isBlank()) "" else item.text
        row.setTextViewText(R.id.later_item_text, supporting)
        row.setViewVisibility(R.id.later_item_text, if (supporting.isBlank()) View.GONE else View.VISIBLE)
        row.setTextViewText(
            R.id.later_item_meta,
            context.getString(R.string.later_item_meta, item.appLabel, postedTime(item.postedAt)),
        )
        row.setOnClickFillInIntent(R.id.later_item, LaterActionActivity.fillIn(item))
        return row
    }

    /** The time for today's notifications, otherwise the date. */
    fun postedTime(postedAt: Long): CharSequence =
        DateUtils.formatSameDayTime(postedAt, System.currentTimeMillis(), DateFormat.SHORT, DateFormat.SHORT)

    private fun pickerIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            REQUEST_PICKER,
            LaterPickerActivity.intent(context),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun appIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            REQUEST_APP,
            MainActivity.intent(context, Destination.Home).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    /** Mutable so each row can fill in which notification it is; the target class is fixed. */
    private fun actionTemplate(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            REQUEST_ACTION,
            Intent(context, LaterActionActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private const val REQUEST_PICKER = 1
    private const val REQUEST_APP = 2
    private const val REQUEST_ACTION = 3
}
