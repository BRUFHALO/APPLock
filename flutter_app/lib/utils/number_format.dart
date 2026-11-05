/// Formato numérico venezolano y lógica de conversión de monedas.
///
/// Replica exacta del comportamiento de CalculatorActivity.java:
/// - Salida: separador de miles con punto, decimales con coma (ej: 10.000,50).
/// - Parseo flexible de entrada: detecta si el punto es miles o decimal.
library;

/// Índices de monedas (igual que CalculatorActivity.CURRENCIES).
const int kUsd = 0;
const int kEur = 1;
const int kVes = 2;
const int kUsdt = 3;

const List<String> kCurrencies = ['\$ USD', '€ EUR', 'Bs VES', '₮ USDT'];

/// Convierte [amount] desde [from] a [to] usando las tasas contra el bolívar.
///
/// Devuelve [double.nan] si las tasas necesarias no están disponibles (igual al
/// `return -1` del original, pero sin colisión con un monto válido).
double convert(double amount, int from, int to,
    {required double tasaDolar,
    required double tasaEuro,
    required double tasaUsdt}) {
  if (from == to) return amount;
  if (tasaDolar <= 0 || tasaEuro <= 0) return double.nan;

  // 1) Convertir a bolívares.
  double amountInVes;
  switch (from) {
    case kUsd:
      amountInVes = amount * tasaDolar;
    case kEur:
      amountInVes = amount * tasaEuro;
    case kUsdt:
      if (tasaUsdt <= 0) return double.nan;
      amountInVes = amount * tasaUsdt;
    case kVes:
    default:
      amountInVes = amount;
  }

  // 2) Convertir de bolívares a la moneda destino.
  switch (to) {
    case kUsd:
      return amountInVes / tasaDolar;
    case kEur:
      return amountInVes / tasaEuro;
    case kUsdt:
      if (tasaUsdt <= 0) return double.nan;
      return amountInVes / tasaUsdt;
    case kVes:
    default:
      return amountInVes;
  }
}

/// Parsea un texto con formato venezolano a [double].
///
/// Reglas (idénticas a CalculatorActivity.parseFormattedNumber):
/// - Si tiene coma: la coma es decimal, los puntos son miles.
///   "10.000,50" -> 10000.5
/// - Si solo tiene punto y coincide con `\d{1,3}(\.\d{3})+`: es separador de miles.
///   "1.500" -> 1500, "10.000" -> 10000
/// - En otro caso, el punto es decimal: "1.5" -> 1.5, "25.99" -> 25.99
/// - Sin separadores: entero o decimal simple.
double parseFormattedNumber(String formatted) {
  final String text = formatted.trim();
  if (text.isEmpty) return 0;

  if (text.contains(',')) {
    // Coma decimal: los puntos son separadores de miles.
    final String normalized = text.replaceAll('.', '').replaceAll(',', '.');
    return double.parse(normalized);
  } else if (text.contains('.')) {
    if (RegExp(r'^\d{1,3}(\.\d{3})+$').hasMatch(text)) {
      // Punto como separador de miles.
      return double.parse(text.replaceAll('.', ''));
    }
    // Punto como separador decimal.
    return double.parse(text);
  } else {
    return double.parse(text);
  }
}

/// Formatea un [double] al estilo venezolano: `10.000,50`.
///
/// - Sin parte fraccionaria: `10000` -> "10.000".
/// - Con parte fraccionaria: `10000.5` -> "10.000,50".
String formatResult(double value) {
  if (value.isNaN) return '';
  if (value == value.floorToDouble()) {
    return formatWithThousandsSeparator(value.toInt());
  }
  int integerPart = value.truncate();
  int decimalPart = ((value - integerPart) * 100).round();
  if (decimalPart >= 100) {
    decimalPart -= 100;
    integerPart += 1;
  }
  return '${formatWithThousandsSeparator(integerPart)},${decimalPart.toString().padLeft(2, '0')}';
}

/// Formatea un entero con separador de miles (punto).
String formatWithThousandsSeparator(int value) {
  final bool negative = value < 0;
  final String str = value.abs().toString();
  final StringBuffer buf = StringBuffer();
  int count = 0;
  for (int i = str.length - 1; i >= 0; i--) {
    if (count > 0 && count % 3 == 0) {
      buf.write('.');
    }
    buf.write(str[i]);
    count++;
  }
  final String reversed = buf.toString().split('').reversed.join();
  return negative ? '-$reversed' : reversed;
}

/// Símbolo de moneda para mostrar en la imagen de pago.
String currencySymbol(String currency) {
  if (currency.contains('USDT')) return '₮';
  if (currency.contains('USD')) return '\$';
  if (currency.contains('EUR')) return '€';
  if (currency.contains('VES')) return 'Bs.';
  return '';
}

/// Código de moneda (USD/EUR/VES/USDT).
String currencyCode(String currency) {
  if (currency.contains('USDT')) return 'USDT';
  if (currency.contains('USD')) return 'USD';
  if (currency.contains('EUR')) return 'EUR';
  if (currency.contains('VES')) return 'VES';
  return '';
}
