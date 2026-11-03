import { QueryClient } from '@tanstack/react-query'

export function crearClienteQuery(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: {
        // Por defecto TanStack Query reintenta 3 veces con espera creciente (unos 7 segundos en total).
        // Con un solo reintento la persona ve rápido el error y decide si reintenta.
        retry: 1,
        // 30 segundos sin volver a pedir lo mismo al cambiar de pantalla o de pestaña.
        staleTime: 30_000,
      },
    },
  })
}
