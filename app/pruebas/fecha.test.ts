import { fechaLarga } from '@/utilidades/fecha';

describe('fechaLarga', () => {
  test('arma la fecha como en el diseño', () => {
    expect(fechaLarga(new Date(2026, 10, 3))).toBe('Martes 3 de noviembre');
    expect(fechaLarga(new Date(2026, 9, 29))).toBe('Jueves 29 de octubre');
    expect(fechaLarga(new Date(2027, 0, 1))).toBe('Viernes 1 de enero');
  });
});
