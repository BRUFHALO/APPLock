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

    // Binance UI
    private TextView binancePrice;
    private TextView binanceLastUpdate;
    private TextView binanceCacheIndicator;
    private Button binanceRefreshButton;
    private ProgressBar binanceLoader;
    private BinanceScraper binanceScraper;

    // Data Portal Animation
    private DataPortalView dataPortalView;

    // Botones de acción
    private Button swapButton;
    private Button copyTopButton;
    private Button copyBottomButton;
    private Button shareWhatsappButton;
    private Button changeQrButton;

    // Share helper
    private PaymentShareHelper shareHelper;
    private static final int PICK_QR_IMAGE = 100;
    private static final int REQUEST_READ_IMAGES = 101;

    private double tasaDolar = 0;
    private double tasaEuro = 0;
    private double tasaUSDT = 0; // Tasa Binance P2P

    private boolean isUpdating = false;

    private static final String[] CURRENCIES = {"$ USD", "€ EUR", "Bs VES", "₮ USDT"};
    private static final int USD = 0;
    private static final int EUR = 1;
    private static final int VES = 2;
    private static final int USDT = 3;

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

        // Binance UI References
        binancePrice = findViewById(R.id.binancePrice);
        binanceLastUpdate = findViewById(R.id.binanceLastUpdate);
        binanceCacheIndicator = findViewById(R.id.binanceCacheIndicator);
        binanceRefreshButton = findViewById(R.id.binanceRefreshButton);
        binanceLoader = findViewById(R.id.binanceLoader);
        binanceScraper = new BinanceScraper(this);

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

        // Binance refresh button
        binanceRefreshButton.setOnClickListener(v -> fetchBinanceData());

        // Botón swap: intercambiar valores
        swapButton.setOnClickListener(v -> swapValues());

        // Botones de copiar
        copyTopButton.setOnClickListener(v -> copyToClipboard(inputTop.getText().toString(), "TENGO"));
        copyBottomButton.setOnClickListener(v -> copyToClipboard(inputBottom.getText().toString(), "QUIERO"));

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
    }

    private void setupSpinners() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, CURRENCIES) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView tv = (TextView) view;
                tv.setTextColor(0xFFE6C163);
                tv.setTextSize(14);
                tv.setGravity(android.view.Gravity.CENTER);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                tv.setTextColor(0xFFE6C163);
                tv.setTextSize(14);
                tv.setPadding(24, 16, 24, 16);
                tv.setBackgroundColor(0xFF2D2F3C);
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

            double amount = parseFormattedNumber(text);
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

            double amount = parseFormattedNumber(text);
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
            case USDT:
                // USDT usa la tasa de Binance
                if (tasaUSDT <= 0) return -1;
                amountInVes = amount * tasaUSDT;
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
            case USDT:
                if (tasaUSDT <= 0) return -1;
                return amountInVes / tasaUSDT;
            case VES:
            default:
                return amountInVes;
        }
    }

    private String formatResult(double value) {
        // Formato: separador de miles con punto, decimales con coma
        // Ejemplo: 10.000,50
        if (value == Math.floor(value)) {
            // Sin decimales
            return formatWithThousandsSeparator((long) value);
        } else {
            // Con decimales
            long integerPart = (long) value;
            int decimalPart = (int) Math.round((value - integerPart) * 100);
            if (decimalPart >= 100) {
                decimalPart -= 100;
                integerPart++;
            }
            return formatWithThousandsSeparator(integerPart) + "," + String.format("%02d", decimalPart);
        }
    }

    private String formatWithThousandsSeparator(long value) {
        StringBuilder result = new StringBuilder();
        String str = String.valueOf(Math.abs(value));
        int count = 0;

        for (int i = str.length() - 1; i >= 0; i--) {
            if (count > 0 && count % 3 == 0) {
                result.insert(0, ".");
            }
            result.insert(0, str.charAt(i));
            count++;
        }

        if (value < 0) {
            result.insert(0, "-");
        }

        return result.toString();
    }

    // ========== BCV Scraper ==========

    private void updateBcvUIFromIntent() {
        // Actualizar card BCV si hay tasas
        if (tasaDolar > 0) {
            bcvDolarPrice.setText(String.format("%.2f", tasaDolar));
            bcvDolarPrice.setTextColor(0xFFEDEDED);
        }
        if (tasaEuro > 0) {
            bcvEuroPrice.setText(String.format("%.2f", tasaEuro));
            bcvEuroPrice.setTextColor(0xFFEDEDED);
        }

        // Si no hay tasas, cargar desde BCV
        if (tasaDolar <= 0 || tasaEuro <= 0) {
            fetchBcvData();
        }

        // Siempre cargar tasa Binance (con caché de 8 min)
        fetchBinanceData();
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
            bcvDolarPrice.setTextColor(0xFFEDEDED);
        } else {
            bcvDolarPrice.setText("N/A");
            bcvDolarPrice.setTextColor(0xFFA89060);
        }

        if (data.euro > 0) {
            bcvEuroPrice.setText(String.format("%.2f", data.euro));
            bcvEuroPrice.setTextColor(0xFFEDEDED);
        } else {
            bcvEuroPrice.setText("N/A");
            bcvEuroPrice.setTextColor(0xFFA89060);
        }

        // Fecha valor
        bcvFechaValor.setText("📅 Fecha valor: " + data.fechaValor);

        // Última consulta
        bcvLastUpdate.setText("🕓 Última consulta: " + data.lastUpdate);

        // Indicador de caché
        if (data.isCache) {
            bcvCacheIndicator.setText("📦 Dato en caché");
            bcvCacheIndicator.setTextColor(0xFFD4AF37);
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

    // ========== Binance Scraper ==========

    private void fetchBinanceData() {
        // Mostrar loader y deshabilitar botón
        binanceRefreshButton.setEnabled(false);
        binanceRefreshButton.setVisibility(View.INVISIBLE);
        binanceLoader.setVisibility(View.VISIBLE);
        binancePrice.setText("...");

        binanceScraper.fetchUsdtVesPrice(new BinanceScraper.BinanceCallback() {
            @Override
            public void onSuccess(BinanceScraper.BinanceData data) {
                mainHandler.post(() -> {
                    updateBinanceUI(data);
                    hideBinanceLoader();
                });
            }

            @Override
            public void onError(String error, BinanceScraper.BinanceData cachedData) {
                mainHandler.post(() -> {
                    if (cachedData != null) {
                        updateBinanceUI(cachedData);
                        Toast.makeText(CalculatorActivity.this, error + " (mostrando caché)", Toast.LENGTH_SHORT).show();
                    } else {
                        binancePrice.setText("---");
                        binancePrice.setTextColor(0xFFFF5252);
                        binanceLastUpdate.setText("🕓 " + error);
                        Toast.makeText(CalculatorActivity.this, error, Toast.LENGTH_LONG).show();
                    }
                    hideBinanceLoader();
                });
            }
        });
    }

    private void hideBinanceLoader() {
        binanceLoader.setVisibility(View.GONE);
        binanceRefreshButton.setVisibility(View.VISIBLE);
        binanceRefreshButton.setEnabled(true);
    }

    private void updateBinanceUI(BinanceScraper.BinanceData data) {
        // Actualizar tasa local para la conversión
        tasaUSDT = data.price;

        // Actualizar card Binance
        if (data.price > 0) {
            binancePrice.setText(data.priceStr + " Bs/USDT");
            binancePrice.setTextColor(0xFFEDEDED);
        } else {
            binancePrice.setText("N/A");
            binancePrice.setTextColor(0xFFA89060);
        }

        // Última consulta
        binanceLastUpdate.setText("🕓 Última consulta: " + data.lastUpdate);

        // Indicador de caché
        if (data.isCache) {
            binanceCacheIndicator.setText("📦 Dato en caché");
            binanceCacheIndicator.setTextColor(0xFFD4AF37);
            binanceCacheIndicator.setVisibility(View.VISIBLE);
        } else {
            binanceCacheIndicator.setVisibility(View.GONE);
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

        // Determinar qué tasa usar: si hay USDT seleccionado, usar Binance
        int topPos = spinnerTop.getSelectedItemPosition();
        int bottomPos = spinnerBottom.getSelectedItemPosition();
        boolean hasUsdt = (topPos == USDT || bottomPos == USDT);

        double tasaToUse = hasUsdt ? tasaUSDT : tasaDolar;
        String tasaLabel = hasUsdt ? "Tasa Binance" : "Tasa BCV";

        shareHelper.generateAndShare(montoTop, currencyTop, montoBottom, currencyBottom, tasaToUse, tasaLabel);
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
        // Guardar valores y monedas actuales
        String topValue = inputTop.getText().toString().trim();
        String bottomValue = inputBottom.getText().toString().trim();
        int topCurrency = spinnerTop.getSelectedItemPosition();
        int bottomCurrency = spinnerBottom.getSelectedItemPosition();

        // Intercambiar todo: montos y monedas
        isUpdating = true;
        inputTop.setText(bottomValue);
        inputBottom.setText(topValue);
        spinnerTop.setSelection(bottomCurrency);
        spinnerBottom.setSelection(topCurrency);
        isUpdating = false;
    }

    private double parseFormattedNumber(String formatted) {
        // Quitar puntos de miles y reemplazar coma decimal por punto
        String normalized = formatted.replace(".", "").replace(",", ".");
        return Double.parseDouble(normalized);
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
        if (binanceScraper != null) {
            binanceScraper.destroy();
        }
    }
}
