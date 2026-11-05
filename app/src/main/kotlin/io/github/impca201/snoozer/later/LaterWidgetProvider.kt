package io.github.impca201.snoozer.later

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import io.github.impca201.snoozer.snoozerApp
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The "Do later" widget. Changes to the list are pushed by `SnoozerApp`; this only answers the
 * system when a widget is added or the launcher asks for fresh content.
 */
class LaterWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val pending = goAsync()
        val app = context.snoozerApp
        app.appScope.launch {
            try {
                LaterWidget.update(app, app.repository.laterItems.filterNotNull().first())
            } finally {
                pending.finish()
            }
        }
    }
}
