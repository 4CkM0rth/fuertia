import { Navigate, Outlet, useLocation } from 'react-router'
import { useSesion } from '../sesion/sesion'

type EstadoNavegacion = { desde?: string } | null

/** Rutas que piden sesión. Sin sesión manda a /ingresar y recuerda a dónde quería ir. */
export function RutaProtegida() {
  const sesion = useSesion()
  const ubicacion = useLocation()

  if (sesion.tipo === 'cargando') {
    return (
      <p role="status" className="cargando">
        Cargando…
      </p>
    )
  }
  if (sesion.tipo === 'sin-sesion') {
    // replace: así el botón Atrás no vuelve a la página protegida y rebota otra vez.
    return <Navigate to="/ingresar" replace state={{ desde: ubicacion.pathname + ubicacion.search }} />
  }
  return <Outlet />
}

/** Rutas para quien no ha entrado (ingresar, registro). Con sesión activa, al panel. */
export function SoloSinSesion() {
  const sesion = useSesion()
  const ubicacion = useLocation()
  if (sesion.tipo === 'activa') {
    const desde = (ubicacion.state as EstadoNavegacion)?.desde
    return <Navigate to={desde ?? '/'} replace />
  }
  return <Outlet />
}
