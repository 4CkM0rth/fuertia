import createClient from 'openapi-fetch'
import type { components, paths } from './esquema'

// esquema.d.ts lo genera "npm run generar:api" desde docs/api/openapi.yaml. No se edita a mano:
// si el contrato cambia, se vuelve a generar y TypeScript avisa dónde se rompió la web.

export type EstadoSistema = components['schemas']['EstadoSistema']

// VITE_API_URL vacía (lo normal en desarrollo) significa "el mismo origen de la página":
// las llamadas van a /api y el proxy de Vite las manda al backend en el puerto 8080.
const urlBase = import.meta.env.VITE_API_URL || globalThis.location?.origin

export const api = createClient<paths>({
  baseUrl: urlBase,
  // openapi-fetch guarda la función fetch cuando se crea el cliente. Con esta envoltura se usa
  // la fetch vigente en cada llamada, lo que permite que MSW la intercepte en las pruebas.
  fetch: (solicitud) => globalThis.fetch(solicitud),
})
