import createClient from 'openapi-fetch'
import { almacenSesion } from '../sesion/almacenSesion'
import type { components, paths } from './esquema'

// esquema.d.ts lo genera "npm run generar:api" desde docs/api/openapi.yaml. No se edita a mano:
// si el contrato cambia, se vuelve a generar y TypeScript avisa dónde se rompió la web.

export type EstadoSistema = components['schemas']['EstadoSistema']
export type Problema = components['schemas']['Problema']

// VITE_API_URL vacía (lo normal en desarrollo) significa "el mismo origen de la página":
// las llamadas van a /api y el proxy de Vite las manda al backend en el puerto 8080.
const urlBase = import.meta.env.VITE_API_URL || globalThis.location?.origin

const RUTA_SESIONES = '/api/v1/identidad/sesiones'
const RUTA_RENOVACION = '/api/v1/identidad/sesiones/renovacion'

// Rutas que no llevan token de acceso (security: [] en el contrato). Un 401 en ellas no se
// renueva: en el inicio de sesión, por ejemplo, un 401 significa "contraseña incorrecta".
const RUTAS_PUBLICAS = new Set([
  '/api/v1/sistema/estado',
  '/api/v1/identidad/registro',
  '/api/v1/identidad/verificacion',
  RUTA_SESIONES,
  RUTA_RENOVACION,
  '/api/v1/identidad/recuperacion',
  '/api/v1/identidad/recuperacion/confirmacion',
])

/** Copia la solicitud agregando el token y, en las rutas de sesiones, la cookie. */
function preparar(solicitud: Request, token: string | undefined, conCookie: boolean): Request {
  const encabezados = new Headers(solicitud.headers)
  if (token) encabezados.set('Authorization', `Bearer ${token}`)
  return new Request(solicitud, {
    headers: encabezados,
    // La cookie del token de renovación tiene Path=/api/v1/identidad/sesiones, así que el navegador
    // solo la manda ahí. 'include' hace que también viaje si la API está en otro origen (VITE_API_URL).
    credentials: conCookie ? 'include' : solicitud.credentials,
  })
}

/**
 * Todas las llamadas de la web pasan por aquí. Si una llamada con token responde 401
 * (el token de acceso venció, dura 15 minutos), renueva la sesión una vez y la repite.
 */
async function fetchFuertia(solicitud: Request): Promise<Response> {
  const ruta = new URL(solicitud.url).pathname
  const publica = RUTAS_PUBLICAS.has(ruta)
  const conCookie = ruta.startsWith(RUTA_SESIONES)

  // Se copia antes de enviarla: el cuerpo de una solicitud solo se puede leer una vez
  // y necesitamos otra copia intacta por si hay que repetirla.
  const copia = publica ? undefined : solicitud.clone()

  // Se usa la fetch vigente en cada llamada (y no la del momento de crear el cliente)
  // para que MSW pueda interceptarla en las pruebas.
  const respuesta = await globalThis.fetch(
    preparar(solicitud, publica ? undefined : almacenSesion.tokenDeAcceso(), conCookie),
  )
  if (respuesta.status !== 401 || !copia) return respuesta

  const renovada = await renovarSesion()
  if (!renovada) return respuesta // renovarSesion ya cerró la sesión: la web manda a /ingresar.
  return globalThis.fetch(preparar(copia, almacenSesion.tokenDeAcceso(), conCookie))
}

export const api = createClient<paths>({ baseUrl: urlBase, fetch: fetchFuertia })

/*
 * Una sola renovación a la vez. Si cinco llamadas reciben 401 al mismo tiempo, las cinco esperan
 * esta misma promesa en lugar de pedir cinco renovaciones. Además es obligatorio: el token de
 * renovación se usa una sola vez, y si se mandara dos veces el backend lo tomaría como robo
 * y cerraría toda la sesión.
 */
let renovacionEnCurso: Promise<boolean> | null = null

/** Pide un token de acceso nuevo con la cookie. Devuelve true si la sesión sigue activa. */
export function renovarSesion(): Promise<boolean> {
  renovacionEnCurso ??= (async () => {
    try {
      const { data } = await api.POST(RUTA_RENOVACION, {
        // Encabezado que exige el contrato para el canal WEB: un sitio ajeno no lo puede poner (CSRF).
        params: { header: { 'X-Fuertia-Csrf': '1' } },
      })
      if (data) {
        almacenSesion.iniciar(data)
        return true
      }
    } catch {
      // Sin red: se trata igual que una renovación fallida.
    }
    almacenSesion.terminar()
    return false
  })().finally(() => {
    renovacionEnCurso = null
  })
  return renovacionEnCurso
}
