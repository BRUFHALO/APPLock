const { withDangerousMod } = require('@expo/config-plugins');
const fs = require('fs');
const path = require('path');

const withNativeFiles = (config) => {
  return withDangerousMod(config, [
    'android',
    async (config) => {
      const projectRoot = config.modRequest.projectRoot;
      const androidProjectPath = path.join(config.modRequest.platformProjectRoot, 'app', 'src', 'main');
      
      // Crear directorio java si no existe
      const javaPath = path.join(androidProjectPath, 'java', 'app', 'rork', 'aq5x1apay2ugjrt36htwv');
      if (!fs.existsSync(javaPath)) {
        fs.mkdirSync(javaPath, { recursive: true });
      }

      // Copiar DeviceAdminReceiver.java
      const receiverSource = path.join(projectRoot, 'plugins', 'android-native', 'DeviceAdminReceiver.java');
      const receiverDest = path.join(javaPath, 'MyDeviceAdminReceiver.java');
      if (fs.existsSync(receiverSource)) {
        fs.copyFileSync(receiverSource, receiverDest);
      }

      // Copiar ScreenLockModule.java
      const moduleSource = path.join(projectRoot, 'plugins', 'android-native', 'ScreenLockModule.java');
      const moduleDest = path.join(javaPath, 'ScreenLockModule.java');
      if (fs.existsSync(moduleSource)) {
        fs.copyFileSync(moduleSource, moduleDest);
      }

      // Crear directorio xml si no existe
      const xmlPath = path.join(androidProjectPath, 'res', 'xml');
      if (!fs.existsSync(xmlPath)) {
        fs.mkdirSync(xmlPath, { recursive: true });
      }

      // Copiar device_admin.xml
      const xmlSource = path.join(projectRoot, 'plugins', 'android-native', 'device_admin.xml');
      const xmlDest = path.join(xmlPath, 'device_admin.xml');
      if (fs.existsSync(xmlSource)) {
        fs.copyFileSync(xmlSource, xmlDest);
      }

      return config;
    },
  ]);
};

module.exports = withNativeFiles;
