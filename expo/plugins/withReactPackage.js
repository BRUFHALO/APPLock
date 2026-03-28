const { withMainApplication } = require('@expo/config-plugins');

const withReactPackage = (config) => {
  return withMainApplication(config, (config) => {
    const mainApplication = config.modResults;
    
    // Agregar import del módulo
    if (!mainApplication.contents.includes('import app.rork.aq5x1apay2ugjrt36htwv.ScreenLockPackage;')) {
      mainApplication.contents = mainApplication.contents.replace(
        /import com\.facebook\.react\.PackageList;/,
        `import com.facebook.react.PackageList;\nimport app.rork.aq5x1apay2ugjrt36htwv.ScreenLockPackage;`
      );
    }

    // Agregar el package a la lista
    if (!mainApplication.contents.includes('packages.add(new ScreenLockPackage());')) {
      mainApplication.contents = mainApplication.contents.replace(
        /List<ReactPackage> packages = new PackageList\(this\)\.getPackages\(\);/,
        `List<ReactPackage> packages = new PackageList(this).getPackages();\n      packages.add(new ScreenLockPackage());`
      );
    }

    return config;
  });
};

module.exports = withReactPackage;
