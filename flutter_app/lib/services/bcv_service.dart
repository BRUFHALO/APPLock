/// Servicio de tasas del BCV (Banco Central de Venezuela).
///
/// Port de BcvScraper.java: descarga www.bcv.org.ve, extrae los precios del
/// dólar y el euro con selectores CSS y los guarda en caché
/// (SharedPreferences) para consulta offline.
library;

import 'dart:convert';
import 'dart:io';

import 'package:html/parser.dart' as html_parser;
import 'package:http/http.dart' as http;
import 'package:http/io_client.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../utils/number_format.dart';
import '../utils/vigencia.dart';

const String _kBcvUrl = 'https://www.bcv.org.ve/';
const String _kUserAgent =
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 '
    '(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36';
const Duration _kTimeout = Duration(seconds: 15);

// Fallback si el scraping a bcv.org.ve falla.
const String _kDolarApiUsd = 'https://ve.dolarapi.com/v1/dolares/oficial';
const String _kDolarApiEur = 'https://ve.dolarapi.com/v1/euros/oficial';

// Claves de caché (mismas que BcvScraper).
const String _kKeyDolar = 'bcv_dolar_price';
const String _kKeyEuro = 'bcv_euro_price';
const String _kKeyFechaValor = 'bcv_fecha_valor';
const String _kKeyLastUpdate = 'bcv_last_update';
const String _kKeyFetchDate = 'bcv_fetch_date';
const String _kKeyCacheVersion = 'bcv_cache_version';
const int _kCurrentCacheVersion = 2;

class BcvData {
  BcvData({
    required this.dolarStr,
    required this.euroStr,
    required this.dolar,
    required this.euro,
    required this.fechaValor,
    required this.lastUpdate,
    required this.isCache,
    required this.vigenciaMsg,
  });

  final String dolarStr;
  final String euroStr;
  final double dolar;
  final double euro;
  final String fechaValor;
  final String lastUpdate;
  final bool isCache;
  final String vigenciaMsg;

  @override
  String toString() =>
      'Dólar: $dolarStr Bs. | Euro: $euroStr Bs. | Fecha: $fechaValor';
}

/// Resultado de la consulta: éxito con datos nuevos, o error con caché (si hay).
class BcvResult {
  BcvResult.success(this.data)
      : error = null,
        cachedData = null;

  BcvResult.failure(this.error, this.cachedData) : data = null;

  final BcvData? data;
  final String? error;
  final BcvData? cachedData;

  bool get isSuccess => data != null;
}

class BcvService {
  /// HttpClient que acepta el certificado del BCV (equivalente al
  /// SSLSocketFactory trust-all del Java original, limitado a bcv.org.ve).
  static http.Client _createClient() {
    final HttpClient ioClient = HttpClient()
      ..badCertificateCallback =
          (X509Certificate cert, String host, int port) =>
              host.endsWith('bcv.org.ve');
    return IOClient(ioClient);
  }

  /// Obtiene las tasas del BCV: primero scraping a bcv.org.ve, y si falla
  /// usa DolarApi como respaldo. En caso de error devuelve el caché si existe.
  ///
  /// Si ya se consultó hoy ([isFetchedToday]) y [force] es false, devuelve
  /// los datos del día sin hacer llamada de red — la app solo consulta una
  /// vez al día; el botón ↻ pasa [force] = true para refresco manual.
  Future<BcvResult> obtenerTasas({bool force = false}) async {
    if (!force && await isFetchedToday()) {
      final BcvData? today = await loadFromCache(markAsCache: false);
      if (today != null && _plausible(today.dolar, today.euro)) {
        return BcvResult.success(today);
      }
    }
    try {
      final BcvData data = await _fetchFromWeb();
      await _saveToCache(data);
      return BcvResult.success(data);
    } catch (_) {
      // El scraping falló: intentar con DolarApi.
      try {
        final BcvData data = await _fetchFromDolarApi();
        await _saveToCache(data);
        return BcvResult.success(data);
      } catch (e) {
        final BcvData? cached = await loadFromCache();
        if (cached != null && _plausible(cached.dolar, cached.euro)) {
          return BcvResult.failure('Error de conexión', cached);
        }
        return BcvResult.failure(
            'Error de conexión y sin caché disponible', null);
      }
    }
  }

  /// Scraping de www.bcv.org.ve (Jsoup selectores `#dolar strong`,
  /// `#euro strong` y la fecha valor).
  Future<BcvData> _fetchFromWeb() async {
    final client = _createClient();
    try {
      final http.Response response = await client
          .get(Uri.parse(_kBcvUrl), headers: {'User-Agent': _kUserAgent})
          .timeout(_kTimeout);

      final document = html_parser.parse(response.body);

      final dolarElement = document.querySelector('#dolar strong');
      final euroElement = document.querySelector('#euro strong');
      var fechaElement = document.querySelector('span.date-display-single');
      fechaElement ??= document.querySelector('.pull-right .dinpro');
      fechaElement ??= document.querySelector('.fecha-valor span');

      final String dolarStr =
          dolarElement != null ? _sanitizePrice(dolarElement.text) : 'N/A';
      final String euroStr =
          euroElement != null ? _sanitizePrice(euroElement.text) : 'N/A';
      final String fechaValor = fechaElement != null
          ? fechaElement.text.replaceAll(RegExp(r'\s+'), ' ').trim()
          : 'No disponible';

      final BcvData data = _buildData(
          dolarStr: dolarStr, euroStr: euroStr, fechaValor: fechaValor);
      if (!_plausible(data.dolar, data.euro)) {
        throw Exception('Selectores BCV sin datos');
      }
      return data;
    } finally {
      client.close();
    }
  }

