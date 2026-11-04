import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { renderizarApp } from '../pruebas/renderizar'
import { problema, RUTA_REGISTRO, servidor } from '../pruebas/servidor'

const MENSAJE_202 = 'Te enviamos un correo. Ábrelo para continuar.'

async function llenar(usuario: ReturnType<typeof userEvent.setup>, contrasena = 'una-clave-segura') {
  await usuario.type(screen.getByLabelText('Nombre del negocio'), 'Sebastian Rojas Coaching')
  await usuario.type(screen.getByLabelText('Tu nombre'), 'Sebastian Rojas')
  await usuario.type(screen.getByLabelText('Correo'), 'sebastian@correo.com')
  await usuario.type(screen.getByLabelText('Contraseña'), contrasena)
  await usuario.click(screen.getByRole('button', { name: 'Crear cuenta' }))
}

describe('Registro', () => {
  it('muestra "Revisa tu correo" con el mensaje del 202', async () => {
    let cuerpo: unknown
    servidor.use(
      http.post(RUTA_REGISTRO, async ({ request }) => {
        cuerpo = await request.json()
        return HttpResponse.json({ mensaje: MENSAJE_202 }, { status: 202 })
      }),
    )
    const usuario = userEvent.setup()
    renderizarApp('/registro')

    await llenar(usuario)

    const titulo = await screen.findByRole('heading', { name: 'Revisa tu correo' })
    expect(titulo).toHaveFocus()
    expect(screen.getByText(MENSAJE_202)).toBeInTheDocument()
    expect(cuerpo).toEqual({
      nombreNegocio: 'Sebastian Rojas Coaching',
      nombre: 'Sebastian Rojas',
      correo: 'sebastian@correo.com',
      contrasena: 'una-clave-segura',
    })
  })

  it('exige 10 caracteres en la contraseña antes de enviar', async () => {
    const usuario = userEvent.setup()
    renderizarApp('/registro')

    await llenar(usuario, 'corta')

    const campo = screen.getByLabelText('Contraseña')
    expect(await screen.findByText('Usa 10 caracteres o más')).toBeInTheDocument()
    // La descripción junta la ayuda fija y el error.
    expect(campo).toHaveAccessibleDescription('Usa 10 caracteres o más. Usa 10 caracteres o más')
  })

  it('pone los errores del 400 junto a cada campo', async () => {
    servidor.use(
      http.post(RUTA_REGISTRO, () =>
        problema(400, 'Datos inválidos', { errores: { nombreNegocio: 'Ese nombre es demasiado corto' } }),
      ),
    )
    const usuario = userEvent.setup()
    renderizarApp('/registro')

    await llenar(usuario)

    expect(await screen.findByText('Ese nombre es demasiado corto')).toBeInTheDocument()
    expect(screen.getByLabelText('Nombre del negocio')).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByRole('alert')).toHaveTextContent('Revisa los datos marcados.')
  })
})
