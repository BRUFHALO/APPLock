# APPLock / Don't look at me

App nativa Android (Java, en `app/`) + migración a Flutter (`flutter_app/`).

## Alcance de la versión Flutter

Solo **calculadora multimoneda** (tasas BCV + Binance P2P, compartir QR) + **widget de home screen solo Android**. Sin bloqueo de pantalla, sin voz, sin notificación persistente. Plan completo en `docs/plans/2026-11-05-flutter-calculadora-widget.md`. Los planes se guardan en `docs/plans/`.

## Build (Linux)

```bash
export PATH="$PATH:/home/ronaldorivero/flutter/bin"
export JAVA_HOME=/home/ronaldorivero/jdk/jdk-17.0.21+9   # el sistema solo tiene JRE
cd flutter_app
flutter analyze          # lint
dart test                # tests de utils
flutter build apk --debug
```

- Flutter 3.47.9 en `~/flutter` (no está en PATH).
- JDK 17 portable en `~/jdk/jdk-17.0.21+9` — **necesario** o Gradle falla con "does not provide the required capabilities: [JAVA_COMPILER]".
- Android SDK en `~/Android/Sdk`.
- iOS: scaffold generado (`ios/`), pero compilar requiere Mac/Xcode. Sin widget en iOS.

## Arquitectura Flutter

- `lib/main.dart` → `CalculatorScreen` directo (no hay home propia).
- `lib/services/bcv_service.dart` — scraper bcv.org.ve (html) + caché SharedPreferences.
- `lib/services/binance_service.dart` — API P2P Binance, caché 8 min.
- `lib/services/qr_share_service.dart` — compone imagen de pago y comparte (share_plus).
- `lib/services/widget_service.dart` — push de tasas al widget + callback `dlam://widget_refresh` (interactivity de home_widget).
- `android/.../DolarRateWidget.kt` — HomeWidgetProvider que lee HomeWidgetPreferences.
- `lib/utils/number_format.dart`, `vigencia.dart` — lógica pura con tests en `test/`.
