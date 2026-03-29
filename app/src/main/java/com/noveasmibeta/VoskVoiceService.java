package com.noveasmibeta;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;
import org.vosk.android.StorageService;
import java.io.IOException;

public class VoskVoiceService extends Service implements RecognitionListener {

    private static final String TAG = "VoskService";
    private static final String CHANNEL_ID = "VoskVoiceChannel";
    private static final int NOTIFICATION_ID = 1001;

    private Model model;
    private SpeechService speechService;
    private DevicePolicyManager devicePolicyManager;
    private ComponentName adminComponent;
    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "onCreate - Inicializando servicio Vosk");

        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponent = new ComponentName(this, AdminReceiver.class);

        // WakeLock para mantener activo
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NoVeasMiBeta::VoskWakeLock");
        wakeLock.acquire();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand - Iniciando foreground");

        try {
            createNotificationChannel();
            startForeground(NOTIFICATION_ID, createNotification());

            if (model == null) {
                initModel();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error en onStartCommand: " + e.getMessage());
            e.printStackTrace();
        }

        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Servicio de Voz Offline",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Reconocimiento de voz offline");
            channel.setShowBadge(false);
            channel.enableVibration(false);
            channel.setSound(null, null);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        return builder
            .setContentTitle("No Veas Mi Beta")
            .setContentText("🔇 Escucha offline activa")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(Notification.PRIORITY_LOW)
            .build();
    }

    private void initModel() {
        new Thread(() -> {
            try {
                Log.d(TAG, "Cargando modelo desde assets...");

                String[] assetFiles = getAssets().list("");
                boolean modelExists = false;
                for (String file : assetFiles) {
                    if (file.equals("model-es")) {
                        modelExists = true;
                        break;
                    }
                }

                if (!modelExists) {
                    Log.e(TAG, "Modelo no encontrado en assets/model-es/");
                    runOnUiThread(() ->
                        Toast.makeText(this, "Modelo de voz no encontrado", Toast.LENGTH_LONG).show());
                    return;
                }

                StorageService.unpack(this, "model-es", "model",
                    (model) -> {
                        this.model = model;
                        Log.d(TAG, "Modelo cargado exitosamente");
                        startRecognition();
                    },
                    (exception) -> {
                        Log.e(TAG, "Error al cargar modelo: " + exception.getMessage());
                        runOnUiThread(() ->
                            Toast.makeText(this, "Error modelo: " + exception.getMessage(), Toast.LENGTH_LONG).show());
                    });
            } catch (Exception e) {
                Log.e(TAG, "Error al verificar modelo: " + e.getMessage());
            }
        }).start();
    }

    private void runOnUiThread(Runnable action) {
        new android.os.Handler(android.os.Looper.getMainLooper()).post(action);
    }

    private void startRecognition() {
        if (speechService != null) {
            speechService.stop();
            speechService = null;
        }
        try {
            Recognizer rec = new Recognizer(model, 16000.0f,
                "[\"bloquear\", \"bloquea\", \"lock\", \"bloqueado\", \"bloque\", \"locker\", \"[unk]\"]");
            speechService = new SpeechService(rec, 16000.0f);
            speechService.startListening(this);
            Log.d(TAG, "Reconocimiento offline iniciado");
        } catch (IOException e) {
            Log.e(TAG, "Error al iniciar reconocimiento: " + e.getMessage());
        }
    }

    @Override
    public void onPartialResult(String hypothesis) {
        // Solo log en debug
    }

    @Override
    public void onResult(String hypothesis) {
        if (hypothesis != null) {
            String lower = hypothesis.toLowerCase();
            if (lower.contains("bloquear") || lower.contains("bloquea") ||
                lower.contains("lock") || lower.contains("bloqueado") ||
                lower.contains("bloque") || lower.contains("locker")) {

                Log.d(TAG, "Comando detectado (Offline) - Bloqueando");
                lockScreen();
            }
        }
    }

    @Override
    public void onFinalResult(String hypothesis) {
        onResult(hypothesis);
    }

    @Override
    public void onError(Exception e) {
        Log.e(TAG, "Error: " + e.getMessage());
    }

    @Override
    public void onTimeout() {
        Log.d(TAG, "Timeout - continuando");
    }

    private void lockScreen() {
        if (devicePolicyManager.isAdminActive(adminComponent)) {
            devicePolicyManager.lockNow();
            Log.d(TAG, "Pantalla bloqueada exitosamente");
        } else {
            Log.w(TAG, "Device Admin no activo");
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "Tarea removida - Reiniciando");
        Intent restartIntent = new Intent(getApplicationContext(), this.getClass());
        restartIntent.setPackage(getPackageName());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getApplicationContext().startForegroundService(restartIntent);
        } else {
            getApplicationContext().startService(restartIntent);
        }
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Servicio destruido");

        if (speechService != null) {
            speechService.stop();
            speechService.shutdown();
        }

        if (model != null) {
            model.close();
        }

        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
