package com.noveasmibeta;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class CalculatorActivity extends Activity {

    private EditText inputTop, inputBottom;
    private Spinner spinnerTop, spinnerBottom;
    private TextView calcTasaDolar, calcTasaEuro;

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

        // Referencias UI
        inputTop = findViewById(R.id.inputTop);
        inputBottom = findViewById(R.id.inputBottom);
        spinnerTop = findViewById(R.id.spinnerTop);
        spinnerBottom = findViewById(R.id.spinnerBottom);
        calcTasaDolar = findViewById(R.id.calcTasaDolar);
        calcTasaEuro = findViewById(R.id.calcTasaEuro);

        // Mostrar tasas actuales
        if (tasaDolar > 0) {
            calcTasaDolar.setText(String.format("%.2f Bs", tasaDolar));
        }
        if (tasaEuro > 0) {
            calcTasaEuro.setText(String.format("%.2f Bs", tasaEuro));
        }

        // Configurar Spinners con estilo
        setupSpinners();

        // Configurar TextWatchers para conversión en tiempo real
        setupTextWatchers();

        // Botón volver
        findViewById(R.id.backButton).setOnClickListener(v -> finish());
    }

    private void setupSpinners() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, CURRENCIES) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView tv = (TextView) view;
                tv.setTextColor(0xFFFFFFFF);
                tv.setTextSize(16);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                tv.setTextColor(0xFFFFFFFF);
                tv.setBackgroundColor(0xFF222244);
                tv.setTextSize(16);
                tv.setPadding(24, 24, 24, 24);
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
}
