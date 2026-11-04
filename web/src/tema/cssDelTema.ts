import { medidas, temas, tipografia, variablesCss } from '@fuertia/tokens'

/**
 * Medidas, radios y tamaños de letra como variables CSS. No cambian con el tema, por eso van
 * una sola vez en :root. Siguen la misma convención que los colores: --fu-radio-medio, --fu-espacio-l.
 * (nombreVariableCss solo acepta nombres de color, por eso aquí se arman a mano con el mismo formato).
 */
function variablesDeMedidas(): string {
  const lineas = [`--fu-tactil-minimo: ${medidas.tactilMinimo}px;`]
  for (const [nombre, valor] of Object.entries(medidas.radios)) lineas.push(`--fu-radio-${nombre}: ${valor}px;`)
  for (const [nombre, valor] of Object.entries(medidas.espacios)) lineas.push(`--fu-espacio-${nombre}: ${valor}px;`)
  for (const [nombre, valor] of Object.entries(tipografia.tamanos)) lineas.push(`--fu-tamano-${nombre}: ${valor}px;`)
  return lineas.join('\n')
}

const sangrar = (bloque: string, espacios: number) =>
  bloque
    .split('\n')
    .map((linea) => ' '.repeat(espacios) + linea)
    .join('\n')

/**
 * CSS con las variables de color de los dos temas.
 * Orden de prioridad:
 * 1. Por defecto, claro.
 * 2. Si el sistema está en oscuro y la persona no eligió "claro", oscuro.
 * 3. Si la persona eligió "oscuro" (data-tema="oscuro" en <html>), oscuro siempre.
 * data-tema="sistema" no tiene regla propia: deja que mande la media query.
 */
export function generarCssTema(): string {
  const claro = sangrar(variablesCss(temas.claro), 2)
  const oscuroEnMedia = sangrar(variablesCss(temas.oscuro), 4)
  const oscuro = sangrar(variablesCss(temas.oscuro), 2)

  return `/* Generado desde @fuertia/tokens por web/src/tema/cssDelTema.ts. No lo edites a mano. */
:root {
  color-scheme: light;
  --fu-fuente: ${tipografia.familia}, system-ui, sans-serif;
${sangrar(variablesDeMedidas(), 2)}
${claro}
}

@media (prefers-color-scheme: dark) {
  :root:not([data-tema='claro']) {
    color-scheme: dark;
${oscuroEnMedia}
  }
}

:root[data-tema='oscuro'] {
  color-scheme: dark;
${oscuro}
}
`
}
