import { NativeModules, Platform, Linking } from 'react-native';

const { ScreenLockModule } = NativeModules;

export interface ScreenLockInterface {
  isDeviceAdminEnabled(): Promise<boolean>;
  requestDeviceAdminPermission(): Promise<boolean>;
  lockScreen(): Promise<boolean>;
  openDeviceAdminSettings(): Promise<void>;
}

// Implementación para Android
const ScreenLockAndroid: ScreenLockInterface = {
  async isDeviceAdminEnabled(): Promise<boolean> {
    if (!ScreenLockModule) {
      console.warn('ScreenLockModule not available');
      return false;
    }
    try {
      return await ScreenLockModule.isDeviceAdminEnabled();
    } catch (error) {
      console.error('Error checking device admin:', error);
      return false;
    }
  },

  async requestDeviceAdminPermission(): Promise<boolean> {
    if (!ScreenLockModule) {
      console.warn('ScreenLockModule not available');
      return false;
    }
    try {
      return await ScreenLockModule.requestDeviceAdminPermission();
    } catch (error) {
      console.error('Error requesting device admin:', error);
      return false;
    }
  },

  async lockScreen(): Promise<boolean> {
    if (!ScreenLockModule) {
      console.warn('ScreenLockModule not available');
      return false;
    }
    try {
      const isEnabled = await ScreenLockModule.isDeviceAdminEnabled();
      if (!isEnabled) {
        console.warn('Device admin not enabled. Please enable it first.');
        return false;
      }
      return await ScreenLockModule.lockScreen();
    } catch (error) {
      console.error('Error locking screen:', error);
      return false;
    }
  },

  async openDeviceAdminSettings(): Promise<void> {
    try {
      await Linking.openSettings();
    } catch (error) {
      console.error('Error opening settings:', error);
    }
  },
};

// Implementación vacía para otras plataformas
const ScreenLockDefault: ScreenLockInterface = {
  async isDeviceAdminEnabled(): Promise<boolean> {
    return false;
  },
  async requestDeviceAdminPermission(): Promise<boolean> {
    return false;
  },
  async lockScreen(): Promise<boolean> {
    return false;
  },
  async openDeviceAdminSettings(): Promise<void> {
    console.warn('Screen lock is only available on Android');
  },
};

export default Platform.OS === 'android' ? ScreenLockAndroid : ScreenLockDefault;
