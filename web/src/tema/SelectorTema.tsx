import { useEffect, useState } from 'react'
import { aplicarTema, guardarPreferencia, leerPreferencia, opcionesTema } from './tema'

/** Selector Claro / Oscuro / Sistema, como el control de Apariencia del diseño (Perfil). */
export function SelectorTema() {
  // Se lee una sola vez al montar; el script de index.html ya aplicó el tema antes de pintar.
  const [preferencia, setPreferencia] = useState(leerPreferencia)

  useEffect(() => {
    aplicarTema(preferencia)
    guardarPreferencia(preferencia)
  }, [preferencia])

  return (
    <div className="selector-tema" role="group" aria-label="Apariencia">
      {opcionesTema.map((opcion) => (
        <button
          key={opcion.valor}
          type="button"
          className="selector-tema__opcion"
          // aria-pressed le dice al lector de pantalla cuál está activa (es un grupo de botones conmutables).
          aria-pressed={preferencia === opcion.valor}
          onClick={() => setPreferencia(opcion.valor)}
        >
          {opcion.etiqueta}
        </button>
      ))}
    </div>
  )
}
