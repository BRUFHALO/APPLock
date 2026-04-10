# Don't look at me 🔒

App nativa Android multifuncional: **Bloqueo de pantalla instantáneo** + **Calculadora multimoneda** con tasas BCV y Binance en tiempo real.

---

## 🔒 Bloqueo de Pantalla Instantáneo

La función principal de la app es permitir bloquear la pantalla de tu dispositivo de múltiples formas:

### Botón de Bloqueo Manual
- **Bloqueo instantáneo** con un solo toque
- Requiere permisos de Administrador del Dispositivo (solicitados en la app)
- Funciona incluso cuando la app está en segundo plano

### Comando de Voz "Bloquear"
- **Bloqueo sin tocar el teléfono**: simplemente di "bloquear", "bloquea", "lock" o "locker"
- **Servicio híbrido inteligente**:
  - **Modo Online** (con internet): usa Google Speech Recognition para máxima precisión
  - **Modo Offline** (sin internet): usa Vosk (reconocimiento de voz local)
- El servicio se adapta automáticamente según la conectividad disponible
- **Servicio persistente**: se reinicia automáticamente si la app es cerrada
- **Notificación en barra de estado** indicando que la escucha está activa

---

## 💰 Calculadora Multimoneda

Conversor de divisas profesional con tasas actualizadas:

### Monedas Soportadas
- **USD** ($) - Dólar estadounidense
- **EUR** (€) - Euro
- **VES** (Bs.) - Bolívar venezolano
- **USDT** (₮) - Tether (criptomoneda)

### Funciones de Conversión
- **Conversión en tiempo real** mientras escribes
- **Formato numérico local**: separador de miles con punto, decimales con coma (ej: 10.000,50)
- **Swap rápido** (⇅): intercambia montos y monedas entre inputs con un botón
- **Copiar al portapapeles**: copia cualquier monto fácilmente con el botón 📋

---

## 📊 Tasas de Cambio en Tiempo Real

### Tasa BCV (Banco Central de Venezuela)
- **Actualización automática** desde el sitio oficial del BCV
- Muestra tasa del **dólar** y **euro**
- **Indicador de fecha valor** de las tasas
- **Sistema de caché** para consultas offline (muestra últimos datos disponibles)
- **Notificación persistente opcional** con tasas visibles en la barra de notificaciones
- **Widget de home screen** con dólar y euro BCV

### Tasa Binance P2P (USDT/VES)
- **Precio promedio** de USDT en Bolívares desde Binance P2P
- **Caché inteligente de 8 minutos**: evita llamadas excesivas a la API
- **Indicador "⚠️ Valor aproximado"** debajo del precio
- Botón de refresco manual 🔄
- Seleccionable en la calculadora para conversiones USDT↔VES

---

## 📱 Widget de Home Screen

Visualiza las tasas sin abrir la app:
- **3 tasas principales**: Dólar BCV, Euro BCV, USDT Binance
- **Símbolos de moneda**: $, €, ₮
- **Botón de actualización rápida** (↻)
- **Se actualiza automáticamente** cada hora
- Al tocar el widget abre la calculadora

---

## 📤 Compartir por WhatsApp

Genera imágenes de pago móvil para compartir:
- **QR del banco configurable**: guarda tu código QR de pago móvil
- **Imagen generada con**:
  - QR de tu banco
  - Montos convertidos
  - Tasa utilizada (BCV o Binance según la moneda)
  - Diseño profesional Dark + Gold
- **Comparte directamente** a WhatsApp o cualquier app compatible
- Soporta símbolo ₮ para USDT en la imagen compartida

---

## 🎨 Diseño Premium

