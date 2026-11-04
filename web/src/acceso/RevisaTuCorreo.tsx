import { Link } from 'react-router'
import { useEnfocarAlMostrar } from '../ui/useEnfocarAlMostrar'

/**
 * Pantalla después de registrarse o de pedir recuperar la contraseña. Muestra el mensaje del 202
 * tal cual: es el mismo exista o no la cuenta, para que nadie pueda averiguar quién usa Fuertia.
 */
export function RevisaTuCorreo({ mensaje }: { mensaje: string }) {
  const titulo = useEnfocarAlMostrar<HTMLHeadingElement>()
  return (
    <div className="acceso__formulario">
      <title>Revisa tu correo · Fuertia</title>
      <h1 ref={titulo} tabIndex={-1} className="acceso__titulo">
        Revisa tu correo
      </h1>
      <p className="acceso__texto">{mensaje}</p>
      <p className="acceso__texto acceso__texto--secundario">
        Si no lo ves en unos minutos, revisa la carpeta de spam.
      </p>
      <Link to="/ingresar" className="boton-primario boton-primario--grande">
        Volver a entrar
      </Link>
    </div>
  )
}
