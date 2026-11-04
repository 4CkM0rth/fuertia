import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { delay, http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { api } from '../api/cliente'
import { contenidoDelAlmacenamiento, renderizarApp } from '../pruebas/renderizar'
import {
  problema,
  RUTA_RENOVACION,
  RUTA_SESION_ACTUAL,
  RUTA_YO,
  servidor,
  sesionDePrueba,
  usuarioDePrueba,
} from '../pruebas/servidor'
import { almacenSesion } from './almacenSesion'
import { arrancarSesion } from './sesion'

/**
 * Backend falso donde el token "viejo" ya venció y la renovación entrega el token "nuevo".
 * Cuenta las renovaciones para comprobar que solo hay una.
 */
function backendConTokenVencido() {
  const conteo = { renovaciones: 0, llamadasYo: 0 }
  const encabezadosRenovacion: { csrf: string | null; credenciales: RequestCredentials }[] = []
  servidor.use(
    http.post(RUTA_RENOVACION, async ({ request }) => {
      conteo.renovaciones++
      encabezadosRenovacion.push({ csrf: request.headers.get('X-Fuertia-Csrf'), credenciales: request.credentials })
      await delay(20)
      return HttpResponse.json(sesionDePrueba('token-nuevo'))
    }),
    http.get(RUTA_YO, ({ request }) => {
      conteo.llamadasYo++
      return request.headers.get('Authorization') === 'Bearer token-nuevo'
        ? HttpResponse.json(usuarioDePrueba)
        : problema(401, 'El token venció')
    }),
  )
  almacenSesion.iniciar(sesionDePrueba('token-viejo'))
  return { conteo, encabezadosRenovacion }
}

describe('Sesión', () => {
  it('ante un 401 renueva una sola vez y repite la llamada', async () => {
    const { conteo, encabezadosRenovacion } = backendConTokenVencido()

    const { data } = await api.GET('/api/v1/identidad/yo')

    expect(data).toEqual(usuarioDePrueba)
    expect(conteo).toEqual({ renovaciones: 1, llamadasYo: 2 })
    // La renovación del canal WEB va con la cookie y con el encabezado contra CSRF.
    expect(encabezadosRenovacion).toEqual([{ csrf: '1', credenciales: 'include' }])
    expect(almacenSesion.tokenDeAcceso()).toBe('token-nuevo')
    expect(contenidoDelAlmacenamiento()).not.toContain('token-nuevo')
  })

  it('con dos llamadas a la vez que reciben 401 hace una sola renovación', async () => {
    const { conteo } = backendConTokenVencido()

    const [primera, segunda] = await Promise.all([
      api.GET('/api/v1/identidad/yo'),
      api.GET('/api/v1/identidad/yo'),
    ])

    expect(primera.data).toEqual(usuarioDePrueba)
    expect(segunda.data).toEqual(usuarioDePrueba)
    expect(conteo.renovaciones).toBe(1)
  })

  it('si la renovación falla, cierra la sesión y manda a /ingresar', async () => {
    servidor.use(http.get(RUTA_YO, () => problema(401, 'El token venció')))
    // La renovación por defecto responde 401 (no hay cookie válida).
    const { enrutador } = renderizarApp('/', { sesion: 'activa' })

    expect(await screen.findByRole('heading', { name: 'Entrar' })).toBeInTheDocument()
    expect(enrutador.state.location.pathname).toBe('/ingresar')
    expect(almacenSesion.obtener()).toEqual({ tipo: 'sin-sesion' })
  })

  it('al arrancar recupera la sesión con la cookie', async () => {
    servidor.use(http.post(RUTA_RENOVACION, () => HttpResponse.json(sesionDePrueba('token-recuperado'))))

    expect(almacenSesion.obtener()).toEqual({ tipo: 'cargando' })
    await expect(arrancarSesion()).resolves.toBe(true)

    expect(almacenSesion.tokenDeAcceso()).toBe('token-recuperado')
  })

  it('al arrancar sin cookie queda sin sesión', async () => {
    await expect(arrancarSesion()).resolves.toBe(false)
    expect(almacenSesion.obtener()).toEqual({ tipo: 'sin-sesion' })
  })

  it('cerrar sesión llama al DELETE y vuelve a /ingresar', async () => {
    let autorizacion: string | null = null
    servidor.use(
      http.delete(RUTA_SESION_ACTUAL, ({ request }) => {
        autorizacion = request.headers.get('Authorization')
        return new HttpResponse(null, { status: 204 })
      }),
    )
    const usuario = userEvent.setup()
    const { enrutador } = renderizarApp('/', { sesion: 'activa' })

    await usuario.click(await screen.findByRole('button', { name: 'Cerrar sesión' }))

    expect(await screen.findByRole('heading', { name: 'Entrar' })).toBeInTheDocument()
    expect(enrutador.state.location.pathname).toBe('/ingresar')
    expect(autorizacion).toBe('Bearer token-de-acceso-de-prueba')
    expect(almacenSesion.tokenDeAcceso()).toBeUndefined()
  })
})
