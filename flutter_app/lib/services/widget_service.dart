/// Empuja las tasas al widget de home screen de Android.
///
/// Usa el paquete home_widget: los datos se escriben en SharedPreferences
/// ("HomeWidgetPreferences") que lee DolarRateWidget.kt, y el botón ↻ del
/// widget dispara [widgetInteractivityCallback] en un isolate de fondo.
library;

import 'dart:ui' show DartPluginRegistrant;

import 'package:flutter/widgets.dart';
import 'package:home_widget/home_widget.dart';

import 'bcv_service.dart';
import 'binance_service.dart';

const String _kWidgetName = 'DolarRateWidget';
const String _kRefreshUri = 'dlam://widget_refresh';

/// Guarda las tasas actuales en el widget y pide que se redibuje.
Future<void> pushRatesToWidget({
  required double dolar,
  required double euro,
  required double usdt,
}) async {
  try {
    final now = DateTime.now();
    final hhmm =
        '${now.hour.toString().padLeft(2, '0')}:${now.minute.toString().padLeft(2, '0')}';
    await HomeWidget.saveWidgetData<String>(
        'widget_dolar', dolar > 0 ? dolar.toStringAsFixed(2) : '---');
    await HomeWidget.saveWidgetData<String>(
        'widget_euro', euro > 0 ? euro.toStringAsFixed(2) : '---');
    await HomeWidget.saveWidgetData<String>(
        'widget_usdt', usdt > 0 ? usdt.toStringAsFixed(2) : '---');
    await HomeWidget.saveWidgetData<String>('widget_last_update', hhmm);
    await HomeWidget.updateWidget(name: _kWidgetName);
  } catch (e) {
    debugPrint('pushRatesToWidget: $e');
  }
}

/// Callback que corre en segundo plano cuando el usuario toca ↻ en el widget.
@pragma('vm:entry-point')
Future<void> widgetInteractivityCallback(Uri? uri) async {
  if (uri?.toString() != _kRefreshUri) return;
  WidgetsFlutterBinding.ensureInitialized();
  DartPluginRegistrant.ensureInitialized();

  double dolar = 0, euro = 0, usdt = 0;

  final bcv = await BcvService().obtenerTasas(force: true);
  final bcvData = bcv.data ?? bcv.cachedData;
  if (bcvData != null) {
    dolar = bcvData.dolar;
    euro = bcvData.euro;
  }

  final binance = await BinanceService().fetchUsdtVesPrice(force: true);
  final binanceData = binance.data ?? binance.cachedData;
  if (binanceData != null) {
    usdt = binanceData.price;
  }

  await pushRatesToWidget(dolar: dolar, euro: euro, usdt: usdt);
}

/// Registra el callback de interactividad del widget (llamar en main()).
Future<void> registerWidgetCallback() async {
  try {
    await HomeWidget.setAppGroupId('group.com.noveasmibeta.dont_look_at_me');
    await HomeWidget.registerInteractivityCallback(widgetInteractivityCallback);
  } catch (e) {
    debugPrint('registerWidgetCallback: $e');
  }
}
