import { api, type Problema } from '../api/cliente'
import { almacenSesion, type SesionIniciada } from '../sesion/almacenSesion'

export const MENSAJES = {
  credenciales: 'Correo o contraseña incorrectos',
  sinVerificar: 'Confirma tu correo para entrar',
  sinVerificarAyuda:
    'Te enviamos un enlace cuando creaste la cuenta. Revisa tu bandeja de entrada y la carpeta de spam.',
  demasiadosIntentos: 'Hiciste demasiados intentos. Espera un momento y vuelve a probar.',
  enlaceInvalido: 'El enlace no es válido o ya venció',
  sinConexion: 'No pudimos conectar con Fuertia. Revisa tu conexión y vuelve a probar.',
  datosInvalidos: 'Revisa los datos marcados.',
} as const

/**
 * Resultado de una acción de acceso. Los errores se devuelven como datos (no se lanzan) para que
 * cada pantalla decida qué mostrar sin bloques try/catch.
 */
export type Resultado<T = undefined> =
  | { ok: true; datos: T }
  | {
      ok: false
      mensaje: string
      ayuda?: string
      /** Errores por campo del 400, con el nombre del campo del contrato. */
      errores?: Record<string, string>
      /** El enlace del correo no sirve (400 ENLACE_INVALIDO). */
      enlaceInvalido?: boolean
    }

type Llamada<T> = () => Promise<{ data?: T; error?: unknown; response: Response }>

/** Hace la llamada y traduce los errores comunes a todas las pantallas de acceso. */
async function ejecutar<T>(llamada: Llamada<T>, propios?: (estado: number, problema?: Problema) => Resultado<T> | undefined): Promise<Resultado<T>> {
  let respuesta: Awaited<ReturnType<Llamada<T>>>
  try {
    respuesta = await llamada()
  } catch {
    return { ok: false, mensaje: MENSAJES.sinConexion }
  }
  const { data, error, response } = respuesta
  // Las respuestas 202 y 204 son éxito aunque no traigan cuerpo (204 llega sin data).
  if (response.ok) return { ok: true, datos: data as T }

  const problema = typeof error === 'object' && error !== null ? (error as Problema) : undefined
  const propio = propios?.(response.status, problema)
  if (propio) return propio

  if (response.status === 429) return { ok: false, mensaje: MENSAJES.demasiadosIntentos }
  if (response.status === 400 && problema?.codigo === 'ENLACE_INVALIDO') {
    return { ok: false, mensaje: MENSAJES.enlaceInvalido, enlaceInvalido: true }
  }
  if (response.status === 400 && problema?.errores) {
    return { ok: false, mensaje: MENSAJES.datosInvalidos, errores: problema.errores }
  }
  return { ok: false, mensaje: problema?.title ?? MENSAJES.sinConexion }
}

export async function iniciarSesion(correo: string, contrasena: string): Promise<Resultado<SesionIniciada>> {
  const resultado = await ejecutar(
    // Canal WEB: el backend manda el token de renovación en la cookie HttpOnly y no en el cuerpo.
    () => api.POST('/api/v1/identidad/sesiones', { body: { correo, contrasena, canal: 'WEB' } }),
    (estado, problema) => {
      if (estado === 401) return { ok: false, mensaje: MENSAJES.credenciales }
      if (estado === 403 && problema?.codigo === 'CORREO_SIN_VERIFICAR') {
        return { ok: false, mensaje: MENSAJES.sinVerificar, ayuda: MENSAJES.sinVerificarAyuda }
      }
      return undefined
    },
  )
  if (resultado.ok) almacenSesion.iniciar(resultado.datos)
  return resultado
}

export type DatosRegistro = { nombreNegocio: string; nombre: string; correo: string; contrasena: string }

/** Devuelve el mensaje del 202. Es el mismo exista o no la cuenta (así nadie averigua quién usa Fuertia). */
export async function registrarEntrenador(datos: DatosRegistro): Promise<Resultado<string>> {
  const resultado = await ejecutar(() => api.POST('/api/v1/identidad/registro', { body: datos }))
  return resultado.ok ? { ok: true, datos: resultado.datos.mensaje } : resultado
}

export function verificarCorreo(token: string): Promise<Resultado> {
  return ejecutar(() => api.POST('/api/v1/identidad/verificacion', { body: { token } }), (estado) =>
    // Cualquier 400 aquí es un enlace que no sirve: no hay campos que corregir.
    estado === 400 ? { ok: false, mensaje: MENSAJES.enlaceInvalido, enlaceInvalido: true } : undefined,
  ) as Promise<Resultado>
}

export async function solicitarRecuperacion(correo: string): Promise<Resultado<string>> {
  const resultado = await ejecutar(() => api.POST('/api/v1/identidad/recuperacion', { body: { correo } }))
  return resultado.ok ? { ok: true, datos: resultado.datos.mensaje } : resultado
}

export function confirmarRecuperacion(token: string, contrasenaNueva: string): Promise<Resultado> {
  return ejecutar(() =>
    api.POST('/api/v1/identidad/recuperacion/confirmacion', { body: { token, contrasenaNueva } }),
  ) as Promise<Resultado>
}
