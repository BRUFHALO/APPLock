package com.noveasmibeta;

import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;
import org.vosk.android.StorageService;
import java.io.IOException;

public class VoskVoiceService extends Service implements RecognitionListener {
    
    private Model model;
    private SpeechService speechService;
    private DevicePolicyManager devicePolicyManager;
    private ComponentName adminComponent;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponent = new ComponentName(this, AdminReceiver.class);
        
        // Inicializar modelo de Vosk en segundo plano
        initModel();
    }
    
    private void initModel() {
        // Intentar cargar modelo desde assets
        new Thread(() -> {
            try {
                android.util.Log.d("VoskService", "Intentando cargar modelo desde assets...");
                
                // Verificar si existe la carpeta model-es en assets
                String[] assetFiles = getAssets().list("");
                boolean modelExists = false;
                for (String file : assetFiles) {
                    if (file.equals("model-es")) {
                        modelExists = true;
                        break;
                    }
                }
                
                if (!modelExists) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "⚠ Modelo de voz no encontrado en assets/model-es", Toast.LENGTH_LONG).show();
                        android.util.Log.e("VoskService", "Modelo no encontrado. Descarga el modelo y colócalo en app/src/main/assets/model-es/");
                    });
                    return;
                }
                
                StorageService.unpack(this, "model-es", "model",
                    (model) -> {
                        this.model = model;
                        android.util.Log.d("VoskService", "Modelo cargado exitosamente");
                        recognizeMicrophone();
                    },
                    (exception) -> {
                        android.util.Log.e("VoskService", "Error al cargar modelo: " + exception.getMessage());
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Error al cargar modelo: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                        });
                    });
            } catch (Exception e) {
                android.util.Log.e("VoskService", "Error al verificar modelo: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
    
    private void runOnUiThread(Runnable action) {
        new android.os.Handler(android.os.Looper.getMainLooper()).post(action);
    }
    
    private void recognizeMicrophone() {
        if (speechService != null) {
            speechService.stop();
            speechService = null;
        } else {
            try {
                Recognizer rec = new Recognizer(model, 16000.0f, "[\"bloquear\", \"bloquea\", \"lock\", \"bloqueado\", \"bloque\", \"locker\", \"[unk]\"]");
                speechService = new SpeechService(rec, 16000.0f);
                speechService.startListening(this);
                android.util.Log.d("VoskService", "Reconocimiento iniciado - Escuchando 'bloquear'");
            } catch (IOException e) {
                android.util.Log.e("VoskService", "Error al iniciar reconocimiento: " + e.getMessage());
            }
        }
    }
    
    @Override
    public void onPartialResult(String hypothesis) {
        android.util.Log.d("VoskService", "Partial: " + hypothesis);
    }
    
    @Override
    public void onResult(String hypothesis) {
        android.util.Log.d("VoskService", "Result: " + hypothesis);
        
        if (hypothesis != null) {
            String lowerHypothesis = hypothesis.toLowerCase();
            
            if (lowerHypothesis.contains("bloquear") || 
                lowerHypothesis.contains("bloquea") || 
                lowerHypothesis.contains("lock") || 
                lowerHypothesis.contains("bloqueado") ||
                lowerHypothesis.contains("bloque") || 
                lowerHypothesis.contains("locker")) {
                
                android.util.Log.d("VoskService", "¡Comando detectado! Bloqueando pantalla...");
                lockScreen();
            }
        }
    }
    
    @Override
    public void onFinalResult(String hypothesis) {
        android.util.Log.d("VoskService", "Final: " + hypothesis);
        onResult(hypothesis);
    }
    
    @Override
    public void onError(Exception e) {
        android.util.Log.e("VoskService", "Error: " + e.getMessage());
    }
    
    @Override
    public void onTimeout() {
        android.util.Log.d("VoskService", "Timeout - continuando escucha");
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
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        if (speechService != null) {
            speechService.stop();
            speechService.shutdown();
        }
        
        if (model != null) {
            model.close();
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
