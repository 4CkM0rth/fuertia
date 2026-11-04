import { claro, medidas, oscuro } from '@fuertia/tokens'
import { describe, expect, it } from 'vitest'
import { generarCssTema } from './cssDelTema'

describe('generarCssTema', () => {
  const css = generarCssTema()

  it('pone los colores claros en :root', () => {
    const bloqueRaiz = css.slice(css.indexOf(':root {'), css.indexOf('@media'))
    expect(bloqueRaiz).toContain(`--fu-fondo: ${claro.fondo};`)
    expect(bloqueRaiz).toContain(`--fu-texto-secundario: ${claro.textoSecundario};`)
  })

  it('pone las medidas en :root con el mismo prefijo', () => {
    const bloqueRaiz = css.slice(css.indexOf(':root {'), css.indexOf('@media'))
    expect(bloqueRaiz).toContain(`--fu-tactil-minimo: ${medidas.tactilMinimo}px;`)
    expect(bloqueRaiz).toContain(`--fu-radio-medio: ${medidas.radios.medio}px;`)
    expect(bloqueRaiz).toContain(`--fu-espacio-lxl: ${medidas.espacios.lxl}px;`)
    expect(bloqueRaiz).toContain('--fu-tamano-titulo: 28px;')
  })

  it('usa oscuro con el sistema en oscuro, salvo que la persona elija claro', () => {
    expect(css).toMatch(/@media \(prefers-color-scheme: dark\) \{\s*:root:not\(\[data-tema='claro'\]\)/)
  })

  it('fuerza oscuro con data-tema="oscuro"', () => {
    const bloqueOscuro = css.slice(css.indexOf(":root[data-tema='oscuro']"))
    expect(bloqueOscuro).toContain(`--fu-fondo: ${oscuro.fondo};`)
    expect(bloqueOscuro).toContain('color-scheme: dark;')
  })
})
