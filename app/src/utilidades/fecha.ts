const DIAS = ['Domingo', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'];
const MESES = [
  'enero',
  'febrero',
  'marzo',
  'abril',
  'mayo',
  'junio',
  'julio',
  'agosto',
  'septiembre',
  'octubre',
  'noviembre',
  'diciembre',
];

/**
 * "Martes 3 de noviembre", como en la pantalla Hoy.
 * Se arma a mano en vez de usar Intl.DateTimeFormat('es-CO') porque el soporte de idiomas de
 * Intl cambia entre Hermes en Android, Hermes en iOS y Node (pruebas), y el resultado
 * ("martes, 3 de noviembre") no trae la mayúscula ni el formato del diseño.
 */
export function fechaLarga(fecha: Date): string {
  return `${DIAS[fecha.getDay()]} ${fecha.getDate()} de ${MESES[fecha.getMonth()]}`;
}
