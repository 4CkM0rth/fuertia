import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { SelectorTema } from './SelectorTema'
import { CLAVE_TEMA } from './tema'

const html = () => document.documentElement

describe('SelectorTema', () => {
  it('empieza en Sistema si no hay nada guardado', () => {
    render(<SelectorTema />)

    expect(screen.getByRole('button', { name: 'Sistema' })).toHaveAttribute('aria-pressed', 'true')
    expect(html()).toHaveAttribute('data-tema', 'sistema')
  })

  it('cambia data-tema y guarda la elección', async () => {
    const usuario = userEvent.setup()
    render(<SelectorTema />)

    await usuario.click(screen.getByRole('button', { name: 'Oscuro' }))
    expect(html()).toHaveAttribute('data-tema', 'oscuro')
    expect(localStorage.getItem(CLAVE_TEMA)).toBe('oscuro')
    expect(screen.getByRole('button', { name: 'Oscuro' })).toHaveAttribute('aria-pressed', 'true')

    await usuario.click(screen.getByRole('button', { name: 'Claro' }))
    expect(html()).toHaveAttribute('data-tema', 'claro')
    expect(localStorage.getItem(CLAVE_TEMA)).toBe('claro')
  })

  it('recupera la elección guardada', () => {
    localStorage.setItem(CLAVE_TEMA, 'oscuro')
    render(<SelectorTema />)

    expect(screen.getByRole('button', { name: 'Oscuro' })).toHaveAttribute('aria-pressed', 'true')
    expect(html()).toHaveAttribute('data-tema', 'oscuro')
  })
})
