/// Lógica de mensajes de vigencia de la tasa BCV.
///
/// Replica de BcvScraper.calcularVigencia: según hora y día de la semana,
/// indica si la tasa mostrada es la de ayer, la nueva de mañana, etc.
library;

/// Devuelve el mensaje de vigencia para la fecha/hora dada.
///
/// [weekday]: 1=lunes ... 7=domingo (DateTime.weekday).
/// [hour]: hora del día 0-23.
String calcularVigencia({required int weekday, required int hour}) {
  // Fin de semana (sábado=6, domingo=7).
  if (weekday == DateTime.saturday || weekday == DateTime.sunday) {
    return '📅 Tasa vigente del viernes (fin de semana)';
  }

  // Día hábil antes de las 4PM.
  if (hour < 16) {
    return '📅 Tasa vigente desde ayer';
  }

  // Día hábil entre 4PM y 6PM.
  if (hour >= 16 && hour <= 18) {
    return '🔄 Nueva tasa disponible para mañana';
  }

  // Después de las 6PM.
  return '✅ Tasa actualizada - vigente mañana';
}
