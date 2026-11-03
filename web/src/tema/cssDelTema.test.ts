import { claro, oscuro } from '@fuertia/tokens'
import { describe, expect, it } from 'vitest'
import { generarCssTema } from './cssDelTema'

describe('generarCssTema', () => {
  const css = generarCssTema()

  it('pone los colores claros en :root', () => {
    const bloqueRaiz = css.slice(css.indexOf(':root {'), css.indexOf('@media'))
    expect(bloqueRaiz).toContain(`--fu-fondo: ${claro.fondo};`)
    expect(bloqueRaiz).toContain(`--fu-texto-secundario: ${claro.textoSecundario};`)
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
