# 🚀 Instrucciones para Compilar "No Veas Mi Beta" (Linux)

## ✅ Requisitos

- **Android Studio** instalado en `~/android-studio`
- **Android SDK** en `~/Android/Sdk`
- El JDK necesario ya viene **bundled con Android Studio** (`~/android-studio/jbr`) — no se necesita instalar JDK por separado

---

## 📋 Opción 1: Línea de Comandos (Recomendado)

### Paso 1 — Configurar el SDK (solo la primera vez)

El archivo `local.properties` debe apuntar al SDK de Linux. Ya está configurado, pero si necesitas regenerarlo:

```bash
echo "sdk.dir=$HOME/Android/Sdk" > local.properties
```

### Paso 2 — Dar permisos al wrapper de Gradle

```bash
chmod +x gradlew
```

### Paso 3 — Compilar el APK debug

```bash
export JAVA_HOME=~/android-studio/jbr
./gradlew assembleDebug
```

La primera vez tarda ~2 min (descarga dependencias). Las siguientes veces ~30 seg.

### Paso 4 — Ubicación del APK

```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 📋 Opción 2: Android Studio (GUI)

1. Abre Android Studio
2. **File → Open** → selecciona la carpeta `NoVeasMiBeta`
3. Espera que Gradle sincronice (~1-2 min)
4. **Build → Build Bundle(s) / APK(s) → Build APK(s)**
5. El APK estará en: `app/build/outputs/apk/debug/app-debug.apk`
6. Android Studio mostrará un enlace directo para abrir la carpeta

---

## 📱 Instalar en dispositivo / emulador

```bash
# Verificar dispositivos conectados
~/Android/Sdk/platform-tools/adb devices

# Instalar el APK
~/Android/Sdk/platform-tools/adb install app/build/outputs/apk/debug/app-debug.apk

# Iniciar la app
~/Android/Sdk/platform-tools/adb shell am start -n com.noveasmibeta/.MainActivity
```

---

## 🔧 Solución de Problemas

**Error: "Toolchain does not provide JAVA_COMPILER"**
- El sistema tiene JRE pero no JDK. Solución: usa el JDK de Android Studio:
  ```bash
  export JAVA_HOME=~/android-studio/jbr
  ```

**Error: "SDK not found" / "sdk.dir" incorrecto**
- Regenera `local.properties`:
  ```bash
  echo "sdk.dir=$HOME/Android/Sdk" > local.properties
  ```

**Error: "Device not found"**
- Verifica que el emulador o dispositivo esté conectado:
  ```bash
  ~/Android/Sdk/platform-tools/adb devices
  ```

**Para compilar en background (sin bloquear la terminal)**
```bash
nohup bash -c 'export JAVA_HOME=~/android-studio/jbr && ./gradlew assembleDebug' > /tmp/gradle_build.txt 2>&1 &
# Ver progreso:
tail -f /tmp/gradle_build.txt
```
