package io.github.impca201.snoozer.later

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import io.github.impca201.snoozer.data.LaterItem
import io.github.impca201.snoozer.snoozerApp
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** Supplies the widget's rows on Android 8 to 11. Android 12+ gets them directly from [LaterWidget]. */
class LaterWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = Factory(applicationContext)

    private class Factory(
        private val context: Context,
    ) : RemoteViewsFactory {
        private var items: List<LaterItem> = emptyList()

        override fun onCreate() = Unit

        // Runs on a binder thread, where blocking is allowed.
        override fun onDataSetChanged() {
            items = runBlocking { context.snoozerApp.repository.laterItems.filterNotNull().first() }
        }

        override fun onDestroy() = Unit

        override fun getCount(): Int = items.size

        override fun getViewAt(position: Int): RemoteViews? = items.getOrNull(position)?.let { LaterWidget.itemViews(context, it) }

        override fun getLoadingView(): RemoteViews? = null

        override fun getViewTypeCount(): Int = 1

        override fun getItemId(position: Int): Long = items.getOrNull(position)?.let(LaterWidget::stableId) ?: position.toLong()

        override fun hasStableIds(): Boolean = true
    }
}
