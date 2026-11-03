import { useQuery } from '@tanstack/react-query';

import { api, type EstadoSistema } from './cliente';

/** Consulta GET /api/v1/sistema/estado. Sirve para saber si la app habla con el backend. */
export function useEstadoSistema() {
  return useQuery<EstadoSistema>({
    queryKey: ['sistema', 'estado'],
    queryFn: async ({ signal }) => {
      const { data, response } = await api.GET('/api/v1/sistema/estado', { signal });
      // openapi-fetch no lanza errores con respuestas 4xx o 5xx: devuelve data vacía y el error
      // aparte. TanStack Query necesita que la función lance para pasar al estado de error.
      if (!response.ok || data === undefined) {
        throw new Error(`El backend respondió ${response.status}`);
      }
      return data;
    },
  });
}
