package com.noveasmibeta;

import android.Manifest;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.cardview.widget.CardView;
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

    // BCV UI
    private TextView bcvDolarPrice;
    private TextView bcvEuroPrice;
    private TextView bcvFechaValor;
    private TextView bcvLastUpdate;
    private TextView bcvVigencia;
    private TextView bcvCacheIndicator;
    private Button bcvRefreshButton;
    private BcvScraper bcvScraper;
    private Handler mainHandler;

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

        // BCV UI References
        bcvDolarPrice = findViewById(R.id.bcvDolarPrice);
        bcvEuroPrice = findViewById(R.id.bcvEuroPrice);
        bcvFechaValor = findViewById(R.id.bcvFechaValor);
        bcvLastUpdate = findViewById(R.id.bcvLastUpdate);
        bcvVigencia = findViewById(R.id.bcvVigencia);
        bcvCacheIndicator = findViewById(R.id.bcvCacheIndicator);
        bcvRefreshButton = findViewById(R.id.bcvRefreshButton);

        mainHandler = new Handler(Looper.getMainLooper());
        bcvScraper = new BcvScraper(this);

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

        bcvRefreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchBcvData();
            }
        });

        updateUI();

        // Cargar tasas BCV al iniciar
        fetchBcvData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    private void updateUI() {
        boolean isAdmin = devicePolicyManager.isAdminActive(adminComponent);
        boolean serviceRunning = isServiceRunning(VoskVoiceService.class);
        
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
        boolean serviceRunning = isServiceRunning(VoskVoiceService.class);
        if (serviceRunning) {
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
            Intent serviceIntent = new Intent(this, VoskVoiceService.class);
            startService(serviceIntent);
            Toast.makeText(this, "🎤 Escucha continua activada", Toast.LENGTH_SHORT).show();
            updateUI();
            
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            android.util.Log.e("MainActivity", "Error al iniciar servicio: " + e.getMessage());
        }
    }

    private void stopVoiceService() {
        Intent serviceIntent = new Intent(this, VoskVoiceService.class);
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

    // ========== BCV Scraper ==========

    private void fetchBcvData() {
        bcvRefreshButton.setEnabled(false);
        bcvDolarPrice.setText("...");
        bcvEuroPrice.setText("...");

        bcvScraper.obtenerTasas(new BcvScraper.BcvCallback() {
            @Override
            public void onSuccess(BcvScraper.BcvData data) {
                mainHandler.post(() -> updateBcvUI(data));
            }

            @Override
            public void onError(String error, BcvScraper.BcvData cachedData) {
                mainHandler.post(() -> {
                    if (cachedData != null) {
                        updateBcvUI(cachedData);
                        Toast.makeText(MainActivity.this, error + " (mostrando caché)", Toast.LENGTH_SHORT).show();
                    } else {
                        bcvDolarPrice.setText("---");
                        bcvDolarPrice.setTextColor(0xFFFF5252);
                        bcvEuroPrice.setText("---");
                        bcvEuroPrice.setTextColor(0xFFFF5252);
                        bcvFechaValor.setText("📅 " + error);
                        bcvLastUpdate.setText("");
                        Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
                    }
                    bcvRefreshButton.setEnabled(true);
                });
            }
        });
    }

    private void updateBcvUI(BcvScraper.BcvData data) {
        // Dólar
        if (data.dolar > 0) {
            bcvDolarPrice.setText(data.dolarStr);
            bcvDolarPrice.setTextColor(0xFF4CAF50);
        } else {
            bcvDolarPrice.setText("N/A");
            bcvDolarPrice.setTextColor(0xFFFF5252);
        }

        // Euro
        if (data.euro > 0) {
            bcvEuroPrice.setText(data.euroStr);
            bcvEuroPrice.setTextColor(0xFF2196F3);
        } else {
            bcvEuroPrice.setText("N/A");
            bcvEuroPrice.setTextColor(0xFFFF5252);
        }

        // Fecha valor
        bcvFechaValor.setText("📅 Fecha valor: " + data.fechaValor);

        // Última consulta
        bcvLastUpdate.setText("🕓 Última consulta: " + data.lastUpdate);

        // Indicador de caché
        if (data.isCache) {
            bcvCacheIndicator.setText("📦 Dato en caché");
            bcvCacheIndicator.setVisibility(View.VISIBLE);
        } else {
            bcvCacheIndicator.setVisibility(View.GONE);
        }

        // Vigencia
        if (data.vigenciaMsg != null && !data.vigenciaMsg.isEmpty()) {
            bcvVigencia.setText(data.vigenciaMsg);
            bcvVigencia.setVisibility(View.VISIBLE);
        } else {
            bcvVigencia.setVisibility(View.GONE);
        }

        bcvRefreshButton.setEnabled(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bcvScraper != null) {
            bcvScraper.destroy();
        }
    }
}
