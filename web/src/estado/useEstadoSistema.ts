import { useQuery } from '@tanstack/react-query'
import { api } from '../api/cliente'

export function useEstadoSistema() {
  return useQuery({
    // La llave identifica el dato en la caché: cualquier componente que use esta misma llave
    // comparte la respuesta en lugar de volver a pedirla.
    queryKey: ['sistema', 'estado'],
    queryFn: async () => {
      // La ruta y la forma de la respuesta salen del contrato: un error de tipeo aquí no compila.
      const { data, response } = await api.GET('/api/v1/sistema/estado')
      // openapi-fetch no lanza error con respuestas 4xx o 5xx; lo lanzamos para que TanStack Query
      // pase a estado de error. Si no hay red, fetch ya lanza por su cuenta.
      if (!data) throw new Error(`El backend respondió ${response.status}`)
      return data
    },
  })
}