- **Tema Dark + Gold**: interfaz elegante con acentos dorados (#D4AF37)
- **Fondo animado** "Data Portal": partículas y líneas conectadas que se mueven
- **Tarjetas oscuras** con esquinas redondeadas: diseño moderno tipo "glassmorphism"
- **Tipografía refinada**: combinación de serif y sans-serif
- **Animaciones suaves** en botones y transiciones

---

## 🚀 Cómo usar

### Bloquear Pantalla (Principal)
1. Abre la app "Don't look at me"
2. Toca "⚙️ ACTIVAR PERMISOS" y concede permisos de Administrador
3. Ahora puedes:
   - Tocar "🔒 BLOQUEAR PANTALLA" para bloquear manualmente
   - Tocar "🎤 COMANDO DE VOZ" para activar escucha continua
   - Decir "bloquear" para bloquear sin tocar el teléfono

### Usar la Calculadora
1. En la pantalla principal, toca "🧮 CALCULADORA MULTIMONEDA"
2. Selecciona la moneda de origen y destino
3. Escribe el monto y la conversión es automática
4. Usa ⇅ para intercambiar monedas

### Configurar QR del Banco
1. En la calculadora, toca "📤 SELECCIONAR QR Y COMPARTIR"
2. Selecciona la imagen de tu código QR de pago móvil
3. El QR se guarda para futuros usos

### Compartir Pago
1. Asegúrate de tener el QR configurado
2. Ingresa los montos en la calculadora
3. Toca "📤 COMPARTIR POR WHATSAPP"
4. Selecciona el contacto o grupo

### Agregar Widget
1. En tu home screen, mantén presionado en un espacio vacío
2. Selecciona "Widgets"
3. Busca "Don't look at me"
4. Arrastra el widget a tu home screen

---

## ⚙️ Cómo compilar

### Opción 1: Android Studio (Recomendado)
1. Abre Android Studio
2. File → Open → Selecciona la carpeta `NoVeasMiBeta`
3. Espera a que Gradle sincronice
4. Build → Build Bundle(s) / APK(s) → Build APK(s)
5. El APK estará en: `app/build/outputs/apk/debug/app-debug.apk`

### Opción 2: Línea de comandos
```bash
cd NoVeasMiBeta
.\gradlew assembleDebug
```

El APK estará en: `app/build/outputs/apk/debug/app-debug.apk`

---

## 📋 Requisitos

- **Android 7.0** (API 24) o superior
- **Permisos necesarios**:
  - Administrador del Dispositivo (para bloquear pantalla)
  - Micrófono (para comando de voz)
  - Internet (para actualizar tasas en tiempo real)
  - Almacenamiento (para guardar QR del banco)
- **Desinstalación especial**: Primero desactivar permisos de Admin en Ajustes → Seguridad → Administradores del dispositivo

---

## 🗂️ Estructura del Proyecto

```
NoVeasMiBeta/
├── app/src/main/
│   ├── java/com/noveasmibeta/
│   │   ├── MainActivity.java              # Actividad principal (bloqueo + voz)
│   │   ├── CalculatorActivity.java        # Calculadora multimoneda
│   │   ├── AdminReceiver.java             # Receptor de Device Admin
│   │   ├── BcvScraper.java                # Scraper de tasa BCV
│   │   ├── BinanceScraper.java            # API Binance P2P
│   │   ├── PaymentShareHelper.java        # Generador de imagen WhatsApp
│   │   ├── DolarRateWidget.java           # Widget de home screen
│   │   ├── DataPortalView.java            # Fondo animado de partículas
│   │   ├── GoogleVoiceService.java        # Reconocimiento de voz online
│   │   ├── VoskVoiceService.java          # Reconocimiento de voz offline
│   │   ├── VoiceListenerService.java      # Servicio de escucha de voz
│   │   ├── BcvNotificationService.java    # Notificación persistente BCV
│   │   └── BootReceiver.java              # Inicio automático al bootear
│   ├── res/
│   │   ├── layout/
│   │   │   ├── activity_main.xml          # UI principal (bloqueo)
│   │   │   ├── activity_calculator.xml    # UI calculadora
│   │   │   └── widget_dolar_rate.xml     # Layout del widget
│   │   ├── drawable/                     # Fondos, botones, iconos
│   │   └── xml/                          # Configuraciones
│   └── AndroidManifest.xml
└── README.md
```

---

## 🔌 APIs y Fuentes de Datos

- **BCV**: https://www.bcv.org.ve/ (scraping de tasas oficiales)
- **Binance P2P**: https://p2p.binance.com/bapi/c2c/v2/friendly/c2c/adv/search
- **Google Speech**: API de reconocimiento de voz online
- **Vosk**: Reconocimiento de voz offline (modelo local)

---

## 📄 Licencia

Uso personal únicamente.

**Desarrollado por BRUFHALO**
