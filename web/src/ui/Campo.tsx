import { useState, type ComponentProps, type ReactNode } from 'react'

type PropsCampo = Omit<ComponentProps<'input'>, 'id'> & {
  id: string
  etiqueta: string
  /** Texto de ayuda fijo debajo del campo, por ejemplo el mínimo de caracteres. */
  ayuda?: string
  error?: string
  /** Botón dentro del campo (el de mostrar contraseña). */
  accesorio?: ReactNode
}

/**
 * Campo con etiqueta visible. El error y la ayuda quedan unidos al campo con aria-describedby:
 * el lector de pantalla los lee al llegar al campo, no solo cuando aparecen.
 */
export function Campo({ id, etiqueta, ayuda, error, accesorio, className, ...resto }: PropsCampo) {
  const idAyuda = `${id}-ayuda`
  const idError = `${id}-error`
  const describe = [ayuda ? idAyuda : '', error ? idError : ''].filter(Boolean).join(' ') || undefined

  return (
    <div className="campo">
      <label htmlFor={id} className="campo__etiqueta">
        {etiqueta}
      </label>
      <div className="campo__caja">
        <input
          id={id}
          className={['campo__entrada', accesorio ? 'campo__entrada--con-accesorio' : '', className ?? '']
            .filter(Boolean)
            .join(' ')}
          aria-invalid={error ? true : undefined}
          aria-describedby={describe}
          {...resto}
        />
        {accesorio}
      </div>
      {ayuda && (
        <p id={idAyuda} className="campo__ayuda">
          {ayuda}
        </p>
      )}
      {error && (
        <p id={idError} className="campo__error">
          {error}
        </p>
      )}
    </div>
  )
}

type PropsContrasena = Omit<PropsCampo, 'type' | 'accesorio'>

/** Campo de contraseña con un botón para verla mientras se escribe (útil en el celular). */
export function CampoContrasena(props: PropsContrasena) {
  const [visible, setVisible] = useState(false)

  return (
    <Campo
      {...props}
      type={visible ? 'text' : 'password'}
      // Evita que el navegador ponga corrector o mayúscula inicial cuando la contraseña se ve como texto.
      autoCapitalize="none"
      autoCorrect="off"
      spellCheck={false}
      accesorio={
        <button
          type="button"
          className="campo__accesorio"
          // Etiqueta fija y aria-pressed: el lector dice "Mostrar contraseña, botón, presionado / no presionado".
          aria-label="Mostrar contraseña"
          aria-pressed={visible}
          aria-controls={props.id}
          onClick={() => setVisible((antes) => !antes)}
        >
          {visible ? <IconoOjoTachado /> : <IconoOjo />}
        </button>
      }
    />
  )
}

function IconoOjo() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z" />
      <circle cx="12" cy="12" r="3" />
    </svg>
  )
}

function IconoOjoTachado() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M10.6 5.1A10.7 10.7 0 0 1 12 5c6.5 0 10 7 10 7a17.6 17.6 0 0 1-2.2 3.2" />
      <path d="M6.6 6.6C3.8 8.4 2 12 2 12s3.5 7 10 7a9.7 9.7 0 0 0 5.4-1.6" />
      <path d="M9.9 9.9a3 3 0 0 0 4.2 4.2" />
      <path d="m2 2 20 20" />
    </svg>
  )
}
