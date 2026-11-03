import { test } from 'node:test';
import assert from 'node:assert/strict';
import { claro, oscuro, contraste, nombreVariableCss, tipografia, medidas, type Paleta } from '../src/index.ts';

// Parejas texto/fondo que aparecen en las pantallas. Todas deben pasar AA (4.5).
const parejas: Array<[keyof Paleta, keyof Paleta]> = [
  ['texto', 'fondo'],
  ['texto', 'tarjeta'],
  ['textoSecundario', 'fondo'],
  ['textoSecundario', 'tarjeta'],
  ['textoSobrePrimario', 'primario'],
  ['enlace', 'tarjeta'],
  ['heroeTexto', 'heroe'],
  ['heroeTextoSecundario', 'heroe'],
  ['textoSobreDoradoSuave', 'doradoSuave'],
  ['menuTexto', 'menuFondo'],
  ['exito', 'exitoSuave'],
  ['aviso', 'avisoSuave'],
  ['error', 'errorSuave'],
];

for (const [nombre, paleta] of [['claro', claro], ['oscuro', oscuro]] as const) {
  for (const [texto, fondo] of parejas) {
    test(`${nombre}: ${texto} sobre ${fondo} cumple contraste AA`, () => {
      const valor = contraste(paleta[texto], paleta[fondo]);
      assert.ok(valor >= 4.5, `contraste ${valor.toFixed(2)} menor a 4.5`);
    });
  }
}

test('claro y oscuro tienen exactamente los mismos tokens', () => {
  assert.deepEqual(Object.keys(oscuro).sort(), Object.keys(claro).sort());
});

test('todos los valores son colores hex de 6 dígitos', () => {
  for (const valor of [...Object.values(claro), ...Object.values(oscuro)]) {
    assert.match(valor, /^#[0-9A-F]{6}$/);
  }
});

test('el modo oscuro no tiene tinte rojizo en fondos y tarjetas', () => {
  for (const token of ['fondo', 'tarjeta', 'elevado', 'heroe'] as const) {
    const [r, g, b] = [1, 3, 5].map((i) => parseInt(oscuro[token].slice(i, i + 2), 16));
    assert.ok(r - Math.min(g, b) <= 4, `${token} tiene tinte rojizo: ${oscuro[token]}`);
  }
});

test('reglas de accesibilidad: texto de 12 px o más y objetivos táctiles de 44 px', () => {
  assert.ok(Math.min(...Object.values(tipografia.tamanos)) >= 12);
  assert.equal(medidas.tactilMinimo, 44);
});

test('nombre de variable CSS', () => {
  assert.equal(nombreVariableCss('textoSecundario'), '--fu-texto-secundario');
});
