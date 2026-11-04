import { useQuery } from '@tanstack/react-query'
import { Link, useSearchParams } from 'react-router'
import { useEnfocarAlMostrar } from '../ui/useEnfocarAlMostrar'
import { MENSAJES, verificarCorreo } from './servicioAcceso'

/** /verificar?token=...: confirma el correo apenas se abre el enlace del correo. */
export function Verificar() {
  const [parametros] = useSearchParams()
  const token = parametros.get('token') ?? ''

  // Se usa useQuery (y no un useEffect con fetch) porque deduplica: en modo estricto React monta
  // el componente dos veces en desarrollo, y como el token es de un solo uso, una segunda
  // llamada fallaría y mostraría "enlace no válido" a alguien que sí confirmó.
  const verificacion = useQuery({
    queryKey: ['identidad', 'verificacion', token],
    queryFn: () => verificarCorreo(token),
    enabled: token !== '',
    retry: false,
    staleTime: Infinity,
    gcTime: Infinity,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
  })

  if (token === '') return <ResultadoVerificacion exito={false} mensaje={MENSAJES.enlaceInvalido} />
  if (!verificacion.data) {
    return (
      <div className="acceso__formulario">
        <title>Confirmando tu correo · Fuertia</title>
        <p role="status" className="acceso__texto">
          Confirmando tu correo…
        </p>
      </div>
    )
  }
  const resultado = verificacion.data
  return resultado.ok ? (
    <ResultadoVerificacion exito mensaje="Tu correo quedó confirmado" />
  ) : (
    <ResultadoVerificacion exito={false} mensaje={resultado.mensaje} />
  )
}

function ResultadoVerificacion({ exito, mensaje }: { exito: boolean; mensaje: string }) {
  const titulo = useEnfocarAlMostrar<HTMLHeadingElement>()
  return (
    <div className="acceso__formulario">
      <title>{`${mensaje} · Fuertia`}</title>
      <h1 ref={titulo} tabIndex={-1} className="acceso__titulo">
        {mensaje}
      </h1>
      <p className="acceso__texto acceso__texto--secundario">
        {exito
          ? 'Ya puedes entrar a Fuertia con tu correo y tu contraseña.'
          : 'Si ya confirmaste tu correo, puedes entrar normalmente.'}
      </p>
      <Link to="/ingresar" className="boton-primario boton-primario--grande">
        Entrar
      </Link>
    </div>
  )
}
