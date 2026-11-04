import { useSyncExternalStore } from 'react'
import { api, renovarSesion } from '../api/cliente'
import { almacenSesion } from './almacenSesion'

/** Estado de la sesión dentro de React: el componente se vuelve a pintar cuando cambia. */
export function useSesion() {
  return useSyncExternalStore(almacenSesion.suscribir, almacenSesion.obtener)
}

/**
 * Al abrir o recargar la web no hay token en memoria. Si el navegador todavía tiene la cookie
 * de renovación, esta llamada recupera la sesión sin pedir la contraseña otra vez.
 */
export function arrancarSesion(): Promise<boolean> {
  return renovarSesion()
}

/**
 * Cierra la sesión en el backend (invalida el token de renovación y borra la cookie) y en la web.
 * Aunque el backend falle, la web olvida el token igual: la persona pidió salir.
 */
export async function cerrarSesion(): Promise<void> {
  try {
    await api.DELETE('/api/v1/identidad/sesiones/actual')
  } catch {
    // Sin red: no se puede avisar al backend, pero la sesión se cierra en la web de todos modos.
  } finally {
    almacenSesion.terminar()
  }
}
