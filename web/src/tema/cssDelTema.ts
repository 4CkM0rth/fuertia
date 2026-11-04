import { temas, tipografia, variablesCss } from '@fuertia/tokens'

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
