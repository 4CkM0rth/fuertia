import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/cliente'
import { TarjetaEstado } from '../estado/TarjetaEstado'
import { cerrarSesion, useSesion } from '../sesion/sesion'
import { SelectorTema } from '../tema/SelectorTema'

/** Panel provisional de "/" hasta que se construya el dashboard. */
export function Panel() {
  const sesion = useSesion()
  const clienteQuery = useQueryClient()
  const activa = sesion.tipo === 'activa'

  // Pide los datos frescos del usuario. Es una llamada con token: si el token venció,
  // el cliente renueva la sesión y la repite sin que la persona lo note.
  const yo = useQuery({
    queryKey: ['identidad', 'yo', activa ? sesion.usuario.id : null],
    queryFn: async () => {
      const { data, response } = await api.GET('/api/v1/identidad/yo')
      if (!data) throw new Error(`El backend respondió ${response.status}`)
      return data
    },
    // Sin sesión no se pide nada (por ejemplo, justo después de cerrar sesión).
    enabled: activa,
    retry: false,
  })

  const salida = useMutation({
    mutationFn: cerrarSesion,
    // Borra de memoria los datos de esta persona. RutaProtegida ya la está mandando a /ingresar.
    onSettled: () => clienteQuery.clear(),
  })

  if (!activa) return null
  const usuario = yo.data ?? sesion.usuario

  return (
    <div className="pagina">
      <title>Inicio · Fuertia</title>
      <header className="cabecera">
        <div className="marca">
          <span className="marca__logo" aria-hidden="true">
            F
          </span>
          <div className="marca__textos">
            <span className="marca__nombre">Fuertia</span>
            <span className="marca__lema">Planificación deportiva</span>
          </div>
        </div>
        <SelectorTema />
      </header>

      <main className="contenido">
        <section className="tarjeta" aria-labelledby="titulo-saludo">
          <h1 id="titulo-saludo" className="tarjeta__saludo">
            Hola, {usuario.nombre}
          </h1>
          <p className="tarjeta__detalle">{usuario.nombreNegocio}</p>
          <button
            type="button"
            className="boton-secundario"
            onClick={() => salida.mutate()}
            disabled={salida.isPending}
          >
            {salida.isPending ? 'Cerrando sesión…' : 'Cerrar sesión'}
          </button>
        </section>
        <TarjetaEstado />
      </main>
    </div>
  )
}
