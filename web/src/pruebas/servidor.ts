import { http, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'
import type { EstadoSistema } from '../api/cliente'

// "*" al inicio: coincide con cualquier origen, así la prueba no depende de VITE_API_URL.
export const RUTA_ESTADO = '*/api/v1/sistema/estado'

// Respuesta tipada con el esquema del contrato: si el contrato cambia, esta prueba deja de compilar.
export const estadoDePrueba: EstadoSistema = { nombre: 'Fuertia', version: '0.1.0', estado: 'ok' }

/** Backend falso: MSW intercepta las llamadas fetch y responde lo que digan estos manejadores. */
export const servidor = setupServer(http.get(RUTA_ESTADO, () => HttpResponse.json(estadoDePrueba)))
