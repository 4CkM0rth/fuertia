/** Mensaje de error general del formulario. role="alert" hace que el lector lo anuncie al aparecer. */
export function AvisoError({ mensaje, ayuda }: { mensaje: string; ayuda?: string }) {
  return (
    <div role="alert" className="aviso-error">
      <p className="aviso-error__titulo">{mensaje}</p>
      {ayuda && <p className="aviso-error__ayuda">{ayuda}</p>}
    </div>
  )
}
