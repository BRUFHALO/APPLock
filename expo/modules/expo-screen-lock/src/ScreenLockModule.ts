// Este archivo será reemplazado por el módulo nativo en Android
// Por ahora es un placeholder para desarrollo
export default {
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
    // No-op
  },
};
