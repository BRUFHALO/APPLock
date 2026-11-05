package com.noveasmibeta;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
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

    // Fallback si el scraping a bcv.org.ve falla.
    private static final String DOLARAPI_USD = "https://ve.dolarapi.com/v1/dolares/oficial";
    private static final String DOLARAPI_EUR = "https://ve.dolarapi.com/v1/euros/oficial";

    private static final String PREFS_NAME = "BcvCache";
    private static final String KEY_DOLAR = "dolar_price";
    private static final String KEY_EURO = "euro_price";
    private static final String KEY_FECHA_VALOR = "fecha_valor";
    private static final String KEY_LAST_UPDATE = "last_update";
    private static final String KEY_FETCH_DATE = "fetch_date";

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
        obtenerTasas(false, callback);
    }

    /**
     * Obtiene las tasas: si ya se consultó hoy y {@code force} es false,
     * devuelve los datos del día sin llamada de red (1 consulta diaria).
     * El scrape usa DolarApi como respaldo y el caché como última opción.
     */
    public void obtenerTasas(boolean force, BcvCallback callback) {
        executor.execute(() -> {
            if (!force && fetchedToday()) {
                BcvData today = loadFromCache(false);
                if (today != null && plausible(today.dolar, today.euro)) {
                    Log.d(TAG, "Tasas ya consultadas hoy - usando datos del día");
                    callback.onSuccess(today);
                    return;
                }
            }

            if (!isNetworkAvailable()) {
                Log.w(TAG, "Sin conexión a internet - Cargando caché");
                BcvData cached = loadFromCache(true);
                if (cached != null && plausible(cached.dolar, cached.euro)) {
                    callback.onError("Sin conexión a internet", cached);
                } else {
                    callback.onError("Sin conexión a internet y sin datos en caché", null);
                }
                return;
            }

            // 1) Scraping a bcv.org.ve
            try {
                Log.d(TAG, "Conectando a BCV...");
                BcvData data = fetchFromWeb();
                if (!plausible(data.dolar, data.euro)) {
                    throw new IOException("Datos BCV implausibles: " + data.dolar + "/" + data.euro);
                }
                saveToCache(data);
                Log.d(TAG, "Datos obtenidos exitosamente: " + data);
                callback.onSuccess(data);
                return;
            } catch (Exception e) {
                Log.e(TAG, "Scraping falló: " + e.getMessage() + " - intentando DolarApi");
            }

            // 2) Fallback: DolarApi
            try {
                BcvData data = fetchFromDolarApi();
                if (!plausible(data.dolar, data.euro)) {
                    throw new IOException("Datos DolarApi implausibles");
                }
                saveToCache(data);
                Log.d(TAG, "Datos DolarApi: " + data);
                callback.onSuccess(data);
                return;
            } catch (Exception e) {
                Log.e(TAG, "DolarApi falló: " + e.getMessage());
            }

            // 3) Última opción: caché
            BcvData cached = loadFromCache(true);
            if (cached != null && plausible(cached.dolar, cached.euro)) {
                callback.onError("Error de conexión", cached);
            } else {
                callback.onError("Error de conexión y sin caché disponible", null);
            }
        });
    }

    /** Scraping de www.bcv.org.ve con Jsoup (lanza excepción si falla). */
    private BcvData fetchFromWeb() throws IOException {
        Document doc = Jsoup.connect(BCV_URL)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MS)
                .ignoreHttpErrors(true)
                .sslSocketFactory(getSSLSocketFactory())
                .get();

        Element dolarElement = doc.select("#dolar strong").first();
        Element euroElement = doc.select("#euro strong").first();
        Element fechaElement = doc.select("span.date-display-single").first();
        if (fechaElement == null) {
            fechaElement = doc.select(".pull-right .dinpro").first();
        }
        if (fechaElement == null) {
            fechaElement = doc.select(".fecha-valor span").first();
        }

        String dolarStr = dolarElement != null ? sanitizePrice(dolarElement.text()) : "N/A";
        String euroStr = euroElement != null ? sanitizePrice(euroElement.text()) : "N/A";
        String fechaValor = fechaElement != null
                ? fechaElement.text().replaceAll("\\s+", " ").trim()
                : "No disponible";

        return buildData(dolarStr, euroStr, fechaValor);
    }

    /** Fallback con la API de dolarapi.com (promedio oficial USD y EUR). */
    private BcvData fetchFromDolarApi() throws Exception {
        double dolar = fetchDolarApiPrice(DOLARAPI_USD);
        double euro = fetchDolarApiPrice(DOLARAPI_EUR);
        if (dolar <= 0 || euro <= 0) {
            throw new IOException("DolarApi devolvió datos incompletos");
        }

        String fechaValor = fetchDolarApiDate(DOLARAPI_USD);
        if (fechaValor == null) fechaValor = "No disponible";

        return buildData(String.valueOf(dolar), String.valueOf(euro), fechaValor);
    }

    private double fetchDolarApiPrice(String endpoint) throws Exception {
        JSONObject json = new JSONObject(httpGet(endpoint));
        double promedio = json.optDouble("promedio", 0);
        // Quirk de la API: emite miles con punto como número JSON
        // (euro "1.006" = 1006 real).
        if (promedio > 0 && promedio < 10) promedio *= 1000;
        return promedio;
    }

    private String fetchDolarApiDate(String endpoint) {
        try {
            JSONObject json = new JSONObject(httpGet(endpoint));
            String raw = json.optString("fechaActualizacion", null);
            if (raw == null || raw.isEmpty()) return null;
            SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US);
            Date dt = iso.parse(raw);
            return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dt);
        } catch (Exception e) {
            return null;
        }
    }

    private String httpGet(String endpoint) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);

        int code = conn.getResponseCode();
        if (code != HttpURLConnection.HTTP_OK) {
            throw new IOException("HTTP error: " + code);
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            response.append(line.trim());
        }
        in.close();
        conn.disconnect();
        return response.toString();
    }

    private BcvData buildData(String dolarStr, String euroStr, String fechaValor) {
        BcvData data = new BcvData();
        data.dolarStr = dolarStr;
        data.dolar = parsePrice(dolarStr);
        data.euroStr = euroStr;
        data.euro = parsePrice(euroStr);
        data.fechaValor = fechaValor;
        data.lastUpdate = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
        data.isCache = false;
        data.vigenciaMsg = calcularVigencia(fechaValor);
        return data;
    }

    private String sanitizePrice(String raw) {
        if (raw == null) return "0";
        return raw.trim()
                .replaceAll("[\\s ​]+", "")
                .replaceAll("[^0-9,.]", "");
    }

    /**
     * Parsea precio en formato venezolano (coma decimal, punto miles).
     * Igual que CalculatorActivity.parseFormattedNumber — el BCV usa ambos
     * cuando el valor pasa de 1000 ("1.006,03" = 1006.03).
     */
    private double parsePrice(String priceStr) {
        try {
            if (priceStr == null || priceStr.isEmpty() || priceStr.equals("N/A")) return 0;
            if (priceStr.contains(",")) {
                String normalized = priceStr.replace(".", "").replace(",", ".");
                return Double.parseDouble(normalized);
            } else if (priceStr.contains(".")) {
                if (priceStr.matches("\\d{1,3}(\\.\\d{3})+")) {
                    return Double.parseDouble(priceStr.replace(".", ""));
                }
                return Double.parseDouble(priceStr);
            }
            return Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parseando precio: " + priceStr);
            return 0;
        }
    }

    /**
     * Verificación de cordura: el euro nunca debería estar muy lejos del
     * dólar (EUR/USD ~1.0-1.2). Descarta caché corrupto o parseo roto.
     */
    private boolean plausible(double dolar, double euro) {
        return dolar > 0 && euro >= dolar * 0.8 && euro <= dolar * 2.5;
    }

    private String calcularVigencia(String fechaValor) {
        Calendar now = Calendar.getInstance();
        int hora = now.get(Calendar.HOUR_OF_DAY);
        int diaSemana = now.get(Calendar.DAY_OF_WEEK);

        if (diaSemana == Calendar.SATURDAY || diaSemana == Calendar.SUNDAY) {
            return "📅 Tasa vigente del viernes (fin de semana)";
        }
        if (hora < 16) {
            return "📅 Tasa vigente desde ayer";
        }
        if (hora >= 16 && hora <= 18) {
            return "🔄 Nueva tasa disponible para mañana";
        }
        if (hora > 18) {
            return "✅ Tasa actualizada - vigente mañana";
        }
        return "";
    }

    private void saveToCache(BcvData data) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_DOLAR, data.dolarStr)
                .putString(KEY_EURO, data.euroStr)
                .putString(KEY_FECHA_VALOR, data.fechaValor)
                .putString(KEY_LAST_UPDATE, data.lastUpdate)
                .putString(KEY_FETCH_DATE, today())
                .apply();
        Log.d(TAG, "Datos guardados en caché");
    }

    private String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    /** true si ya se hizo una consulta exitosa el día de hoy. */
    private boolean fetchedToday() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return today().equals(prefs.getString(KEY_FETCH_DATE, null));
    }

    private BcvData loadFromCache(boolean markAsCache) {
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
        data.isCache = markAsCache;
        data.vigenciaMsg = calcularVigencia(data.fechaValor);
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
