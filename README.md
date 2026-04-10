# Don't look at me 🔒

App nativa Android de calculadora multimoneda con tasas BCV y Binance en tiempo real.

## Características Principales

### 💰 Calculadora Multimoneda
- **4 monedas soportadas**: USD ($), EUR (€), VES (Bs.), USDT (₮)
- **Conversión en tiempo real** mientras escribes
- **Formato numérico local**: separador de miles con punto, decimales con coma (ej: 10.000,50)
- **Swap rápido**: intercambia montos y monedas entre inputs con un botón
- **Copiar al portapapeles**: copia cualquier monto fácilmente

### 📊 Tasas de Cambio en Tiempo Real

#### Tasa BCV (Banco Central de Venezuela)
- Actualización automática desde el sitio oficial del BCV
- Muestra tasa del dólar y euro
- Indicador de fecha valor
- Sistema de caché para consultas offline

#### Tasa Binance P2P (USDT/VES)
- Precio promedio de USDT en Bolívares desde Binance P2P
- Actualización cada 8 minutos (caché inteligente)
- Indicador "⚠️ Valor aproximado"
- Botón de refresco manual

### 📱 Widget de Home Screen
- Visualiza las 3 tasas principales sin abrir la app
- Dólar BCV
- Euro BCV
- USDT Binance
- Botón de actualización rápida
- Se actualiza automáticamente cada hora

### 📤 Compartir por WhatsApp
- Genera imagen con datos de pago móvil
- Incluye QR de tu banco (configurable)
- Muestra montos convertidos y tasa utilizada
- Tasa dinámica: muestra "Tasa BCV" o "Tasa Binance" según la moneda seleccionada
- Comparte directamente a WhatsApp o apps compatibles

### 🎨 Diseño Premium
- **Tema Dark + Gold**: interfaz elegante con acentos dorados
- **Fondo animado**: partículas y líneas conectadas (Data Portal)
- **Tarjetas oscuras**: diseño moderno tipo "glassmorphism"
- **Tipografía refinada**: combinación de serif y sans-serif

### 🔒 Funciones de Seguridad (Opcional)
- Bloqueo de pantalla con comando de voz
- Servicio de voz offline (Vosk) y online (Google Speech)
- Reconocimiento de comando "bloquear"
- Notificación persistente con tasas BCV

## Cómo compilar

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

## Cómo usar

### Calculadora de Divisas
1. Abre la app "Don't look at me"
2. Selecciona la moneda de origen (ej: USD)
3. Escribe el monto en el campo superior
4. Selecciona la moneda destino (ej: VES)
5. El monto convertido aparece automáticamente
6. Usa el botón ⇅ para intercambiar monedas

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

## Requisitos

- Android 7.0 (API 24) o superior
- Conexión a internet (para actualizar tasas en tiempo real)
- Permisos de almacenamiento (para guardar QR)

## Estructura del proyecto

```
NoVeasMiBeta/
├── app/src/main/
│   ├── java/com/noveasmibeta/
│   │   ├── CalculatorActivity.java      # Calculadora multimoneda
│   │   ├── BcvScraper.java              # Scraper de tasa BCV
│   │   ├── BinanceScraper.java          # API Binance P2P
│   │   ├── PaymentShareHelper.java      # Generador de imagen para WhatsApp
│   │   ├── DolarRateWidget.java         # Widget de home screen
│   │   ├── DataPortalView.java          # Fondo animado de partículas
│   │   ├── MainActivity.java            # Actividad principal
│   │   ├── GoogleVoiceService.java      # Reconocimiento de voz online
│   │   ├── VoskVoiceService.java        # Reconocimiento de voz offline
│   │   └── AdminReceiver.java           # Receptor de Device Admin
│   ├── res/
│   │   ├── layout/
│   │   │   ├── activity_calculator.xml  # UI calculadora
│   │   │   ├── activity_main.xml        # UI principal
│   │   │   └── widget_dolar_rate.xml    # Layout del widget
│   │   ├── drawable/                    # Fondos, botones, iconos
│   │   └── xml/                         # Configuraciones
│   └── AndroidManifest.xml
└── README.md
```

## APIs y Fuentes de Datos

- **BCV**: https://www.bcv.org.ve/ (scraping)
- **Binance P2P**: https://p2p.binance.com/bapi/c2c/v2/friendly/c2c/adv/search

## Licencia

Uso personal únicamente.
Desarrollado por BRUFHALO.
