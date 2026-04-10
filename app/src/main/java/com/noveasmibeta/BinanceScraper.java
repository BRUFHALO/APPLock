package com.noveasmibeta;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BinanceScraper {

    private static final String TAG = "BinanceScraper";
    private static final String BINANCE_API_URL = "https://p2p.binance.com/bapi/c2c/v2/friendly/c2c/adv/search";
    private static final int TIMEOUT_MS = 15000;
    private static final long CACHE_DURATION_MS = 8 * 60 * 1000; // 8 minutos

    private static final String PREFS_NAME = "BinanceCache";
    private static final String KEY_PRICE = "usdt_price";
    private static final String KEY_LAST_UPDATE = "last_update";
    private static final String KEY_IS_CACHE = "is_cache";

    private final Context context;
    private final ExecutorService executor;

    public interface BinanceCallback {
        void onSuccess(BinanceData data);
        void onError(String error, BinanceData cachedData);
    }

    public static class BinanceData {
        public String priceStr;
        public double price;
        public String lastUpdate;
        public boolean isCache;
    }

    public BinanceScraper(Context context) {
        this.context = context.getApplicationContext();
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void fetchUsdtVesPrice(BinanceCallback callback) {
        // Verificar si hay caché válido (menos de 8 minutos)
        BinanceData cachedData = getCachedData();
        if (cachedData != null && !isCacheExpired()) {
            Log.d(TAG, "Usando caché de Binance");
            cachedData.isCache = true;
            callback.onSuccess(cachedData);
            return;
        }

        // Si no hay caché válido, hacer la petición
        if (!isNetworkAvailable()) {
            if (cachedData != null) {
                cachedData.isCache = true;
                callback.onError("Sin conexión a internet", cachedData);
            } else {
                callback.onError("Sin conexión y sin datos en caché", null);
            }
            return;
        }

        executor.execute(() -> {
            try {
                BinanceData data = fetchFromApi();
                if (data != null) {
                    saveToCache(data);
                    data.isCache = false;
                    callback.onSuccess(data);
                } else {
                    if (cachedData != null) {
                        cachedData.isCache = true;
                        callback.onError("Error al obtener datos", cachedData);
                    } else {
                        callback.onError("Error al obtener datos", null);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error en fetch", e);
                if (cachedData != null) {
                    cachedData.isCache = true;
                    callback.onError("Error: " + e.getMessage(), cachedData);
                } else {
                    callback.onError("Error: " + e.getMessage(), null);
                }
            }
        });
    }

    private BinanceData fetchFromApi() throws Exception {
        URL url = new URL(BINANCE_API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36");
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setDoOutput(true);

        // Request body
        String jsonInput = "{\n" +
                "  \"asset\": \"USDT\",\n" +
                "  \"fiat\": \"VES\",\n" +
                "  \"merchantCheck\": true,\n" +
                "  \"page\": 1,\n" +
                "  \"rows\": 10,\n" +
                "  \"payTypes\": [],\n" +
                "  \"publisherType\": null,\n" +
                "  \"tradeType\": \"BUY\"\n" +
                "}";

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonInput.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new Exception("HTTP error: " + responseCode);
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            response.append(line.trim());
        }
        in.close();

        // Parse JSON
        JSONObject jsonResponse = new JSONObject(response.toString());
        JSONArray data = jsonResponse.getJSONArray("data");

        if (data.length() == 0) {
            return null;
        }

        // Calcular promedio de precios
        double totalPrice = 0;
        int count = 0;

        for (int i = 0; i < Math.min(data.length(), 5); i++) {
            JSONObject item = data.getJSONObject(i);
            JSONObject adv = item.getJSONObject("adv");
            String priceStr = adv.getString("price");
            double price = Double.parseDouble(priceStr);
            totalPrice += price;
            count++;
        }

        double averagePrice = totalPrice / count;

        BinanceData result = new BinanceData();
        result.price = averagePrice;
        result.priceStr = String.format(Locale.US, "%.2f", averagePrice);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        result.lastUpdate = sdf.format(new Date());

        return result;
    }

    private void saveToCache(BinanceData data) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_PRICE, data.priceStr);
        editor.putString(KEY_LAST_UPDATE, data.lastUpdate);
        editor.putLong("timestamp", System.currentTimeMillis());
        editor.apply();
    }

    private BinanceData getCachedData() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String priceStr = prefs.getString(KEY_PRICE, null);
        String lastUpdate = prefs.getString(KEY_LAST_UPDATE, null);

        if (priceStr == null || lastUpdate == null) {
            return null;
        }

        BinanceData data = new BinanceData();
        data.priceStr = priceStr;
        try {
            data.price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            return null;
        }
        data.lastUpdate = lastUpdate;
        data.isCache = true;

        return data;
    }

    private boolean isCacheExpired() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long timestamp = prefs.getLong("timestamp", 0);
        return (System.currentTimeMillis() - timestamp) > CACHE_DURATION_MS;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    public void destroy() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
