const { withAndroidManifest } = require('@expo/config-plugins');

const DEVICE_ADMIN_RECEIVER = `
    <receiver
      android:name=".DeviceAdminReceiver"
      android:permission="android.permission.BIND_DEVICE_ADMIN"
      android:exported="true">
      <meta-data
        android:name="android.app.device_admin"
        android:resource="@xml/device_admin" />
      <intent-filter>
        <action android:name="android.app.action.DEVICE_ADMIN_ENABLED" />
      </intent-filter>
    </receiver>`;

const withDeviceAdmin = (config) => {
  return withAndroidManifest(config, async (config) => {
    const androidManifest = config.modResults;
    const application = androidManifest.manifest.application[0];

    // Add BIND_DEVICE_ADMIN permission
    if (!androidManifest.manifest['uses-permission']) {
      androidManifest.manifest['uses-permission'] = [];
    }

    const hasDeviceAdminPermission = androidManifest.manifest['uses-permission'].some(
      (perm) => perm.$['android:name'] === 'android.permission.BIND_DEVICE_ADMIN'
    );

    if (!hasDeviceAdminPermission) {
      androidManifest.manifest['uses-permission'].push({
        $: { 'android:name': 'android.permission.BIND_DEVICE_ADMIN' },
      });
    }

    // Add receiver to application
    if (!application.receiver) {
      application.receiver = [];
    }

    const hasReceiver = application.receiver.some(
      (receiver) => receiver.$['android:name'] === '.DeviceAdminReceiver'
    );

    if (!hasReceiver) {
      application.receiver.push({
        $: {
          'android:name': '.DeviceAdminReceiver',
          'android:permission': 'android.permission.BIND_DEVICE_ADMIN',
          'android:exported': 'true',
        },
        'meta-data': [
          {
            $: {
              'android:name': 'android.app.device_admin',
              'android:resource': '@xml/device_admin',
            },
          },
        ],
        'intent-filter': [
          {
            action: [
              {
                $: { 'android:name': 'android.app.action.DEVICE_ADMIN_ENABLED' },
              },
            ],
          },
        ],
      });
    }

    return config;
  });
};

module.exports = withDeviceAdmin;
