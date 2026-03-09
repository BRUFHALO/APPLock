# ⚠️ IMPORTANTE: Reconocimiento de Voz

## 🎤 El reconocimiento de voz NO funciona en emuladores

El servicio de reconocimiento de voz de Google **requiere un dispositivo Android real** porque:

- ❌ Los emuladores no tienen micrófono físico
- ❌ Google Speech API no está disponible en emuladores
- ❌ Los servicios de Google Play pueden no estar completos

---

## ✅ Soluciones:

### **Opción 1: Usar en un teléfono Android real (RECOMENDADO)**

1. **Compila el APK en Android Studio:**
   - Build → Build Bundle(s) / APK(s) → Build APK(s)
   - Espera 1-2 minutos
   - Click en "locate" cuando termine

2. **Transfiere el APK a tu teléfono:**
   - Por USB, Bluetooth, email, etc.
   - Archivo: `app-debug.apk`

3. **Instala en tu teléfono:**
   - Habilita "Instalar apps desconocidas"
   - Abre el APK e instala

4. **Usa la app:**
   - Activa permisos de Device Admin
   - Acepta permiso de micrófono
   - Toca "🎤 INICIAR ESCUCHA"
   - **Di "bloquear"** → ¡Pantalla bloqueada!

---

### **Opción 2: Usar solo el botón manual**

Si solo quieres probar el bloqueo sin voz:

1. **Abre la app en el emulador**
2. **Activa permisos de Device Admin**
3. **Toca "🔒 BLOQUEAR PANTALLA"**
4. ✅ La pantalla se bloqueará

---

## 📱 Características de la App:

### ✅ **Funciona en emulador:**
- Botón manual de bloqueo
- Permisos de Device Admin
- Interfaz de usuario

### ⚠️ **Solo funciona en teléfono real:**
- Reconocimiento de voz continuo
- Comando "bloquear"
- Escucha en segundo plano

---

## 🔧 Cómo generar el APK:

1. **En Android Studio:**
   - Build → Build Bundle(s) / APK(s) → Build APK(s)

2. **Ubicación del APK:**
   ```
   NoVeasMiBeta\app\build\outputs\apk\debug\app-debug.apk
   ```

3. **Instalar en teléfono:**
   - Conecta tu teléfono por USB
   - O envía el APK por cualquier medio
   - Instala y prueba

---

## 🎯 Resumen:

| Característica | Emulador | Teléfono Real |
|----------------|----------|---------------|
| Botón manual | ✅ Funciona | ✅ Funciona |
| Bloqueo de pantalla | ✅ Funciona | ✅ Funciona |
| Comando de voz | ❌ No funciona | ✅ Funciona |
| Escucha continua | ❌ No funciona | ✅ Funciona |

---

## 💡 Recomendación:

**Para probar el comando de voz "bloquear", necesitas instalarlo en un teléfono Android real.**

El botón manual funciona perfectamente en el emulador para probar el bloqueo.
