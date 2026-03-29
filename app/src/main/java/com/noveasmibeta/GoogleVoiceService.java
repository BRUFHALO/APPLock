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
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;

public class GoogleVoiceService extends Service {

    private static final String TAG = "GoogleVoiceService";
    private static final String CHANNEL_ID = "GoogleVoiceChannel";
    private static final int NOTIFICATION_ID = 2001;

    private SpeechRecognizer speechRecognizer;
    private DevicePolicyManager devicePolicyManager;
    private ComponentName adminComponent;
    private PowerManager.WakeLock wakeLock;
    private AudioManager audioManager;
    private boolean isListening = false;
    private int originalNotificationVolume = -1;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "onCreate - Inicializando servicio Google Voice");

        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponent = new ComponentName(this, AdminReceiver.class);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // WakeLock para mantener activo
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NoVeasMiBeta::GoogleVoiceWakeLock");
        wakeLock.acquire();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand - Iniciando foreground");

        try {
            createNotificationChannel();
            startForeground(NOTIFICATION_ID, createNotification());

            if (!SpeechRecognizer.isRecognitionAvailable(this)) {
                Log.e(TAG, "SpeechRecognizer no disponible");
                stopSelf();
                return START_NOT_STICKY;
            }

            initializeSpeechRecognizer();
            silenceNotificationStream();
            startListening();

        } catch (Exception e) {
            Log.e(TAG, "Error en onStartCommand: " + e.getMessage());
            e.printStackTrace();
        }

        return START_STICKY;
    }

    private void silenceNotificationStream() {
        if (audioManager != null) {
            try {
                originalNotificationVolume = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
                audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0);
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
                Log.d(TAG, "Streams de notificación y música silenciados");
            } catch (Exception e) {
                Log.e(TAG, "Error al silenciar streams: " + e.getMessage());
            }
        }
    }

    private void restoreNotificationStream() {
        if (audioManager != null && originalNotificationVolume >= 0) {
            try {
                audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, originalNotificationVolume, 0);
                Log.d(TAG, "Stream de notificación restaurado");
            } catch (Exception e) {
                Log.e(TAG, "Error al restaurar stream: " + e.getMessage());
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Servicio de Voz Online",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Reconocimiento de voz online");
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
            .setContentText("🌐 Escucha online activa")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(Notification.PRIORITY_LOW)
            .build();
    }

    private void initializeSpeechRecognizer() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        if (speechRecognizer == null) {
            Log.e(TAG, "No se pudo crear SpeechRecognizer");
            return;
        }

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                isListening = true;
                Log.d(TAG, "Listo para escuchar (Online)");
            }

            @Override
            public void onBeginningOfSpeech() {}

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {
                isListening = false;
            }

            @Override
            public void onError(int error) {
                isListening = false;
                Log.e(TAG, "Error de reconocimiento: " + error);

                // Reintentar
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    try {
                        if (speechRecognizer != null) {
                            silenceNotificationStream();
                            startListening();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error al reiniciar: " + e.getMessage());
                    }
                }, 1500);
            }

            @Override
            public void onResults(Bundle results) {
                isListening = false;
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

                if (matches != null && !matches.isEmpty()) {
                    boolean commandFound = false;

                    for (String match : matches) {
                        String lower = match.toLowerCase();
                        Log.d(TAG, "Reconocido (Online): " + lower);

                        if (lower.contains("bloquear") || lower.contains("bloquea") ||
                            lower.contains("lock") || lower.contains("bloqueado") ||
                            lower.contains("bloque") || lower.contains("locker")) {
                            commandFound = true;
                            break;
                        }
                    }

                    if (commandFound) {
                        Log.d(TAG, "Comando detectado - Bloqueando pantalla");
                        lockScreen();

                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                            silenceNotificationStream();
                            startListening();
                        }, 2000);
                    } else {
                        silenceNotificationStream();
                        startListening();
                    }
                } else {
                    silenceNotificationStream();
                    startListening();
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {}

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });
    }

    private void startListening() {
        if (!isListening && speechRecognizer != null) {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES");
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

            try {
                speechRecognizer.startListening(intent);
                Log.d(TAG, "Escuchando 'bloquear' (Online)...");
            } catch (Exception e) {
                Log.e(TAG, "Error al iniciar escucha: " + e.getMessage());
            }
        }
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

        restoreNotificationStream();

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
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
