import { NativeModulesProxy, EventEmitter, Subscription } from 'expo-modules-core';

// Import the native module. On web, it will be resolved to ScreenLock.web.ts
// and on native platforms to ScreenLock.ts
import ScreenLockModule from './src/ScreenLockModule';

export async function isDeviceAdminEnabled(): Promise<boolean> {
  return await ScreenLockModule.isDeviceAdminEnabled();
}

export async function requestDeviceAdminPermission(): Promise<boolean> {
  return await ScreenLockModule.requestDeviceAdminPermission();
}

export async function lockScreen(): Promise<boolean> {
  return await ScreenLockModule.lockScreen();
}

export async function openDeviceAdminSettings(): Promise<void> {
  return await ScreenLockModule.openDeviceAdminSettings();
}
