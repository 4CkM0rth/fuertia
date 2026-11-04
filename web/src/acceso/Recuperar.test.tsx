import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { renderizarApp } from '../pruebas/renderizar'
import { problema, RUTA_CONFIRMACION, RUTA_RECUPERACION, servidor } from '../pruebas/servidor'

const MENSAJE_202 = 'Si hay una cuenta con ese correo, te enviamos un enlace para cambiar la contraseña.'

describe('Recuperar', () => {
  it('pide el correo y muestra "Revisa tu correo" con el mensaje del 202', async () => {
    let cuerpo: unknown
    servidor.use(
      http.post(RUTA_RECUPERACION, async ({ request }) => {
        cuerpo = await request.json()
        return HttpResponse.json({ mensaje: MENSAJE_202 }, { status: 202 })
      }),
    )
    const usuario = userEvent.setup()
    renderizarApp('/recuperar')

    await usuario.type(screen.getByLabelText('Correo'), 'sebastian@correo.com')
    await usuario.click(screen.getByRole('button', { name: 'Enviar enlace' }))

    expect(await screen.findByRole('heading', { name: 'Revisa tu correo' })).toBeInTheDocument()
    expect(screen.getByText(MENSAJE_202)).toBeInTheDocument()
    expect(cuerpo).toEqual({ correo: 'sebastian@correo.com' })
  })

  it('con el token del enlace cambia la contraseña', async () => {
    let cuerpo: unknown
    servidor.use(
      http.post(RUTA_CONFIRMACION, async ({ request }) => {
        cuerpo = await request.json()
        return new HttpResponse(null, { status: 204 })
      }),
    )
    const usuario = userEvent.setup()
    renderizarApp('/recuperar?token=xyz789')

    await usuario.type(screen.getByLabelText('Contraseña nueva'), 'otra-clave-segura')
    await usuario.click(screen.getByRole('button', { name: 'Guardar contraseña' }))

    expect(await screen.findByRole('heading', { name: 'Tu contraseña cambió' })).toBeInTheDocument()
    expect(cuerpo).toEqual({ token: 'xyz789', contrasenaNueva: 'otra-clave-segura' })
  })

  it('con un token vencido dice que el enlace no sirve y ofrece pedir otro', async () => {
    servidor.use(
      http.post(RUTA_CONFIRMACION, () =>
        problema(400, 'El enlace no es válido o ya venció', { codigo: 'ENLACE_INVALIDO' }),
      ),
    )
    const usuario = userEvent.setup()
    renderizarApp('/recuperar?token=vencido')

    await usuario.type(screen.getByLabelText('Contraseña nueva'), 'otra-clave-segura')
    await usuario.click(screen.getByRole('button', { name: 'Guardar contraseña' }))

    expect(await screen.findByRole('heading', { name: 'El enlace no es válido o ya venció' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Pedir otro enlace' })).toHaveAttribute('href', '/recuperar')
  })
})
