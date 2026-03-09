package com.noveasmibeta;

import android.Manifest;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends Activity {
    
    private static final int REQUEST_CODE_ENABLE_ADMIN = 1;
    private static final int REQUEST_CODE_RECORD_AUDIO = 2;
    private static final int REQUEST_CODE_POST_NOTIFICATIONS = 3;
    private DevicePolicyManager devicePolicyManager;
    private ComponentName adminComponent;
    private TextView statusText;
    private Button lockButton;
    private Button enableAdminButton;
    private Button voiceButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar Device Policy Manager
        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponent = new ComponentName(this, AdminReceiver.class);

        // Referencias a las vistas
        statusText = findViewById(R.id.statusText);
        lockButton = findViewById(R.id.lockButton);
        enableAdminButton = findViewById(R.id.enableAdminButton);
        voiceButton = findViewById(R.id.voiceButton);

        // Configurar botones
        lockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lockScreen();
            }
        });

        enableAdminButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enableDeviceAdmin();
            }
        });

        voiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleVoiceService();
            }
        });

        updateUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    private void updateUI() {
        boolean isAdmin = devicePolicyManager.isAdminActive(adminComponent);
        boolean serviceRunning = isServiceRunning(VoiceListenerService.class);
        
        if (isAdmin) {
            if (serviceRunning) {
                statusText.setText("✓ Escucha activa\n\nDi 'bloquear' para bloquear");
                statusText.setTextColor(0xFF4CAF50); // Verde
                voiceButton.setText("⏹ DETENER ESCUCHA");
            } else {
                statusText.setText("✓ Administrador activado\n\nToca para activar escucha continua");
                statusText.setTextColor(0xFF2196F3); // Azul
                voiceButton.setText("🎤 INICIAR ESCUCHA");
            }
            lockButton.setEnabled(true);
            voiceButton.setEnabled(true);
            enableAdminButton.setVisibility(View.GONE);
        } else {
            statusText.setText("⚠ Administrador no activado\n\nActiva los permisos primero");
            statusText.setTextColor(0xFFFF9800); // Naranja
            lockButton.setEnabled(false);
            voiceButton.setEnabled(false);
            enableAdminButton.setVisibility(View.VISIBLE);
        }
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        android.app.ActivityManager manager = (android.app.ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (android.app.ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void toggleVoiceService() {
        if (isServiceRunning(VoiceListenerService.class)) {
            stopVoiceService();
        } else {
            if (checkMicrophonePermission()) {
                startVoiceService();
            } else {
                requestMicrophonePermission();
            }
        }
    }

    private void startVoiceService() {
        try {
            Intent serviceIntent = new Intent(this, VoiceListenerService.class);
            startService(serviceIntent);
            Toast.makeText(this, "🎤 Escucha continua activada", Toast.LENGTH_SHORT).show();
            updateUI();
            
            // Iniciar tarea periódica para mantener el servicio activo
            startServiceKeeper();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            android.util.Log.e("MainActivity", "Error al iniciar servicio: " + e.getMessage());
        }
    }
    
    private void startServiceKeeper() {
        // Tarea periódica para verificar y reiniciar el servicio si es necesario
        final android.os.Handler handler = new android.os.Handler();
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (!isServiceRunning(VoiceListenerService.class)) {
                    try {
                        Intent serviceIntent = new Intent(MainActivity.this, VoiceListenerService.class);
                        startService(serviceIntent);
                        android.util.Log.d("MainActivity", "Servicio reiniciado automáticamente");
                    } catch (Exception e) {
                        android.util.Log.e("MainActivity", "Error al reiniciar servicio: " + e.getMessage());
                    }
                }
                // Repetir cada 30 segundos
                handler.postDelayed(this, 30000);
            }
        };
        // Iniciar después de 30 segundos
        handler.postDelayed(runnable, 30000);
    }

    private void stopVoiceService() {
        Intent serviceIntent = new Intent(this, VoiceListenerService.class);
        stopService(serviceIntent);
        Toast.makeText(this, "Escucha detenida", Toast.LENGTH_SHORT).show();
        updateUI();
    }

    private void enableDeviceAdmin() {
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Necesitas activar el administrador del dispositivo para permitir que 'No Veas Mi Beta' bloquee la pantalla.");
        startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN);
    }

    private void lockScreen() {
        if (devicePolicyManager.isAdminActive(adminComponent)) {
            devicePolicyManager.lockNow();
            Toast.makeText(this, "Pantalla bloqueada", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Primero activa el administrador del dispositivo", 
                          Toast.LENGTH_LONG).show();
            enableDeviceAdmin();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_CODE_ENABLE_ADMIN) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "¡Administrador activado correctamente!", 
                              Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Necesitas activar el administrador para bloquear la pantalla", 
                              Toast.LENGTH_LONG).show();
            }
            updateUI();
        } else if (requestCode == REQUEST_CODE_RECORD_AUDIO) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Permiso de micrófono concedido", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) {
            // Intentar iniciar el servicio nuevamente después de conceder permisos
            startVoiceService();
        }
    }

    private boolean checkMicrophonePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestMicrophonePermission() {
        ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.RECORD_AUDIO}, 
                REQUEST_CODE_RECORD_AUDIO);
    }
}
