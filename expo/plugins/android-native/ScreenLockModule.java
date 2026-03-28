package app.rork.aq5x1apay2ugjrt36htwv;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

public class ScreenLockModule extends ReactContextBaseJavaModule {
    private static final int REQUEST_CODE_ENABLE_ADMIN = 1;
    private DevicePolicyManager devicePolicyManager;
    private ComponentName adminComponent;

    public ScreenLockModule(ReactApplicationContext reactContext) {
        super(reactContext);
        devicePolicyManager = (DevicePolicyManager) reactContext.getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponent = new ComponentName(reactContext, MyDeviceAdminReceiver.class);
    }

    @Override
    public String getName() {
        return "ScreenLockModule";
    }

    @ReactMethod
    public void isDeviceAdminEnabled(Promise promise) {
        try {
            boolean isAdmin = devicePolicyManager.isAdminActive(adminComponent);
            promise.resolve(isAdmin);
        } catch (Exception e) {
            promise.reject("ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void requestDeviceAdminPermission(Promise promise) {
        try {
            if (devicePolicyManager.isAdminActive(adminComponent)) {
                promise.resolve(true);
                return;
            }

            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Necesitas activar el administrador del dispositivo para permitir que la app bloquee la pantalla con comandos de voz.");

            getCurrentActivity().startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN);
            promise.resolve(true);
        } catch (Exception e) {
            promise.reject("ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void lockScreen(Promise promise) {
        try {
            if (!devicePolicyManager.isAdminActive(adminComponent)) {
                promise.reject("NOT_ADMIN", "Device admin not enabled");
                return;
            }

            devicePolicyManager.lockNow();
            promise.resolve(true);
        } catch (Exception e) {
            promise.reject("ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void openDeviceAdminSettings(Promise promise) {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS);
            getCurrentActivity().startActivity(intent);
            promise.resolve(null);
        } catch (Exception e) {
            promise.reject("ERROR", e.getMessage());
        }
    }
}
