import { screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { renderizarApp } from '../pruebas/renderizar'
import { problema, RUTA_VERIFICACION, servidor } from '../pruebas/servidor'

describe('Verificar', () => {
  it('con un token válido confirma el correo al abrir, una sola vez', async () => {
    const tokens: unknown[] = []
    servidor.use(
      http.post(RUTA_VERIFICACION, async ({ request }) => {
        tokens.push(await request.json())
        return new HttpResponse(null, { status: 204 })
      }),
    )
    renderizarApp('/verificar?token=abc123')

    expect(screen.getByText('Confirmando tu correo…')).toBeInTheDocument()
    expect(await screen.findByRole('heading', { name: 'Tu correo quedó confirmado' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Entrar' })).toHaveAttribute('href', '/ingresar')
    expect(tokens).toEqual([{ token: 'abc123' }])
  })

  it('con un token inválido o vencido lo dice', async () => {
    servidor.use(
      http.post(RUTA_VERIFICACION, () =>
        problema(400, 'El enlace no es válido o ya venció', { codigo: 'ENLACE_INVALIDO' }),
      ),
    )
    renderizarApp('/verificar?token=usado')

    expect(await screen.findByRole('heading', { name: 'El enlace no es válido o ya venció' })).toBeInTheDocument()
  })

  it('sin token no llama al backend y dice que el enlace no sirve', async () => {
    renderizarApp('/verificar')

    expect(await screen.findByRole('heading', { name: 'El enlace no es válido o ya venció' })).toBeInTheDocument()
  })
})