  /// Fallback con la API de dolarapi.com (promedio oficial USD y EUR).
  Future<BcvData> _fetchFromDolarApi() async {
    final List<http.Response> responses = await Future.wait([
      http
          .get(Uri.parse(_kDolarApiUsd), headers: {'User-Agent': _kUserAgent})
          .timeout(_kTimeout),
      http
          .get(Uri.parse(_kDolarApiEur), headers: {'User-Agent': _kUserAgent})
          .timeout(_kTimeout),
    ]);

    final double dolar = _parseDolarApiPrice(responses[0].body);
    final double euro = _parseDolarApiPrice(responses[1].body);
    if (!_plausible(dolar, euro)) {
      throw Exception('DolarApi devolvió datos incompletos');
    }

    final String fechaValor =
        _parseDolarApiDate(responses[0].body) ?? 'No disponible';

    return _buildData(
      dolarStr: dolar.toString(),
      euroStr: euro.toString(),
      fechaValor: fechaValor,
    );
  }

  /// `promedio` de DolarApi. Ojo: la API emite los miles con punto como
  /// número JSON (ej. euro "1.006" = 1006), por eso valores < 10 se
  /// interpretan como miles.
  static double _parseDolarApiPrice(String body) {
    try {
      final decoded = jsonDecode(body) as Map<String, dynamic>;
      double promedio = (decoded['promedio'] as num).toDouble();
      if (promedio > 0 && promedio < 10) promedio *= 1000;
      return promedio;
    } catch (_) {
      return 0;
    }
  }

  static String? _parseDolarApiDate(String body) {
    try {
      final decoded = jsonDecode(body) as Map<String, dynamic>;
      final String? raw = decoded['fechaActualizacion'] as String?;
      if (raw == null) return null;
      final DateTime dt = DateTime.parse(raw);
      return '${_two(dt.day)}/${_two(dt.month)}/${dt.year}';
    } catch (_) {
      return null;
    }
  }

  static BcvData _buildData({
    required String dolarStr,
    required String euroStr,
    required String fechaValor,
  }) {
    final now = DateTime.now();
    return BcvData(
      dolarStr: dolarStr,
      euroStr: euroStr,
      dolar: _parsePrice(dolarStr),
      euro: _parsePrice(euroStr),
      fechaValor: fechaValor,
      lastUpdate:
          '${_two(now.day)}/${_two(now.month)}/${now.year} ${_two(now.hour)}:${_two(now.minute)}',
      isCache: false,
      vigenciaMsg: calcularVigencia(weekday: now.weekday, hour: now.hour),
    );
  }

  static String _two(int v) => v.toString().padLeft(2, '0');

  static String _sanitizePrice(String raw) {
    return raw
        .trim()
        .replaceAll(RegExp(r'[\s\u{00A0}\u{200B}]+'), '')
        .replaceAll(RegExp(r'[^0-9,.]'), '');
  }

  static double _parsePrice(String priceStr) {
    if (priceStr.isEmpty || priceStr == 'N/A') return 0;
    // El BCV puede usar punto de miles y coma decimal ("1.006,03").
    try {
      return parseFormattedNumber(priceStr);
    } on FormatException {
      return 0;
    }
  }

  static Future<void> _saveToCache(BcvData data) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_kKeyDolar, data.dolarStr);
    await prefs.setString(_kKeyEuro, data.euroStr);
    await prefs.setString(_kKeyFechaValor, data.fechaValor);
    await prefs.setString(_kKeyLastUpdate, data.lastUpdate);
    await prefs.setString(_kKeyFetchDate, _today());
    await prefs.setInt(_kKeyCacheVersion, _kCurrentCacheVersion);
  }

  static String _today() {
    final n = DateTime.now();
    return '${n.year}-${_two(n.month)}-${_two(n.day)}';
  }

  /// Verificación de cordura: el euro nunca debería estar muy lejos del
  /// dólar (EUR/USD ~1.0-1.2). Valores como 16.36 con dólar a 894 son
  /// caché corrupto o parseo roto.
  static bool _plausible(double dolar, double euro) =>
      dolar > 0 && euro >= dolar * 0.8 && euro <= dolar * 2.5;

  /// true si ya se hizo una consulta exitosa el día de hoy con la versión
  /// de caché actual (invalida cachés de builds anteriores).
  static Future<bool> isFetchedToday() async {
    final prefs = await SharedPreferences.getInstance();
    if (prefs.getInt(_kKeyCacheVersion) != _kCurrentCacheVersion) return false;
    return prefs.getString(_kKeyFetchDate) == _today();
  }

  /// Carga las tasas desde caché, o null si no hay.
  /// [markAsCache] controla el flag `isCache` (false cuando el dato es del
  /// fetch del día y no un respaldo por error).
  static Future<BcvData?> loadFromCache({bool markAsCache = true}) async {
    final prefs = await SharedPreferences.getInstance();
    final String? dolar = prefs.getString(_kKeyDolar);
    if (dolar == null) return null;

    final now = DateTime.now();
    return BcvData(
      dolarStr: dolar,
      dolar: _parsePrice(dolar),
      euroStr: prefs.getString(_kKeyEuro) ?? 'N/A',
      euro: _parsePrice(prefs.getString(_kKeyEuro) ?? 'N/A'),
      fechaValor: prefs.getString(_kKeyFechaValor) ?? 'No disponible',
      lastUpdate: prefs.getString(_kKeyLastUpdate) ?? 'Desconocido',
      isCache: markAsCache,
      vigenciaMsg: calcularVigencia(weekday: now.weekday, hour: now.hour),
    );
  }
}
