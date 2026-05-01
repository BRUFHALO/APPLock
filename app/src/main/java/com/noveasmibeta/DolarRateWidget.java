package com.noveasmibeta;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DolarRateWidget extends AppWidgetProvider {

    private static final String TAG = "DolarRateWidget";
    private static final String ACTION_REFRESH = "com.noveasmibeta.WIDGET_REFRESH";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            try {
                setupWidget(context, appWidgetManager, appWidgetId);
                fetchData(context.getApplicationContext(), appWidgetManager, appWidgetId);
            } catch (Exception e) {
                android.util.Log.e(TAG, "Error en onUpdate: " + e.getMessage());
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        try {
            if (ACTION_REFRESH.equals(intent.getAction())) {
                AppWidgetManager mgr = AppWidgetManager.getInstance(context);
                ComponentName cn = new ComponentName(context, DolarRateWidget.class);
                int[] ids = mgr.getAppWidgetIds(cn);
                for (int id : ids) {
                    fetchData(context.getApplicationContext(), mgr, id);
                }
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error en onReceive: " + e.getMessage());
        }
    }

    private void setupWidget(Context context, AppWidgetManager mgr, int widgetId) {
        RemoteViews views = buildViews(context);
        mgr.updateAppWidget(widgetId, views);
    }

    private RemoteViews buildViews(Context context) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_dolar_rate);

        // Tocar el widget abre la calculadora
        Intent calcIntent = new Intent(context, CalculatorActivity.class);
        calcIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        calcIntent.putExtra("tasa_dolar", 0.0);
        calcIntent.putExtra("tasa_euro", 0.0);
        PendingIntent calcPI = PendingIntent.getActivity(
                context, 0, calcIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widgetRoot, calcPI);

        // Botón refresh
        Intent refreshIntent = new Intent(context, DolarRateWidget.class);
        refreshIntent.setAction(ACTION_REFRESH);
        PendingIntent refreshPI = PendingIntent.getBroadcast(
                context, 0, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widgetRefreshButton, refreshPI);

        return views;
    }

    private void fetchData(Context appContext, AppWidgetManager mgr, int widgetId) {
        try {
            // Mostrar loading
            RemoteViews loading = buildViews(appContext);
            loading.setTextViewText(R.id.widgetDolarRate, "...");
            loading.setTextViewText(R.id.widgetEuroRate, "...");
            loading.setTextViewText(R.id.widgetUsdtRate, "...");
            loading.setTextViewText(R.id.widgetLastUpdate, "Cargando...");
            mgr.updateAppWidget(widgetId, loading);

            // Fetch BCV data
            BcvScraper scraper = new BcvScraper(appContext);
            scraper.obtenerTasas(new BcvScraper.BcvCallback() {
                @Override
                public void onSuccess(BcvScraper.BcvData data) {
                    // After BCV success, fetch Binance
                    fetchBinanceData(appContext, mgr, widgetId, data.dolar, data.euro, false);
                    scraper.destroy();
                }

                @Override
                public void onError(String error, BcvScraper.BcvData cachedData) {
                    // On BCV error, still try to get Binance data
                    double dolar = (cachedData != null) ? cachedData.dolar : 0;
                    double euro = (cachedData != null) ? cachedData.euro : 0;
                    fetchBinanceData(appContext, mgr, widgetId, dolar, euro, true);
                    scraper.destroy();
                }
            });
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error fetchData: " + e.getMessage());
        }
    }

    private void fetchBinanceData(Context appContext, AppWidgetManager mgr, int widgetId,
                                   double bcvDolar, double bcvEuro, boolean isCache) {
        try {
            BinanceScraper binanceScraper = new BinanceScraper(appContext);
            binanceScraper.fetchUsdtVesPrice(new BinanceScraper.BinanceCallback() {
                @Override
                public void onSuccess(BinanceScraper.BinanceData data) {
                    try {
                        RemoteViews v = buildViews(appContext);
                        // BCV rates
                        if (bcvDolar > 0) {
                            v.setTextViewText(R.id.widgetDolarRate, String.format("%.2f", bcvDolar));
                        } else {
                            v.setTextViewText(R.id.widgetDolarRate, "---");
                        }
                        if (bcvEuro > 0) {
                            v.setTextViewText(R.id.widgetEuroRate, String.format("%.2f", bcvEuro));
                        } else {
                            v.setTextViewText(R.id.widgetEuroRate, "---");
                        }
                        // Binance USDT rate
                        if (data.price > 0) {
                            v.setTextViewText(R.id.widgetUsdtRate, String.format("%.2f", data.price));
                        } else {
                            v.setTextViewText(R.id.widgetUsdtRate, "---");
                        }
                        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                        String updateText = isCache || data.isCache ? "Caché" : sdf.format(new Date());
                        v.setTextViewText(R.id.widgetLastUpdate, updateText);
                        mgr.updateAppWidget(widgetId, v);
                    } catch (Exception e) {
                        android.util.Log.e(TAG, "Error actualizando widget: " + e.getMessage());
                    }
                    binanceScraper.destroy();
                }

                @Override
                public void onError(String error, BinanceScraper.BinanceData cachedData) {
                    try {
                        RemoteViews v = buildViews(appContext);
                        // BCV rates
                        if (bcvDolar > 0) {
                            v.setTextViewText(R.id.widgetDolarRate, String.format("%.2f", bcvDolar));
                        } else {
                            v.setTextViewText(R.id.widgetDolarRate, "---");
                        }
                        if (bcvEuro > 0) {
                            v.setTextViewText(R.id.widgetEuroRate, String.format("%.2f", bcvEuro));
                        } else {
                            v.setTextViewText(R.id.widgetEuroRate, "---");
                        }
                        // Binance USDT rate (from cache or error)
                        if (cachedData != null && cachedData.price > 0) {
                            v.setTextViewText(R.id.widgetUsdtRate, String.format("%.2f", cachedData.price));
                        } else {
                            v.setTextViewText(R.id.widgetUsdtRate, "---");
                        }
                        v.setTextViewText(R.id.widgetLastUpdate, "Caché");
                        mgr.updateAppWidget(widgetId, v);
                    } catch (Exception e) {
                        android.util.Log.e(TAG, "Error en widget error: " + e.getMessage());
                    }
                    binanceScraper.destroy();
                }
            });
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error fetchBinanceData: " + e.getMessage());
        }
    }
}
