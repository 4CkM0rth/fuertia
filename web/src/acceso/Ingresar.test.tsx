import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { delay, http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { contenidoDelAlmacenamiento, renderizarApp } from '../pruebas/renderizar'
import { problema, RUTA_SESIONES, servidor, sesionDePrueba } from '../pruebas/servidor'

/** Llena el formulario de ingreso y lo envía. */
async function entrar(usuario: ReturnType<typeof userEvent.setup>, contrasena = 'una-clave-segura') {
  await usuario.type(screen.getByLabelText('Correo'), 'sebastian@correo.com')
  await usuario.type(screen.getByLabelText('Contraseña'), contrasena)
  await usuario.click(screen.getByRole('button', { name: 'Entrar' }))
}

describe('Ingresar', () => {
  it('sin sesión, / manda a /ingresar', async () => {
    const { enrutador } = renderizarApp('/')

    expect(await screen.findByRole('heading', { name: 'Entrar' })).toBeInTheDocument()
    expect(enrutador.state.location.pathname).toBe('/ingresar')
    // Decisión de la Bitácora: no hay credenciales de Google todavía.
    expect(screen.queryByRole('button', { name: /google/i })).not.toBeInTheDocument()
  })

  it('con la clave correcta lleva al panel y el token no queda en el almacenamiento', async () => {
    let cuerpo: unknown
    let credenciales: RequestCredentials | undefined
    servidor.use(
      http.post(RUTA_SESIONES, async ({ request }) => {
        cuerpo = await request.json()
        credenciales = request.credentials
        return HttpResponse.json(sesionDePrueba('token-secreto-123'))
      }),
    )
    const usuario = userEvent.setup()
    const { enrutador } = renderizarApp('/ingresar')

    await entrar(usuario)

    expect(await screen.findByRole('heading', { name: 'Hola, Sebastian Rojas' })).toBeInTheDocument()
    expect(screen.getByText('Sebastian Rojas Coaching')).toBeInTheDocument()
    expect(enrutador.state.location.pathname).toBe('/')
    expect(cuerpo).toEqual({ correo: 'sebastian@correo.com', contrasena: 'una-clave-segura', canal: 'WEB' })
    expect(credenciales).toBe('include')
    expect(contenidoDelAlmacenamiento()).not.toContain('token-secreto-123')
  })

  it('muestra "Entrando…" mientras espera y no envía dos veces', async () => {
    let llamadas = 0
    servidor.use(
      http.post(RUTA_SESIONES, async () => {
        llamadas++
        await delay(100)
        return HttpResponse.json(sesionDePrueba())
      }),
    )
    const usuario = userEvent.setup()
    renderizarApp('/ingresar')

    await entrar(usuario)
    const boton = screen.getByRole('button', { name: 'Entrando…' })
    expect(boton).toBeDisabled()
    await usuario.click(boton)
    await usuario.keyboard('{Enter}')

    expect(await screen.findByRole('heading', { name: 'Hola, Sebastian Rojas' })).toBeInTheDocument()
    expect(llamadas).toBe(1)
  })

  it('con la clave mala muestra el mensaje del 401', async () => {
    servidor.use(http.post(RUTA_SESIONES, () => problema(401, 'Correo o contraseña incorrectos')))
    const usuario = userEvent.setup()
    renderizarApp('/ingresar')

    await entrar(usuario, 'clave-equivocada')

    expect(await screen.findByRole('alert')).toHaveTextContent('Correo o contraseña incorrectos')
    expect(screen.getByRole('button', { name: 'Entrar' })).toBeEnabled()
  })

  it('con el correo sin verificar muestra el 403 y la ayuda', async () => {
    servidor.use(
      http.post(RUTA_SESIONES, () =>
        problema(403, 'Confirma tu correo para entrar', { codigo: 'CORREO_SIN_VERIFICAR' }),
      ),
    )
    const usuario = userEvent.setup()
    renderizarApp('/ingresar')

    await entrar(usuario)

    const aviso = await screen.findByRole('alert')
    expect(aviso).toHaveTextContent('Confirma tu correo para entrar')
    expect(aviso).toHaveTextContent(/revisa tu bandeja de entrada/i)
  })

  it('con demasiados intentos muestra el 429', async () => {
    servidor.use(http.post(RUTA_SESIONES, () => problema(429, 'Demasiados intentos')))
    const usuario = userEvent.setup()
    renderizarApp('/ingresar')

    await entrar(usuario)

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Hiciste demasiados intentos. Espera un momento y vuelve a probar.',
    )
  })

  it('pone los errores del 400 junto a cada campo', async () => {
    servidor.use(
      http.post(RUTA_SESIONES, () =>
        problema(400, 'Datos inválidos', { errores: { correo: 'Este correo no es válido' } }),
      ),
    )
    const usuario = userEvent.setup()
    renderizarApp('/ingresar')

    await entrar(usuario)

    const campo = screen.getByLabelText('Correo')
    await waitFor(() => expect(campo).toHaveAccessibleDescription('Este correo no es válido'))
    expect(campo).toHaveAttribute('aria-invalid', 'true')
  })

  it('valida antes de enviar y asocia el error al campo', async () => {
    const usuario = userEvent.setup()
    renderizarApp('/ingresar')

    await usuario.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(await screen.findByText('Escribe tu correo')).toBeInTheDocument()
    expect(screen.getByLabelText('Correo')).toHaveAccessibleDescription('Escribe tu correo')
    expect(screen.getByLabelText('Contraseña')).toHaveAccessibleDescription('Escribe tu contraseña')
  })

  it('muestra y oculta la contraseña con un botón accesible', async () => {
    const usuario = userEvent.setup()
    renderizarApp('/ingresar')
    const campo = screen.getByLabelText('Contraseña')
    const boton = screen.getByRole('button', { name: 'Mostrar contraseña' })

    expect(campo).toHaveAttribute('type', 'password')
    await usuario.click(boton)
    expect(campo).toHaveAttribute('type', 'text')
    expect(boton).toHaveAttribute('aria-pressed', 'true')
    await usuario.click(boton)
    expect(campo).toHaveAttribute('type', 'password')
  })
})
