package com.noveasmibeta;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

public class BcvNotificationService extends Service {

    private static final String TAG = "BcvNotifService";
    private static final String CHANNEL_ID = "bcv_rate_channel_v2";
    private static final int NOTIFICATION_ID = 2001;
    private static final long UPDATE_INTERVAL = 3600000; // 1 hora

    private Handler handler;
    private Runnable updateRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: creando canal de notificación");
        createNotificationChannel();
        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Manejar acción REFRESH
        if (intent != null && "REFRESH".equals(intent.getAction())) {
            fetchAndNotify();
            return START_STICKY;
        }

        // Mostrar notificación inicial
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, buildNotification("Cargando...", "Cargando...", "Actualizando tasas BCV..."),
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NOTIFICATION_ID, buildNotification("Cargando...", "Cargando...", "Actualizando tasas BCV..."));
        }

        // Obtener tasas y actualizar
        fetchAndNotify();

        // Programar actualizaciones periódicas
        if (updateRunnable == null) {
            updateRunnable = new Runnable() {
                @Override
                public void run() {
                    fetchAndNotify();
                    handler.postDelayed(this, UPDATE_INTERVAL);
                }
            };
            handler.postDelayed(updateRunnable, UPDATE_INTERVAL);
        }

        return START_STICKY;
    }

    private void fetchAndNotify() {
        try {
            Log.d(TAG, "fetchAndNotify: obteniendo tasas BCV");
            BcvScraper scraper = new BcvScraper(getApplicationContext());
            scraper.obtenerTasas(new BcvScraper.BcvCallback() {
                @Override
                public void onSuccess(BcvScraper.BcvData data) {
                    handler.post(() -> {
                        try {
                            String dolar = String.format("%.2f Bs", data.dolar);
                            String euro = String.format("%.2f Bs", data.euro);
                            String info = data.fechaValor != null ? data.fechaValor : "";
                            updateNotification(dolar, euro, info);
                        } catch (Exception e) {
                            Log.e(TAG, "Error actualizando notificación: " + e.getMessage());
                        }
                    });
                }

                @Override
                public void onError(String error, BcvScraper.BcvData cachedData) {
                    handler.post(() -> {
                        try {
                            if (cachedData != null) {
                                String dolar = String.format("%.2f Bs", cachedData.dolar);
                                String euro = String.format("%.2f Bs", cachedData.euro);
                                updateNotification(dolar, euro, "Datos en caché");
                            } else {
                                updateNotification("---", "---", "Sin conexión");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error en notificación error: " + e.getMessage());
                        }
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error fetchAndNotify: " + e.getMessage());
        }
    }

    private Notification buildNotification(String dolarRate, String euroRate, String subtitle) {
        // Intent para abrir calculadora al tocar la notificación
        Intent calcIntent = new Intent(this, CalculatorActivity.class);
        calcIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        calcIntent.putExtra("tasa_dolar", 0.0);
        calcIntent.putExtra("tasa_euro", 0.0);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, calcIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Intent para refrescar
        Intent refreshIntent = new Intent(this, BcvNotificationService.class);
        refreshIntent.setAction("REFRESH");
        PendingIntent refreshPI = PendingIntent.getService(
                this, 1, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String title = "$ " + dolarRate + "  |  € " + euroRate;

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        builder.setSmallIcon(android.R.drawable.ic_menu_today)
                .setContentTitle(title)
                .setContentText(subtitle)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setShowWhen(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setCategory(Notification.CATEGORY_STATUS)
                .setPriority(Notification.PRIORITY_DEFAULT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            builder.addAction(new Notification.Action.Builder(
                    null, "↻ Actualizar", refreshPI).build());
        }

        return builder.build();
    }

    private void updateNotification(String dolarRate, String euroRate, String subtitle) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, buildNotification(dolarRate, euroRate, subtitle));
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Tasa BCV",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Muestra la tasa del dólar y euro BCV en la barra de notificaciones");
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
        }
        Log.d(TAG, "onDestroy: servicio detenido");
    }
}
