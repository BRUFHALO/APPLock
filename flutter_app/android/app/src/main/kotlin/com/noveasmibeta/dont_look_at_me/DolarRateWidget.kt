package com.noveasmibeta.dont_look_at_me

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.widget.RemoteViews
import es.antonborri.home_widget.HomeWidgetBackgroundIntent
import es.antonborri.home_widget.HomeWidgetLaunchIntent
import es.antonborri.home_widget.HomeWidgetProvider

class DolarRateWidget : HomeWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
        widgetData: SharedPreferences
    ) {
        for (widgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_dolar_rate)

            // Datos escritos desde Dart con HomeWidget.saveWidgetData.
            views.setTextViewText(
                R.id.widgetDolarRate,
                widgetData.getString("widget_dolar", "---.--")
            )
            views.setTextViewText(
                R.id.widgetEuroRate,
                widgetData.getString("widget_euro", "---.--")
            )
            views.setTextViewText(
                R.id.widgetUsdtRate,
                widgetData.getString("widget_usdt", "---.--")
            )
            views.setTextViewText(
                R.id.widgetLastUpdate,
                widgetData.getString("widget_last_update", "Sin datos")
            )

            // Tocar el widget abre la app.
            views.setOnClickPendingIntent(
                R.id.widgetRoot,
                HomeWidgetLaunchIntent.getActivity(context, MainActivity::class.java)
            )

            // Botón refresh -> callback Dart en segundo plano.
            views.setOnClickPendingIntent(
                R.id.widgetRefreshButton,
                HomeWidgetBackgroundIntent.getBroadcast(
                    context,
                    Uri.parse("dlam://widget_refresh")
                )
            )

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}
