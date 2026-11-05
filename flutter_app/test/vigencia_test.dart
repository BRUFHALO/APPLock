import 'package:test/test.dart';
import 'package:dont_look_at_me/utils/vigencia.dart';

void main() {
  group('calcularVigencia', () {
    test('fin de semana sábado', () {
      expect(calcularVigencia(weekday: DateTime.saturday, hour: 12),
          '📅 Tasa vigente del viernes (fin de semana)');
    });
    test('fin de semana domingo', () {
      expect(calcularVigencia(weekday: DateTime.sunday, hour: 20),
          '📅 Tasa vigente del viernes (fin de semana)');
    });
    test('día hábil antes de las 4PM', () {
      expect(calcularVigencia(weekday: DateTime.tuesday, hour: 10),
          '📅 Tasa vigente desde ayer');
    });
    test('día hábil entre 4PM y 6PM', () {
      expect(calcularVigencia(weekday: DateTime.wednesday, hour: 17),
          '🔄 Nueva tasa disponible para mañana');
    });
    test('día hábil después de las 6PM', () {
      expect(calcularVigencia(weekday: DateTime.thursday, hour: 20),
          '✅ Tasa actualizada - vigente mañana');
    });
  });
}
