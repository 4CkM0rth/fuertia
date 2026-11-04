import createClient from 'openapi-fetch';

import type { components, paths } from './esquema';

// esquema.d.ts lo genera "npm run generar:api" desde docs/api/openapi.yaml. No se edita a mano:
// si el contrato cambia, se vuelve a generar y TypeScript avisa dónde se rompió la app.

export type EstadoSistema = components['schemas']['EstadoSistema'];

// Expo reemplaza las variables EXPO_PUBLIC_* al empaquetar (las lee de app/.env).
// Ojo: en un celular físico "localhost" es el propio celular, no el computador. Ahí hay que
// poner la IP del computador en la red wifi, por ejemplo http://192.168.1.20:8080 (ver README).
export const urlApi = process.env.EXPO_PUBLIC_API_URL || 'http://localhost:8080';

export const api = createClient<paths>({
  baseUrl: urlApi,
  // openapi-fetch guarda la función fetch cuando se crea el cliente. Con esta envoltura se usa
  // la fetch vigente en cada llamada, lo que permite simularla en las pruebas.
  fetch: (solicitud) => globalThis.fetch(solicitud),
});
