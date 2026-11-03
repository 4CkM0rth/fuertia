import { useEstadoSistema } from './useEstadoSistema'

export function TarjetaEstado() {
  const { data, isPending, isError, isFetching, refetch } = useEstadoSistema()

  return (
    <section className="tarjeta" aria-labelledby="titulo-estado">
      <h2 id="titulo-estado" className="tarjeta__titulo">
        Estado del sistema
      </h2>

      {/* role="status" hace que el lector de pantalla anuncie el cambio sin mover el foco. */}
      <div role="status" className="estado">
        {isPending && <p className="estado__texto">Comprobando la conexión…</p>}

        {data && !isError && (
          <p className="estado__texto estado__texto--ok">
            <span className="estado__punto" aria-hidden="true" />
            Conectado con el backend · versión {data.version}
          </p>
        )}

        {isError && (
          <p className="estado__texto estado__texto--error">
            <span className="estado__punto" aria-hidden="true" />
            No hay conexión con el backend
          </p>
        )}
      </div>

      {isError && (
        <button
          type="button"
          className="boton-primario"
          onClick={() => void refetch()}
          // Mientras reintenta se desactiva para no disparar varias llamadas seguidas.
          disabled={isFetching}
        >
          {isFetching ? 'Reintentando…' : 'Reintentar'}
        </button>
      )}
    </section>
  )
}
