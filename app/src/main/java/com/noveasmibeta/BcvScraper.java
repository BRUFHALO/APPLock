package com.noveasmibeta;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BcvScraper {

    private static final String TAG = "BcvScraper";
    private static final String BCV_URL = "https://www.bcv.org.ve/";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final int TIMEOUT_MS = 15000;

    private static final String PREFS_NAME = "BcvCache";
    private static final String KEY_DOLAR = "dolar_price";
    private static final String KEY_EURO = "euro_price";
    private static final String KEY_FECHA_VALOR = "fecha_valor";
    private static final String KEY_LAST_UPDATE = "last_update";
    private static final String KEY_IS_CACHE = "is_cache";

    private final Context context;
    private final ExecutorService executor;

    public interface BcvCallback {
        void onSuccess(BcvData data);
        void onError(String error, BcvData cachedData);
    }

    public static class BcvData {
        public String dolarStr;
        public String euroStr;
        public double dolar;
        public double euro;
        public String fechaValor;
        public String lastUpdate;
        public boolean isCache;
        public String vigenciaMsg;

        @Override
        public String toString() {
            return "Dólar: " + dolarStr + " Bs. | Euro: " + euroStr + " Bs. | Fecha: " + fechaValor;
        }
    }

    public BcvScraper(Context context) {
        this.context = context;
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void obtenerTasas(BcvCallback callback) {
        executor.execute(() -> {
            if (!isNetworkAvailable()) {
                Log.w(TAG, "Sin conexión a internet - Cargando caché");
                BcvData cached = loadFromCache();
                if (cached != null) {
                    cached.isCache = true;
                    callback.onError("Sin conexión a internet", cached);
                } else {
                    callback.onError("Sin conexión a internet y sin datos en caché", null);
                }
                return;
            }

            try {
                Log.d(TAG, "Conectando a BCV...");

                Document doc = Jsoup.connect(BCV_URL)
                        .userAgent(USER_AGENT)
                        .timeout(TIMEOUT_MS)
                        .ignoreHttpErrors(true)
                        .sslSocketFactory(getSSLSocketFactory())
                        .get();

                Log.d(TAG, "Página descargada exitosamente");

                // Extraer dólar
                Element dolarElement = doc.select("#dolar strong").first();
                // Extraer euro
                Element euroElement = doc.select("#euro strong").first();
                // Extraer fecha de vigencia
                Element fechaElement = doc.select(".pull-right .dinpro").first();
                if (fechaElement == null) {
                    fechaElement = doc.select(".fecha-valor span").first();
                }
                if (fechaElement == null) {
                    fechaElement = doc.select(".recuadrotsmc .centrado span").first();
                }

                BcvData data = new BcvData();

                // Sanitizar dólar
                if (dolarElement != null) {
                    data.dolarStr = sanitizePrice(dolarElement.text());
                    data.dolar = parsePrice(data.dolarStr);
                    Log.d(TAG, "Dólar BCV: " + data.dolarStr + " -> " + data.dolar);
                } else {
                    Log.w(TAG, "No se encontró el elemento #dolar strong");
                    data.dolarStr = "N/A";
                    data.dolar = 0;
                }

                // Sanitizar euro
                if (euroElement != null) {
                    data.euroStr = sanitizePrice(euroElement.text());
                    data.euro = parsePrice(data.euroStr);
                    Log.d(TAG, "Euro BCV: " + data.euroStr + " -> " + data.euro);
                } else {
                    Log.w(TAG, "No se encontró el elemento #euro strong");
                    data.euroStr = "N/A";
                    data.euro = 0;
                }

                // Sanitizar fecha
                if (fechaElement != null) {
                    data.fechaValor = fechaElement.text().trim();
                    Log.d(TAG, "Fecha valor: " + data.fechaValor);
                } else {
                    Log.w(TAG, "No se encontró fecha de vigencia");
                    data.fechaValor = "No disponible";
                }

                // Timestamp de la consulta
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                data.lastUpdate = sdf.format(new Date());
                data.isCache = false;

                // Calcular vigencia
                data.vigenciaMsg = calcularVigencia(data.fechaValor);

                // Guardar en caché
                saveToCache(data);

                Log.d(TAG, "Datos obtenidos exitosamente: " + data.toString());
                callback.onSuccess(data);

            } catch (IOException e) {
                Log.e(TAG, "Error de conexión: " + e.getMessage());
                BcvData cached = loadFromCache();
                if (cached != null) {
                    cached.isCache = true;
                    callback.onError("Error de conexión: " + e.getMessage(), cached);
                } else {
                    callback.onError("Error de conexión y sin caché disponible", null);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error inesperado: " + e.getMessage());
                e.printStackTrace();
                BcvData cached = loadFromCache();
                if (cached != null) {
                    cached.isCache = true;
                    callback.onError("Error: " + e.getMessage(), cached);
                } else {
                    callback.onError("Error inesperado: " + e.getMessage(), null);
                }
            }
        });
    }

    private String sanitizePrice(String raw) {
        if (raw == null) return "0";
        // Limpiar espacios, caracteres invisibles y no-breaking spaces
        String cleaned = raw.trim()
                .replaceAll("[\\s\\u00A0\\u200B]+", "")
                .replaceAll("[^0-9,.]", "");
        return cleaned;
    }

    private double parsePrice(String priceStr) {
        try {
            if (priceStr == null || priceStr.isEmpty() || priceStr.equals("N/A")) return 0;
            // BCV usa coma como separador decimal: "36,71" -> "36.71"
            String normalized = priceStr.replace(",", ".");
            return Double.parseDouble(normalized);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parseando precio: " + priceStr + " - " + e.getMessage());
            return 0;
        }
    }

    private String calcularVigencia(String fechaValor) {
        Calendar now = Calendar.getInstance();
        int hora = now.get(Calendar.HOUR_OF_DAY);
        int diaSemana = now.get(Calendar.DAY_OF_WEEK);

        // Fin de semana
        if (diaSemana == Calendar.SATURDAY || diaSemana == Calendar.SUNDAY) {
            return "📅 Tasa vigente del viernes (fin de semana)";
        }

        // Día hábil antes de las 4PM
        if (hora < 16) {
            return "📅 Tasa vigente desde ayer";
        }

        // Día hábil después de las 4PM
        if (hora >= 16 && hora <= 18) {
            return "🔄 Nueva tasa disponible para mañana";
        }

        // Después de las 6PM
        if (hora > 18) {
            return "✅ Tasa actualizada - vigente mañana";
        }

        return "";
    }

    private void saveToCache(BcvData data) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_DOLAR, data.dolarStr);
        editor.putString(KEY_EURO, data.euroStr);
        editor.putString(KEY_FECHA_VALOR, data.fechaValor);
        editor.putString(KEY_LAST_UPDATE, data.lastUpdate);
        editor.apply();
        Log.d(TAG, "Datos guardados en caché");
    }

    private BcvData loadFromCache() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String dolar = prefs.getString(KEY_DOLAR, null);
        if (dolar == null) return null;

        BcvData data = new BcvData();
        data.dolarStr = dolar;
        data.dolar = parsePrice(dolar);
        data.euroStr = prefs.getString(KEY_EURO, "N/A");
        data.euro = parsePrice(data.euroStr);
        data.fechaValor = prefs.getString(KEY_FECHA_VALOR, "No disponible");
        data.lastUpdate = prefs.getString(KEY_LAST_UPDATE, "Desconocido");
        data.isCache = true;
        data.vigenciaMsg = calcularVigencia(data.fechaValor);

        Log.d(TAG, "Datos cargados desde caché: " + data.toString());
        return data;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
        return false;
    }

    private javax.net.ssl.SSLSocketFactory getSSLSocketFactory() {
        try {
            javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[]{
                new javax.net.ssl.X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[0];
                    }
                    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                }
            };
            javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            return sc.getSocketFactory();
        } catch (Exception e) {
            Log.e(TAG, "Error configurando SSL: " + e.getMessage());
            return null;
        }
    }

    public void destroy() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
