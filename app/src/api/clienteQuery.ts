import { QueryClient } from '@tanstack/react-query';

export function crearClienteQuery(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: {
        // Por defecto TanStack Query reintenta 3 veces con espera creciente (unos 7 segundos).
        // Con un solo reintento la persona ve rápido el error y decide si reintenta.
        retry: 1,
        staleTime: 30_000,
      },
    },
  });
}
