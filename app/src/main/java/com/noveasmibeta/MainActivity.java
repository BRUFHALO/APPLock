package com.noveasmibeta;

import android.Manifest;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_ENABLE_ADMIN = 1;
    private static final int REQUEST_CODE_RECORD_AUDIO = 2;
    private static final int REQUEST_CODE_POST_NOTIFICATIONS = 3;

    private DevicePolicyManager devicePolicyManager;
    private ComponentName adminComponent;
    private TextView statusText;
    private Button lockButton;
    private Button enableAdminButton;
    private Button voiceButton;
    private Button calculatorButton;

    // BCV UI
    private TextView bcvDolarPrice;
    private TextView bcvEuroPrice;
    private TextView bcvFechaValor;
    private TextView bcvLastUpdate;
    private TextView bcvVigencia;
    private TextView bcvCacheIndicator;
    private Button bcvRefreshButton;
    private ProgressBar bcvLoader;
    private BcvScraper bcvScraper;
    private Handler mainHandler;

    // Tasas para pasar a la calculadora
    private double lastTasaDolar = 0;
    private double lastTasaEuro = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponent = new ComponentName(this, AdminReceiver.class);

        // Referencias UI principales
        statusText = findViewById(R.id.statusText);
        lockButton = findViewById(R.id.lockButton);
        enableAdminButton = findViewById(R.id.enableAdminButton);
        voiceButton = findViewById(R.id.voiceButton);
        calculatorButton = findViewById(R.id.calculatorButton);

        // BCV UI References
        bcvDolarPrice = findViewById(R.id.bcvDolarPrice);
        bcvEuroPrice = findViewById(R.id.bcvEuroPrice);
        bcvFechaValor = findViewById(R.id.bcvFechaValor);
        bcvLastUpdate = findViewById(R.id.bcvLastUpdate);
        bcvVigencia = findViewById(R.id.bcvVigencia);
        bcvCacheIndicator = findViewById(R.id.bcvCacheIndicator);
        bcvRefreshButton = findViewById(R.id.bcvRefreshButton);
        bcvLoader = findViewById(R.id.bcvLoader);

        mainHandler = new Handler(Looper.getMainLooper());
        bcvScraper = new BcvScraper(this);

        // Botones principales
        lockButton.setOnClickListener(v -> lockScreen());
        enableAdminButton.setOnClickListener(v -> enableDeviceAdmin());
        voiceButton.setOnClickListener(v -> toggleVoiceService());
        bcvRefreshButton.setOnClickListener(v -> fetchBcvData());
        calculatorButton.setOnClickListener(v -> openCalculator());

        updateUI();
        fetchBcvData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    // ========== UI State ==========

    private void updateUI() {
        boolean isAdmin = devicePolicyManager.isAdminActive(adminComponent);
        boolean voskRunning = isServiceRunning(VoskVoiceService.class);
        boolean googleRunning = isServiceRunning(GoogleVoiceService.class);
        boolean anyServiceRunning = voskRunning || googleRunning;

        if (isAdmin) {
            if (anyServiceRunning) {
                String mode = voskRunning ? "🔇 Offline" : "🌐 Online";
                statusText.setText("✓ Escucha activa (" + mode + ")\n\nDi 'bloquear' para bloquear");
                statusText.setTextColor(0xFF4CAF50);
                voiceButton.setText("⏹ DETENER ESCUCHA");
            } else {
                statusText.setText("✓ Administrador activado\n\nToca para activar escucha continua");
                statusText.setTextColor(0xFF2196F3);
                voiceButton.setText("🎤 INICIAR ESCUCHA");
            }
            lockButton.setEnabled(true);
            voiceButton.setEnabled(true);
            enableAdminButton.setVisibility(View.GONE);
        } else {
            statusText.setText("⚠ Administrador no activado\n\nActiva los permisos primero");
            statusText.setTextColor(0xFFFF9800);
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

    // ========== Hybrid Voice ==========

    private boolean checkConnectivity() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
        return false;
    }

    private void toggleVoiceService() {
        boolean voskRunning = isServiceRunning(VoskVoiceService.class);
        boolean googleRunning = isServiceRunning(GoogleVoiceService.class);

        if (voskRunning || googleRunning) {
            stopAllVoiceServices();
        } else {
            if (checkMicrophonePermission()) {
                startHybridVoiceService();
            } else {
                requestMicrophonePermission();
            }
        }
    }

    private void startHybridVoiceService() {
        try {
            boolean hasInternet = checkConnectivity();
            Intent serviceIntent;

            if (hasInternet) {
                Log.d(TAG, "Internet disponible -> Iniciando GoogleVoiceService (Online)");
                serviceIntent = new Intent(this, GoogleVoiceService.class);
                Toast.makeText(this, "🌐 Escucha ONLINE activada", Toast.LENGTH_SHORT).show();
            } else {
                Log.d(TAG, "Sin internet -> Iniciando VoskVoiceService (Offline)");
                serviceIntent = new Intent(this, VoskVoiceService.class);
                Toast.makeText(this, "🔇 Escucha OFFLINE activada", Toast.LENGTH_SHORT).show();
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }

            updateUI();

        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e(TAG, "Error al iniciar servicio: " + e.getMessage());
        }
    }

    private void stopAllVoiceServices() {
        try {
            stopService(new Intent(this, VoskVoiceService.class));
        } catch (Exception e) {
            Log.e(TAG, "Error deteniendo Vosk: " + e.getMessage());
        }
        try {
            stopService(new Intent(this, GoogleVoiceService.class));
        } catch (Exception e) {
            Log.e(TAG, "Error deteniendo Google: " + e.getMessage());
        }
        Toast.makeText(this, "Escucha detenida", Toast.LENGTH_SHORT).show();
        updateUI();
    }

    // ========== Calculator ==========

    private void openCalculator() {
        Intent intent = new Intent(this, CalculatorActivity.class);
        intent.putExtra("tasa_dolar", lastTasaDolar);
        intent.putExtra("tasa_euro", lastTasaEuro);
        startActivity(intent);
    }

    // ========== Device Admin ==========

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

    // ========== Permissions ==========

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_ENABLE_ADMIN) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "¡Administrador activado!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Necesitas activar el administrador", Toast.LENGTH_LONG).show();
            }
            updateUI();
        } else if (requestCode == REQUEST_CODE_RECORD_AUDIO) {
            if (resultCode == RESULT_OK) {
                startHybridVoiceService();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startHybridVoiceService();
            } else {
                Toast.makeText(this, "Se requiere permiso de micrófono", Toast.LENGTH_LONG).show();
            }
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
        // Mostrar loader y deshabilitar botón
        bcvRefreshButton.setEnabled(false);
        bcvRefreshButton.setVisibility(View.INVISIBLE);
        bcvLoader.setVisibility(View.VISIBLE);
        bcvDolarPrice.setText("...");
        bcvEuroPrice.setText("...");

        bcvScraper.obtenerTasas(new BcvScraper.BcvCallback() {
            @Override
            public void onSuccess(BcvScraper.BcvData data) {
                mainHandler.post(() -> {
                    updateBcvUI(data);
                    hideLoader();
                });
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
                    hideLoader();
                });
            }
        });
    }

    private void hideLoader() {
        bcvLoader.setVisibility(View.GONE);
        bcvRefreshButton.setVisibility(View.VISIBLE);
        bcvRefreshButton.setEnabled(true);
    }

    private void updateBcvUI(BcvScraper.BcvData data) {
        // Guardar tasas para la calculadora
        lastTasaDolar = data.dolar;
        lastTasaEuro = data.euro;

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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bcvScraper != null) {
            bcvScraper.destroy();
        }
    }
}
