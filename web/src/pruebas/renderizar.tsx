import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import type { ReactElement } from 'react'

/**
 * Monta un componente con su propio QueryClient. Uno nuevo por prueba para que la caché
 * de una prueba no se cuele en la siguiente, y sin reintentos para que el error aparezca enseguida.
 */
export function renderizar(ui: ReactElement) {
  const clienteQuery = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(<QueryClientProvider client={clienteQuery}>{ui}</QueryClientProvider>)
}
