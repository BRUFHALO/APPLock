# 🚀 Instrucciones para Compilar "No Veas Mi Beta"

## ⚠️ IMPORTANTE
Para compilar esta app necesitas **Android Studio** instalado.

## 📋 Pasos para Compilar:

### Opción 1: Usar Android Studio (RECOMENDADO)

1. **Abre Android Studio**

2. **Importa el Proyecto:**
   - File → Open
   - Navega a: `C:\Users\PC\Documents\Proyectos\Applock\NoVeasMiBeta`
   - Click en "OK"

3. **Espera la Sincronización:**
   - Android Studio descargará Gradle automáticamente
   - Espera 2-3 minutos

4. **Compila el APK:**
   - Build → Build Bundle(s) / APK(s) → Build APK(s)
   - Espera 1-2 minutos

5. **Encuentra el APK:**
   - El APK estará en: `app\build\outputs\apk\debug\app-debug.apk`
   - Android Studio mostrará un link para abrir la carpeta

### Opción 2: Instalar Gradle Manualmente

Si no tienes Android Studio, puedes instalar Gradle:

1. Descarga Gradle: https://gradle.org/releases/
2. Extrae en `C:\Gradle`
3. Agrega a PATH: `C:\Gradle\bin`
4. Reinicia PowerShell
5. Ejecuta: `gradle wrapper`
6. Luego: `.\gradlew assembleDebug`

## 🎯 Probar en el Emulador

Una vez tengas el APK:

```powershell
# Instalar en emulador
adb install app\build\outputs\apk\debug\app-debug.apk

# Iniciar la app
adb shell am start -n com.noveasmibeta/.MainActivity
```

## ✅ Verificar que Funciona

1. Abre "No Veas Mi Beta" en el emulador
2. Toca "ACTIVAR PERMISOS"
3. Activa como Administrador del Dispositivo
4. Toca "BLOQUEAR PANTALLA"
5. ¡La pantalla debería bloquearse instantáneamente!

## 🔧 Solución de Problemas

**Error: "Gradle not found"**
- Necesitas Android Studio o instalar Gradle manualmente

**Error: "SDK not found"**
- Configura ANDROID_HOME apuntando a tu SDK de Android

**Error: "Device not found"**
- Asegúrate de que el emulador esté corriendo
- Ejecuta: `adb devices` para verificar

## 📞 Necesitas Ayuda?

Si tienes Android Studio instalado, el proceso es muy simple:
1. Abre el proyecto
2. Espera la sincronización
3. Build → Build APK
4. ¡Listo!

La compilación toma solo 1-2 minutos (mucho más rápido que React Native).
