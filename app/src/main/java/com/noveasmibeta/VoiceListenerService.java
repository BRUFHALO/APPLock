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
import android.os.Bundle;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.widget.Toast;
import java.util.ArrayList;

public class VoiceListenerService extends Service {
    
    private static final String CHANNEL_ID = "VoiceListenerChannel";
    private static final int NOTIFICATION_ID = 1;
    
    private SpeechRecognizer speechRecognizer;
    private DevicePolicyManager devicePolicyManager;
    private ComponentName adminComponent;
    private boolean isListening = false;

    @Override
    public void onCreate() {
        super.onCreate();
        
        try {
            devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            adminComponent = new ComponentName(this, AdminReceiver.class);
            
            // Verificar si SpeechRecognizer está disponible
            if (!SpeechRecognizer.isRecognitionAvailable(this)) {
                Toast.makeText(this, "Reconocimiento de voz no disponible en el emulador", Toast.LENGTH_LONG).show();
                android.util.Log.e("VoiceService", "SpeechRecognizer no disponible");
                return;
            }
            
            initializeSpeechRecognizer();
            startListening();
        } catch (Exception e) {
            android.util.Log.e("VoiceService", "Error en onCreate: " + e.getMessage());
            Toast.makeText(this, "Error al iniciar servicio de voz: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


    private void initializeSpeechRecognizer() {
        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            if (speechRecognizer == null) {
                android.util.Log.e("VoiceService", "No se pudo crear SpeechRecognizer");
                return;
            }
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                isListening = true;
                android.util.Log.d("VoiceService", "Escuchando comandos...");
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
                String errorMsg = "Error de reconocimiento: " + error;
                android.util.Log.e("VoiceService", errorMsg);
                
                // Reintentar después de un error con más robustez
                new android.os.Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (!isListening && speechRecognizer != null) {
                                startListening();
                            }
                        } catch (Exception e) {
                            android.util.Log.e("VoiceService", "Error al reiniciar escucha: " + e.getMessage());
                        }
                    }
                }, 2000);
            }

            @Override
            public void onResults(Bundle results) {
                isListening = false;
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                
                if (matches != null && !matches.isEmpty()) {
                    boolean commandFound = false;
                    
                    for (String match : matches) {
                        String lowerMatch = match.toLowerCase();
                        android.util.Log.d("VoiceService", "Reconocido: " + lowerMatch);
                        
                        if (lowerMatch.contains("bloquear") || lowerMatch.contains("bloquea") || 
                            lowerMatch.contains("lock") || lowerMatch.contains("bloqueado") ||
                            lowerMatch.contains("bloque") || lowerMatch.contains("locker")) {
                            commandFound = true;
                            break;
                        }
                    }
                    
                    if (commandFound) {
                        android.util.Log.d("VoiceService", "Comando detectado - Bloqueando pantalla...");
                        lockScreen();
                        
                        // Reiniciar escucha después de bloquear
                        new android.os.Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                startListening();
                            }
                        }, 2000);
                    } else {
                        // Continuar escuchando
                        startListening();
                    }
                } else {
                    // Continuar escuchando
                    startListening();
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {}

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });
        } catch (Exception e) {
            android.util.Log.e("VoiceService", "Error al inicializar SpeechRecognizer: " + e.getMessage());

        }
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
                android.util.Log.d("VoiceService", "Escuchando 'bloquear'...");
            } catch (Exception e) {
                android.util.Log.e("VoiceService", "Error al iniciar escucha: " + e.getMessage());
                Toast.makeText(this, "Error al iniciar escucha: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void lockScreen() {
        if (devicePolicyManager.isAdminActive(adminComponent)) {
            devicePolicyManager.lockNow();
            Toast.makeText(this, "✓ Pantalla bloqueada por comando de voz", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Permisos de administrador no activados", Toast.LENGTH_LONG).show();
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        android.util.Log.d("VoiceService", "Servicio iniciado");
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        android.util.Log.d("VoiceService", "Tarea removida - reiniciando servicio");
        // Reiniciar el servicio cuando la app es cerrada
        Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
        restartServiceIntent.setPackage(getPackageName());
        getApplicationContext().startService(restartServiceIntent);
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        android.util.Log.d("VoiceService", "Servicio destruido - limpiando recursos");
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
