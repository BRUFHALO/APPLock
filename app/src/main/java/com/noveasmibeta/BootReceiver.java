package com.noveasmibeta;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Dispositivo iniciado - Seleccionando servicio de voz");

            boolean hasInternet = isNetworkAvailable(context);
            Intent serviceIntent;

            if (hasInternet) {
                Log.d(TAG, "Internet disponible -> GoogleVoiceService");
                serviceIntent = new Intent(context, GoogleVoiceService.class);
            } else {
                Log.d(TAG, "Sin internet -> VoskVoiceService");
                serviceIntent = new Intent(context, VoskVoiceService.class);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        }
    }

    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
        return false;
    }
}
