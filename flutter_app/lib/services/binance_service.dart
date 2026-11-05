/// Servicio de tasa USDT/VES desde Binance P2P.
///
/// Port de BinanceScraper.java: POST al endpoint de anuncios P2P y promedia
/// los 5 primeros precios. El resultado se reutiliza por 10 minutos — el
/// botón 🔄 pasa `force: true` para refresco manual.
library;

import 'dart:convert';

import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

const String _kApiUrl =
    'https://p2p.binance.com/bapi/c2c/v2/friendly/c2c/adv/search';
const String _kUserAgent =
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 '
    '(KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36';
const Duration _kTimeout = Duration(seconds: 15);
const Duration _kCacheDuration = Duration(minutes: 10);

// Claves de caché (mismas que BinanceScraper).
const String _kKeyPrice = 'binance_usdt_price';
const String _kKeyLastUpdate = 'binance_last_update';
const String _kKeyTimestamp = 'binance_timestamp';

class BinanceData {
  BinanceData({
    required this.priceStr,
    required this.price,
    required this.lastUpdate,
    required this.isCache,
  });

  final String priceStr;
  final double price;
  final String lastUpdate;
  final bool isCache;
}

class BinanceResult {
  BinanceResult.success(this.data)
      : error = null,
        cachedData = null;

  BinanceResult.failure(this.error, this.cachedData) : data = null;

  final BinanceData? data;
  final String? error;
  final BinanceData? cachedData;

  bool get isSuccess => data != null;
}

class BinanceService {
  /// Obtiene el precio promedio USDT/VES. Si el caché tiene menos de
  /// 10 minutos y [force] es false, lo devuelve sin llamar a la API —
  /// el botón 🔄 pasa [force] = true para refresco manual.
  /// En caso de error devuelve el caché aunque esté vencido.
  Future<BinanceResult> fetchUsdtVesPrice({bool force = false}) async {
    final BinanceData? cached = await _getCachedData();

    if (!force && cached != null && !await _isCacheExpired()) {
      return BinanceResult.success(
        BinanceData(
          priceStr: cached.priceStr,
          price: cached.price,
          lastUpdate: cached.lastUpdate,
          isCache: false,
        ),
      );
    }

    try {
      final BinanceData? data = await _fetchFromApi();
      if (data != null) {
        await _saveToCache(data);
        return BinanceResult.success(data);
      }
      return BinanceResult.failure('Error al obtener datos', cached);
    } catch (e) {
      return BinanceResult.failure('Error: $e', cached);
    }
  }

  Future<BinanceData?> _fetchFromApi() async {
    final http.Response response = await http
        .post(
          Uri.parse(_kApiUrl),
          headers: {
            'Content-Type': 'application/json',
            'User-Agent': _kUserAgent,
          },
          body: jsonEncode({
            'asset': 'USDT',
            'fiat': 'VES',
            'merchantCheck': true,
            'page': 1,
            'rows': 10,
            'payTypes': <String>[],
            'publisherType': null,
            'tradeType': 'BUY',
          }),
        )
        .timeout(_kTimeout);

    if (response.statusCode != 200) {
      throw Exception('HTTP error: ${response.statusCode}');
    }

    final List<dynamic> data =
        (jsonDecode(response.body) as Map<String, dynamic>)['data'] as List<dynamic>;
    if (data.isEmpty) return null;

    double totalPrice = 0;
    int count = 0;
    for (int i = 0; i < data.length && i < 5; i++) {
      final adv = (data[i] as Map<String, dynamic>)['adv'] as Map<String, dynamic>;
      totalPrice += double.parse(adv['price'] as String);
      count++;
    }
    final double averagePrice = totalPrice / count;

    final now = DateTime.now();
    return BinanceData(
      price: averagePrice,
      priceStr: averagePrice.toStringAsFixed(2),
      lastUpdate:
          '${now.day.toString().padLeft(2, '0')}/${now.month.toString().padLeft(2, '0')}/${now.year} '
          '${now.hour.toString().padLeft(2, '0')}:${now.minute.toString().padLeft(2, '0')}',
      isCache: false,
    );
  }

  static Future<void> _saveToCache(BinanceData data) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_kKeyPrice, data.priceStr);
    await prefs.setString(_kKeyLastUpdate, data.lastUpdate);
    await prefs.setInt(_kKeyTimestamp, DateTime.now().millisecondsSinceEpoch);
  }

  static Future<BinanceData?> _getCachedData() async {
    final prefs = await SharedPreferences.getInstance();
    final String? priceStr = prefs.getString(_kKeyPrice);
    final String? lastUpdate = prefs.getString(_kKeyLastUpdate);
    if (priceStr == null || lastUpdate == null) return null;

    final double? price = double.tryParse(priceStr);
    if (price == null) return null;

    return BinanceData(
      priceStr: priceStr,
      price: price,
      lastUpdate: lastUpdate,
      isCache: true,
    );
  }

  /// true si el caché tiene más de 10 minutos (o no existe).
  static Future<bool> _isCacheExpired() async {
    final prefs = await SharedPreferences.getInstance();
    final int timestamp = prefs.getInt(_kKeyTimestamp) ?? 0;
    return DateTime.now().millisecondsSinceEpoch - timestamp >
        _kCacheDuration.inMilliseconds;
  }
}
