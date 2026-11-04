import { Navigate, type RouteObject } from 'react-router'
import { DisenoAcceso } from '../acceso/DisenoAcceso'
import { Ingresar } from '../acceso/Ingresar'
import { Recuperar } from '../acceso/Recuperar'
import { Registro } from '../acceso/Registro'
import { Verificar } from '../acceso/Verificar'
import { Panel } from '../panel/Panel'
import { RutaProtegida, SoloSinSesion } from './Guardias'

/**
 * Mapa de rutas de la web. Se exporta como lista (y no dentro de un componente) para usar
 * la misma en el navegador (createBrowserRouter) y en las pruebas (createMemoryRouter).
 *
 * Las rutas sin "path" solo envuelven: ponen el marco (DisenoAcceso) o la regla de sesión
 * (RutaProtegida, SoloSinSesion) y muestran a sus hijas donde está el <Outlet />.
 */
export const rutas: RouteObject[] = [
  {
    element: <DisenoAcceso />,
    children: [
      {
        element: <SoloSinSesion />,
        children: [
          { path: 'ingresar', element: <Ingresar /> },
          { path: 'registro', element: <Registro /> },
        ],
      },
      // Verificar y recuperar funcionan con o sin sesión: el enlace llega por correo.
      { path: 'verificar', element: <Verificar /> },
      { path: 'recuperar', element: <Recuperar /> },
    ],
  },
  {
    element: <RutaProtegida />,
    children: [{ index: true, element: <Panel /> }],
  },
  // Cualquier otra dirección vuelve al inicio (que a su vez manda a /ingresar si no hay sesión).
  { path: '*', element: <Navigate to="/" replace /> },
]
