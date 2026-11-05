---
agent: devin-local
session: well-polish
created: 2026-11-05T15:34:51Z
---
# Flutter "Don't look at me" — solo calculadora + widget Android

Simplificar la migración a Flutter: la app abre directo en la calculadora multimoneda (tasas BCV + Binance P2P, conversión, compartir QR por WhatsApp) y un widget de home screen solo para Android con refresco en segundo plano; iOS corre la app sin widget. Se crea docs/ para persistir los planes del proyecto.

## Alcance

Se mantiene: calculadora multimoneda completa (conversión USD/EUR/VES/USDT en tiempo real, tasas BCV y Binance con caché, compartir datos de pago con imagen QR), fondo animado de partículas, y widget de tasas **solo Android** con botón de refresco en segundo plano.

Se omite: bloqueo de pantalla / device admin, servicios de voz (Vosk/Google), notificación persistente BCV, pantalla home propia (la app abre directo en la calculadora).

## Estado actual (ya en disco)

- `lib/utils/number_format.dart`, `lib/utils/vigencia.dart` + tests — listos
- `lib/services/bcv_service.dart`, `binance_service.dart`, `qr_share_service.dart` — ya escritos
- `lib/widgets/data_portal.dart`, `lib/screens/calculator_screen.dart` — ya escritos
- `lib/main.dart` — sigue siendo el template del counter, hay que reescribirlo
- Android: `DolarRateWidget.kt`, `MainActivity.kt` (con MethodChannel de admin — a revertir), `AdminReceiver.kt` (a borrar), manifest (a limpiar), recursos del widget copiados
- **No existe `flutter_app/ios/`** — hay que generarla

## Pasos

### 0. Carpeta de planes
- Crear `docs/plans/` en la raíz del repo y guardar ahí este plan como `docs/plans/2026-11-05-flutter-calculadora-widget.md` (los planes futuros se guardan en esa carpeta)

### 1. Limpieza de lo omitido
- Borrar `android/app/src/main/kotlin/.../AdminReceiver.kt` y `res/xml/device_admin.xml`
- `MainActivity.kt` → volver a `class MainActivity : FlutterActivity()` plano
- `AndroidManifest.xml`: quitar receiver `AdminReceiver` y permiso `BIND_DEVICE_ADMIN`; **mantener** receiver `DolarRateWidget` y `HomeWidgetBackgroundReceiver`

### 2. Servicio de widget (nuevo: `lib/services/widget_service.dart`)
- `pushRatesToWidget()`: guarda `widget_dolar`, `widget_euro`, `widget_usdt`, `widget_last_update` con `HomeWidget.saveWidgetData` y llama `HomeWidget.updateWidget(name: 'DolarRateWidget')`
- Función top-level `@pragma('vm:entry-point')` para `HomeWidget.registerInteractivityCallback`: al recibir `dlam://widget_refresh` → `DartPluginRegistrant.ensureInitialized()`, re-consulta BCV + Binance y hace push al widget
- Llamar al push desde `calculator_screen.dart` tras cada fetch exitoso (BCV y Binance); registrar el callback en `main()`

### 3. `main.dart` — reescribir
- `MaterialApp` con `ThemeData.dark()` + seed dorado (#D4AF37), `home: CalculatorScreen()`
- Registrar `registerInteractivityCallback` antes de `runApp` con `WidgetsFlutterBinding.ensureInitialized()`

### 4. iOS scaffold (solo la app corre, sin widget)
- `flutter create --platforms=ios .` dentro de `flutter_app/` para generar `ios/`
- En `ios/Runner/Info.plist`: agregar `NSPhotoLibraryUsageDescription` (requerido por image_picker para el QR)
- Nota: compilar iOS requiere Mac/Xcode; el scaffold queda listo

### 5. Verificación
- `flutter analyze` en `flutter_app/`
- `dart test` (tests existentes de number_format/vigencia)
- Revisar que `pubspec.yaml` quede consistente (image_picker, share_plus, path_provider, home_widget, http, html, shared_preferences — todos necesarios aún)

## Archivos a tocar

- `docs/plans/2026-11-05-flutter-calculadora-widget.md` — nuevo (este plan)
- `lib/main.dart` — reescribir
- `lib/services/widget_service.dart` — nuevo
- `lib/screens/calculator_screen.dart` — agregar llamadas a `pushRatesToWidget()`
- `android/app/src/main/kotlin/.../MainActivity.kt` — revertir a FlutterActivity plano
- `android/app/src/main/AndroidManifest.xml` — quitar device admin
- Borrar: `AdminReceiver.kt`, `res/xml/device_admin.xml`
- `flutter create --platforms=ios` → nuevo `ios/` + editar `Info.plist`

## Riesgos

- `registerInteractivityCallback` necesita que el refresco del widget levante el isolate en segundo plano; si falla en un dispositivo específico, el widget igual muestra los últimos datos empujados por la app
- El share por WhatsApp en Android moderno va por el share sheet del sistema (share_plus) — comportamiento equivalente, aunque no fuerza WhatsApp directo como el `setPackage` original
