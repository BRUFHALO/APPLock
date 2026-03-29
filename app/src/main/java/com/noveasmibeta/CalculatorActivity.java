package com.noveasmibeta;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class CalculatorActivity extends Activity {

    private EditText inputTop, inputBottom;
    private Spinner spinnerTop, spinnerBottom;

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

    // Data Portal Animation
    private DataPortalView dataPortalView;

    // Botones de acción
    private Button swapButton;
    private Button copyTopButton;
    private Button copyBottomButton;
    private Button bcvNotifButton;
    private Button shareWhatsappButton;
    private Button changeQrButton;

    // Share helper
    private PaymentShareHelper shareHelper;
    private static final int PICK_QR_IMAGE = 100;
    private static final int REQUEST_READ_IMAGES = 101;

    private double tasaDolar = 0;
    private double tasaEuro = 0;

    private boolean isUpdating = false;

    private static final String[] CURRENCIES = {"$ USD", "€ EUR", "Bs VES"};
    private static final int USD = 0;
    private static final int EUR = 1;
    private static final int VES = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculator);

        // Obtener tasas desde Intent extras
        tasaDolar = getIntent().getDoubleExtra("tasa_dolar", 0);
        tasaEuro = getIntent().getDoubleExtra("tasa_euro", 0);

        // Referencias UI principales
        inputTop = findViewById(R.id.inputTop);
        inputBottom = findViewById(R.id.inputBottom);
        spinnerTop = findViewById(R.id.spinnerTop);
        spinnerBottom = findViewById(R.id.spinnerBottom);

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

        // Inicializar Data Portal View
        dataPortalView = findViewById(R.id.dataPortalView);
        if (dataPortalView != null) {
            dataPortalView.post(() -> dataPortalView.startAnimation());
        }

        // Inicializar botones de acción
        swapButton = findViewById(R.id.swapButton);
        copyTopButton = findViewById(R.id.copyTopButton);
        copyBottomButton = findViewById(R.id.copyBottomButton);

        // Configurar Spinners con estilo
        setupSpinners();

        // Configurar TextWatchers para conversión en tiempo real
        setupTextWatchers();

        // BCV refresh button
        bcvRefreshButton.setOnClickListener(v -> fetchBcvData());

        // Botón swap: intercambiar valores
        swapButton.setOnClickListener(v -> swapValues());

        // Botones de copiar
        copyTopButton.setOnClickListener(v -> copyToClipboard(inputTop.getText().toString(), "TENGO"));
        copyBottomButton.setOnClickListener(v -> copyToClipboard(inputBottom.getText().toString(), "QUIERO"));

        // Botón notificación BCV
        bcvNotifButton = findViewById(R.id.bcvNotifButton);
        bcvNotifButton.setOnClickListener(v -> toggleBcvNotification());
        updateBcvNotifButtonText();

        // Botones de compartir
        shareHelper = new PaymentShareHelper(this);
        shareWhatsappButton = findViewById(R.id.shareWhatsappButton);
        changeQrButton = findViewById(R.id.changeQrButton);
        shareWhatsappButton.setOnClickListener(v -> handleShareClick());
        changeQrButton.setOnClickListener(v -> pickQrImage());
        updateShareButtonState();

        // Botón volver
        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        // Actualizar UI con tasas iniciales
        updateBcvUIFromIntent();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (dataPortalView != null) {
            dataPortalView.stopAnimation();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (dataPortalView != null) {
            dataPortalView.startAnimation();
        }
        updateBcvNotifButtonText();
    }

    private void setupSpinners() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, CURRENCIES) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView tv = (TextView) view;
                tv.setTextColor(0xFF2C2416);
                tv.setTextSize(15);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                tv.setTextColor(0xFF2C2416);
                tv.setTextSize(15);
                tv.setPadding(20, 16, 20, 16);
                return view;
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerTop.setAdapter(adapter);
        spinnerBottom.setAdapter(adapter);

        // USD arriba, Bs abajo por defecto
        spinnerTop.setSelection(USD);
        spinnerBottom.setSelection(VES);

        AdapterView.OnItemSelectedListener spinnerListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                convertFromTop();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        spinnerTop.setOnItemSelectedListener(spinnerListener);
        spinnerBottom.setOnItemSelectedListener(spinnerListener);
    }

    private void setupTextWatchers() {
        inputTop.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isUpdating) return;
                convertFromTop();
            }
        });

        inputBottom.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isUpdating) return;
                convertFromBottom();
            }
        });
    }

    private void convertFromTop() {
        if (isUpdating) return;
        isUpdating = true;

        try {
            String text = inputTop.getText().toString().trim();
            if (text.isEmpty()) {
                inputBottom.setText("");
                isUpdating = false;
                return;
            }

            double amount = Double.parseDouble(text);
            int fromCurrency = spinnerTop.getSelectedItemPosition();
            int toCurrency = spinnerBottom.getSelectedItemPosition();

            double result = convert(amount, fromCurrency, toCurrency);

            if (result >= 0) {
                inputBottom.setText(formatResult(result));
            }
        } catch (NumberFormatException e) {
            inputBottom.setText("");
        }

        isUpdating = false;
    }

    private void convertFromBottom() {
        if (isUpdating) return;
        isUpdating = true;

        try {
            String text = inputBottom.getText().toString().trim();
            if (text.isEmpty()) {
                inputTop.setText("");
                isUpdating = false;
                return;
            }

            double amount = Double.parseDouble(text);
            int fromCurrency = spinnerBottom.getSelectedItemPosition();
            int toCurrency = spinnerTop.getSelectedItemPosition();

            double result = convert(amount, fromCurrency, toCurrency);

            if (result >= 0) {
                inputTop.setText(formatResult(result));
            }
        } catch (NumberFormatException e) {
            inputTop.setText("");
        }

        isUpdating = false;
    }

    private double convert(double amount, int from, int to) {
        if (from == to) return amount;
        if (tasaDolar <= 0 || tasaEuro <= 0) return -1;

        // Primero convertir a Bolívares
        double amountInVes;
        switch (from) {
            case USD:
                amountInVes = amount * tasaDolar;
                break;
            case EUR:
                amountInVes = amount * tasaEuro;
                break;
            case VES:
            default:
                amountInVes = amount;
                break;
        }

        // Luego convertir de Bolívares a moneda destino
        switch (to) {
            case USD:
                return amountInVes / tasaDolar;
            case EUR:
                return amountInVes / tasaEuro;
            case VES:
            default:
                return amountInVes;
        }
    }

    private String formatResult(double value) {
        if (value == Math.floor(value) && value < 1000000) {
            return String.format("%.0f", value);
        } else if (value >= 1000) {
            return String.format("%.2f", value);
        } else {
            return String.format("%.4f", value);
        }
    }

    // ========== BCV Scraper ==========

    private void updateBcvUIFromIntent() {
        // Actualizar card BCV si hay tasas
        if (tasaDolar > 0) {
            bcvDolarPrice.setText(String.format("%.2f", tasaDolar));
            bcvDolarPrice.setTextColor(0xFF2C2416);
        }
        if (tasaEuro > 0) {
            bcvEuroPrice.setText(String.format("%.2f", tasaEuro));
            bcvEuroPrice.setTextColor(0xFF2C2416);
        }

        // Si no hay tasas, cargar desde BCV
        if (tasaDolar <= 0 || tasaEuro <= 0) {
            fetchBcvData();
        }
    }

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
                        Toast.makeText(CalculatorActivity.this, error + " (mostrando caché)", Toast.LENGTH_SHORT).show();
                    } else {
                        bcvDolarPrice.setText("---");
                        bcvDolarPrice.setTextColor(0xFFFF5252);
                        bcvEuroPrice.setText("---");
                        bcvEuroPrice.setTextColor(0xFFFF5252);
                        bcvFechaValor.setText("📅 " + error);
                        bcvLastUpdate.setText("");
                        Toast.makeText(CalculatorActivity.this, error, Toast.LENGTH_LONG).show();
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
        // Actualizar tasas locales para la conversión
        tasaDolar = data.dolar;
        tasaEuro = data.euro;

        // Actualizar card BCV (solo 2 decimales)
        if (data.dolar > 0) {
            bcvDolarPrice.setText(String.format("%.2f", data.dolar));
            bcvDolarPrice.setTextColor(0xFF2C2416);
        } else {
            bcvDolarPrice.setText("N/A");
            bcvDolarPrice.setTextColor(0xFF8B7E6A);
        }

        if (data.euro > 0) {
            bcvEuroPrice.setText(String.format("%.2f", data.euro));
            bcvEuroPrice.setTextColor(0xFF2C2416);
        } else {
            bcvEuroPrice.setText("N/A");
            bcvEuroPrice.setTextColor(0xFF8B7E6A);
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

    // ========== BCV Notification ==========

    private void toggleBcvNotification() {
        boolean running = isServiceRunning(BcvNotificationService.class);
        if (running) {
            stopService(new Intent(this, BcvNotificationService.class));
            Toast.makeText(this, "Notificación BCV desactivada", Toast.LENGTH_SHORT).show();
        } else {
            Intent intent = new Intent(this, BcvNotificationService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
            Toast.makeText(this, "\uD83D\uDCCA Tasa BCV visible en notificaciones", Toast.LENGTH_SHORT).show();
        }
        updateBcvNotifButtonText();
    }

    private void updateBcvNotifButtonText() {
        if (bcvNotifButton == null) return;
        boolean running = isServiceRunning(BcvNotificationService.class);
        if (running) {
            bcvNotifButton.setText("\u23F9 DETENER NOTIFICACIÓN BCV");
        } else {
            bcvNotifButton.setText("\uD83D\uDCCA TASA BCV EN NOTIFICACIÓN");
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

    // ========== WhatsApp Share ==========

    private void handleShareClick() {
        if (!shareHelper.hasQrSaved()) {
            Toast.makeText(this, "Primero selecciona la imagen QR de tu banco", Toast.LENGTH_LONG).show();
            pickQrImage();
            return;
        }

        String montoTop = inputTop.getText().toString().trim();
        String montoBottom = inputBottom.getText().toString().trim();

        if (montoTop.isEmpty() && montoBottom.isEmpty()) {
            Toast.makeText(this, "Ingresa un monto para compartir", Toast.LENGTH_SHORT).show();
            return;
        }

        String currencyTop = CURRENCIES[spinnerTop.getSelectedItemPosition()];
        String currencyBottom = CURRENCIES[spinnerBottom.getSelectedItemPosition()];

        shareHelper.generateAndShare(montoTop, currencyTop, montoBottom, currencyBottom, tasaDolar);
    }

    private void pickQrImage() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.READ_MEDIA_IMAGES}, REQUEST_READ_IMAGES);
                return;
            }
        } else if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_IMAGES);
                return;
            }
        }
        launchImagePicker();
    }

    private void launchImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_QR_IMAGE);
    }

    private void updateShareButtonState() {
        if (shareHelper.hasQrSaved()) {
            shareWhatsappButton.setText("📤 COMPARTIR POR WHATSAPP");
            changeQrButton.setVisibility(View.VISIBLE);
        } else {
            shareWhatsappButton.setText("📤 SELECCIONAR QR Y COMPARTIR");
            changeQrButton.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_QR_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                boolean saved = shareHelper.saveQrImage(imageUri);
                if (saved) {
                    Toast.makeText(this, "\u2705 QR guardado correctamente", Toast.LENGTH_SHORT).show();
                    updateShareButtonState();
                } else {
                    Toast.makeText(this, "Error al guardar el QR", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_READ_IMAGES) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchImagePicker();
            } else {
                Toast.makeText(this, "Se necesita permiso para acceder a las im\u00e1genes", Toast.LENGTH_LONG).show();
            }
        }
    }

    // ========== Swap & Copy ==========

    private void swapValues() {
        // Guardar valores actuales
        String topValue = inputTop.getText().toString();
        String bottomValue = inputBottom.getText().toString();
        int topCurrency = spinnerTop.getSelectedItemPosition();
        int bottomCurrency = spinnerBottom.getSelectedItemPosition();

        // Intercambiar valores
        isUpdating = true;
        inputTop.setText(bottomValue);
        inputBottom.setText(topValue);
        spinnerTop.setSelection(bottomCurrency);
        spinnerBottom.setSelection(topCurrency);
        isUpdating = false;

        // Forzar recálculo
        convertFromTop();
    }

    private void copyToClipboard(String value, String label) {
        if (value == null || value.isEmpty() || value.equals("0.00")) {
            Toast.makeText(this, "No hay valor para copiar", Toast.LENGTH_SHORT).show();
            return;
        }

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, value);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "✓ Copiado: " + value, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bcvScraper != null) {
            bcvScraper.destroy();
        }
    }
}
