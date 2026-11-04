import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import type { ReactElement } from 'react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { rutas } from '../rutas/rutas'
import { almacenSesion } from '../sesion/almacenSesion'
import { sesionDePrueba } from './servidor'

/**
 * Un QueryClient nuevo por prueba para que la caché de una prueba no se cuele en la siguiente,
 * y sin reintentos para que el error aparezca enseguida.
 */
const nuevoClienteQuery = () => new QueryClient({ defaultOptions: { queries: { retry: false } } })

/** Monta un componente suelto con TanStack Query. */
export function renderizar(ui: ReactElement) {
  return render(<QueryClientProvider client={nuevoClienteQuery()}>{ui}</QueryClientProvider>)
}

/**
 * Monta la web completa (las mismas rutas del navegador) empezando en `ruta`.
 * createMemoryRouter guarda la dirección en memoria en lugar de la barra del navegador.
 * `sesion` deja la sesión lista antes de pintar, como si ya hubiera arrancado.
 */
export function renderizarApp(ruta: string, { sesion = 'sin-sesion' }: { sesion?: 'activa' | 'sin-sesion' } = {}) {
  if (sesion === 'activa') almacenSesion.iniciar(sesionDePrueba())
  else almacenSesion.terminar()

  const enrutador = createMemoryRouter(rutas, { initialEntries: [ruta] })
  const resultado = render(
    <QueryClientProvider client={nuevoClienteQuery()}>
      <RouterProvider router={enrutador} />
    </QueryClientProvider>,
  )
  return { ...resultado, enrutador }
}

/** Todo lo que hay guardado en localStorage y sessionStorage, para buscar tokens filtrados. */
export function contenidoDelAlmacenamiento(): string {
  const leer = (almacen: Storage) =>
    Array.from({ length: almacen.length }, (_, i) => {
      const clave = almacen.key(i) ?? ''
      return `${clave}=${almacen.getItem(clave) ?? ''}`
    }).join('\n')
  return leer(localStorage) + '\n' + leer(sessionStorage)
}
