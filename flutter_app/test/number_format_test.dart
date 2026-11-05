import 'package:test/test.dart';
import 'package:dont_look_at_me/utils/number_format.dart';

void main() {
  group('parseFormattedNumber', () {
    test('coma decimal con miles', () {
      expect(parseFormattedNumber('10.000,50'), 10000.5);
    });
    test('coma decimal sin miles', () {
      expect(parseFormattedNumber('1,5'), 1.5);
    });
    test('coma decimal simple', () {
      expect(parseFormattedNumber('25,99'), 25.99);
    });
    test('punto como miles (1.500)', () {
      expect(parseFormattedNumber('1.500'), 1500);
    });
    test('punto como miles (10.000)', () {
      expect(parseFormattedNumber('10.000'), 10000);
    });
    test('punto como decimal (1.5)', () {
      expect(parseFormattedNumber('1.5'), 1.5);
    });
    test('punto como decimal (25.99)', () {
      expect(parseFormattedNumber('25.99'), 25.99);
    });
    test('entero simple', () {
      expect(parseFormattedNumber('100'), 100);
    });
    test('miles con coma decimal larga (precio BCV euro)', () {
      expect(parseFormattedNumber('1.006,03263992'), 1006.03263992);
    });
    test('miles con coma decimal (dólar BCV > 1000)', () {
      expect(parseFormattedNumber('1.000,50'), 1000.5);
      expect(parseFormattedNumber('1.015,88760000'), 1015.8876);
    });
    test('vacío devuelve 0', () {
      expect(parseFormattedNumber(''), 0);
    });
  });

  group('formatResult', () {
    test('entero con miles', () {
      expect(formatResult(10000), '10.000');
    });
    test('entero sin miles', () {
      expect(formatResult(100), '100');
    });
    test('con decimales', () {
      expect(formatResult(10000.5), '10.000,50');
    });
    test('con decimales redondeo', () {
      expect(formatResult(10000.509), '10.000,51');
    });
    test('con decimal que acarrea', () {
      expect(formatResult(10000.999), '10.001,00');
    });
    test('NaN devuelve vacío', () {
      expect(formatResult(double.nan), '');
    });
  });

  group('convert', () {
    const double d = 36.71; // USD->VES
    const double e = 39.50; // EUR->VES
    const double u = 40.20; // USDT->VES

    test('misma moneda devuelve el monto', () {
      expect(convert(100, kUsd, kUsd,
          tasaDolar: d, tasaEuro: e, tasaUsdt: u),
          100);
    });
    test('USD -> VES', () {
      expect(convert(1, kUsd, kVes, tasaDolar: d, tasaEuro: e, tasaUsdt: u),
          closeTo(36.71, 0.001));
    });
    test('VES -> USD', () {
      expect(convert(36.71, kVes, kUsd, tasaDolar: d, tasaEuro: e, tasaUsdt: u),
          closeTo(1, 0.001));
    });
    test('EUR -> USD (via VES)', () {
      expect(convert(1, kEur, kUsd, tasaDolar: d, tasaEuro: e, tasaUsdt: u),
          closeTo(e / d, 0.001));
    });
    test('USDT -> VES', () {
      expect(convert(1, kUsdt, kVes, tasaDolar: d, tasaEuro: e, tasaUsdt: u),
          closeTo(40.20, 0.001));
    });
    test('sin tasa USDT devuelve NaN', () {
      expect(convert(1, kUsdt, kVes, tasaDolar: d, tasaEuro: e, tasaUsdt: 0),
          double.nan);
    });
    test('sin tasas BCV devuelve NaN', () {
      expect(convert(1, kUsd, kVes, tasaDolar: 0, tasaEuro: e, tasaUsdt: u),
          double.nan);
    });
  });

  group('currencySymbol / currencyCode', () {
    test('símbolos', () {
      expect(currencySymbol('\$ USD'), '\$');
      expect(currencySymbol('€ EUR'), '€');
      expect(currencySymbol('Bs VES'), 'Bs.');
      expect(currencySymbol('₮ USDT'), '₮');
    });
    test('códigos', () {
      expect(currencyCode('\$ USD'), 'USD');
      expect(currencyCode('€ EUR'), 'EUR');
      expect(currencyCode('Bs VES'), 'VES');
      expect(currencyCode('₮ USDT'), 'USDT');
    });
  });
}
