import { http, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'
import type { EstadoSistema } from '../api/cliente'
import type { SesionIniciada, UsuarioActual } from '../sesion/almacenSesion'

// "*" al inicio: coincide con cualquier origen, así la prueba no depende de VITE_API_URL.
export const RUTA_ESTADO = '*/api/v1/sistema/estado'
export const RUTA_REGISTRO = '*/api/v1/identidad/registro'
export const RUTA_VERIFICACION = '*/api/v1/identidad/verificacion'
export const RUTA_SESIONES = '*/api/v1/identidad/sesiones'
export const RUTA_RENOVACION = '*/api/v1/identidad/sesiones/renovacion'
export const RUTA_SESION_ACTUAL = '*/api/v1/identidad/sesiones/actual'
export const RUTA_RECUPERACION = '*/api/v1/identidad/recuperacion'
export const RUTA_CONFIRMACION = '*/api/v1/identidad/recuperacion/confirmacion'
export const RUTA_YO = '*/api/v1/identidad/yo'

// Datos tipados con el esquema del contrato: si el contrato cambia, estas pruebas dejan de compilar.
export const estadoDePrueba: EstadoSistema = { nombre: 'Fuertia', version: '0.1.0', estado: 'ok' }

export const usuarioDePrueba: UsuarioActual = {
  id: '6f1c2c9e-4b7a-4d0e-9a51-0c2d6b7e8f10',
  tenantId: '0b9d6a3e-1f2c-4e5a-8b7c-9d0e1f2a3b4c',
  nombre: 'Sebastian Rojas',
  correo: 'sebastian@correo.com',
  rol: 'ENTRENADOR',
  nombreNegocio: 'Sebastian Rojas Coaching',
}

/** Sesión con un token de acceso reconocible, para buscarlo luego en el almacenamiento. */
export const sesionDePrueba = (token = 'token-de-acceso-de-prueba'): SesionIniciada => ({
  tokenDeAcceso: token,
  venceEnSegundos: 900,
  usuario: usuarioDePrueba,
})

export const problema = (status: number, title: string, extra: Record<string, unknown> = {}) =>
  HttpResponse.json({ title, status, ...extra }, { status, headers: { 'Content-Type': 'application/problem+json' } })

/**
 * Backend falso: MSW intercepta las llamadas fetch y responde lo que digan estos manejadores.
 * Por defecto se comporta como un navegador recién abierto: no hay cookie, así que renovar da 401.
 * Cada prueba cambia lo que necesita con servidor.use(...).
 */
export const servidor = setupServer(
  http.get(RUTA_ESTADO, () => HttpResponse.json(estadoDePrueba)),
  http.post(RUTA_RENOVACION, () => problema(401, 'La sesión no es válida')),
  http.get(RUTA_YO, ({ request }) =>
    request.headers.get('Authorization')?.startsWith('Bearer ')
      ? HttpResponse.json(usuarioDePrueba)
      : problema(401, 'Falta el token'),
  ),
  http.delete(RUTA_SESION_ACTUAL, () => new HttpResponse(null, { status: 204 })),
)
