import type { components } from '../api/esquema'

export type UsuarioActual = components['schemas']['UsuarioActual']
export type SesionIniciada = components['schemas']['SesionIniciada']

/**
 * Estado de la sesión:
 * - cargando: la web acaba de abrir y está intentando recuperar la sesión con la cookie.
 * - sin-sesion: no hay sesión (nunca entró, cerró sesión o la renovación falló).
 * - activa: hay token de acceso y datos del usuario.
 */
export type EstadoSesion =
  | { tipo: 'cargando' }
  | { tipo: 'sin-sesion' }
  | { tipo: 'activa'; tokenDeAcceso: string; usuario: UsuarioActual }

/*
 * El token de acceso vive SOLO en esta variable de JavaScript, nunca en localStorage ni
 * sessionStorage. Si un script malicioso llegara a correr en la página (XSS), el almacenamiento
 * del navegador se lee con una línea; una variable de un módulo es mucho más difícil de alcanzar,
 * y además desaparece al cerrar o recargar la pestaña. Tras recargar, la sesión se recupera
 * con la cookie HttpOnly del token de renovación, que JavaScript no puede leer.
 */
let estado: EstadoSesion = { tipo: 'cargando' }
const oyentes = new Set<() => void>()

function cambiar(nuevo: EstadoSesion) {
  estado = nuevo
  oyentes.forEach((oyente) => oyente())
}

export const almacenSesion = {
  obtener: (): EstadoSesion => estado,

  tokenDeAcceso: (): string | undefined => (estado.tipo === 'activa' ? estado.tokenDeAcceso : undefined),

  iniciar: (sesion: SesionIniciada) =>
    cambiar({ tipo: 'activa', tokenDeAcceso: sesion.tokenDeAcceso, usuario: sesion.usuario }),

  actualizarUsuario: (usuario: UsuarioActual) => {
    if (estado.tipo === 'activa') cambiar({ ...estado, usuario })
  },

  terminar: () => cambiar({ tipo: 'sin-sesion' }),

  /** Para React (useSyncExternalStore): avisa cada vez que cambia la sesión. */
  suscribir: (oyente: () => void) => {
    oyentes.add(oyente)
    return () => {
      oyentes.delete(oyente)
    }
  },

  /** Solo para pruebas: vuelve al estado del arranque. */
  reiniciar: () => cambiar({ tipo: 'cargando' }),
}
