import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useSearchParams } from 'react-router'
import { AvisoError } from '../ui/AvisoError'
import { Campo, CampoContrasena } from '../ui/Campo'
import { useEnfocarAlMostrar } from '../ui/useEnfocarAlMostrar'
import { aplicarErroresDeCampo } from './erroresDeCampo'
import { esquemaNuevaContrasena, esquemaRecuperacion, type DatosNuevaContrasena, type DatosRecuperacion } from './esquemas'
import { RevisaTuCorreo } from './RevisaTuCorreo'
import { confirmarRecuperacion, MENSAJES, solicitarRecuperacion } from './servicioAcceso'

/** /recuperar pide el correo; /recuperar?token=... (el enlace del correo) pide la contraseña nueva. */
export function Recuperar() {
  const [parametros] = useSearchParams()
  const token = parametros.get('token')
  return token ? <NuevaContrasena token={token} /> : <PedirEnlace />
}

function PedirEnlace() {
  const [falla, setFalla] = useState<string>()
  const [mensajeEnviado, setMensajeEnviado] = useState<string>()
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<DatosRecuperacion>({ resolver: zodResolver(esquemaRecuperacion), defaultValues: { correo: '' } })
  const solicitud = useMutation({ mutationFn: (datos: DatosRecuperacion) => solicitarRecuperacion(datos.correo) })

  const enviar = handleSubmit(async (datos) => {
    setFalla(undefined)
    const resultado = await solicitud.mutateAsync(datos)
    if (resultado.ok) {
      setMensajeEnviado(resultado.datos)
      return
    }
    aplicarErroresDeCampo(resultado.errores, ['correo'], setError)
    setFalla(resultado.mensaje)
  })

  if (mensajeEnviado) return <RevisaTuCorreo mensaje={mensajeEnviado} />

  return (
    <form className="acceso__formulario" onSubmit={enviar} noValidate>
      <title>Recupera tu contraseña · Fuertia</title>
      <h1 className="acceso__titulo">Recupera tu contraseña</h1>
      <p className="acceso__texto acceso__texto--secundario">
        Escribe el correo de tu cuenta y te enviamos un enlace para crear una contraseña nueva.
      </p>

      {falla && <AvisoError mensaje={falla} />}

      <Campo
        id="recuperar-correo"
        etiqueta="Correo"
        type="email"
        autoComplete="email"
        inputMode="email"
        error={errors.correo?.message}
        {...register('correo')}
      />

      <button type="submit" className="boton-primario boton-primario--grande" disabled={isSubmitting}>
        {isSubmitting ? 'Enviando…' : 'Enviar enlace'}
      </button>

      <p className="acceso__pie">
        <Link to="/ingresar" className="acceso__enlace">
          Volver a entrar
        </Link>
      </p>
    </form>
  )
}

function NuevaContrasena({ token }: { token: string }) {
  const [falla, setFalla] = useState<string>()
  const [estadoFinal, setEstadoFinal] = useState<'cambiada' | 'enlace-invalido'>()
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<DatosNuevaContrasena>({
    resolver: zodResolver(esquemaNuevaContrasena),
    defaultValues: { contrasenaNueva: '' },
  })
  const confirmacion = useMutation({
    mutationFn: (datos: DatosNuevaContrasena) => confirmarRecuperacion(token, datos.contrasenaNueva),
  })

  const enviar = handleSubmit(async (datos) => {
    setFalla(undefined)
    const resultado = await confirmacion.mutateAsync(datos)
    if (resultado.ok) {
      setEstadoFinal('cambiada')
      return
    }
    if (resultado.enlaceInvalido) {
      setEstadoFinal('enlace-invalido')
      return
    }
    aplicarErroresDeCampo(resultado.errores, ['contrasenaNueva'], setError)
    setFalla(resultado.mensaje)
  })

  if (estadoFinal) return <ResultadoRecuperacion estado={estadoFinal} />

  return (
    <form className="acceso__formulario" onSubmit={enviar} noValidate>
      <title>Crea una contraseña nueva · Fuertia</title>
      <h1 className="acceso__titulo">Crea una contraseña nueva</h1>

      {falla && <AvisoError mensaje={falla} />}

      <CampoContrasena
        id="recuperar-contrasena"
        etiqueta="Contraseña nueva"
        autoComplete="new-password"
        ayuda="Usa 10 caracteres o más."
        error={errors.contrasenaNueva?.message}
        {...register('contrasenaNueva')}
      />

      <button type="submit" className="boton-primario boton-primario--grande" disabled={isSubmitting}>
        {isSubmitting ? 'Guardando…' : 'Guardar contraseña'}
      </button>
    </form>
  )
}

function ResultadoRecuperacion({ estado }: { estado: 'cambiada' | 'enlace-invalido' }) {
  const titulo = useEnfocarAlMostrar<HTMLHeadingElement>()
  const cambiada = estado === 'cambiada'
  const encabezado = cambiada ? 'Tu contraseña cambió' : MENSAJES.enlaceInvalido
  return (
    <div className="acceso__formulario">
      <title>{`${encabezado} · Fuertia`}</title>
      <h1 ref={titulo} tabIndex={-1} className="acceso__titulo">
        {encabezado}
      </h1>
      <p className="acceso__texto acceso__texto--secundario">
        {cambiada
          ? 'Ya puedes entrar con tu contraseña nueva. Por seguridad cerramos las sesiones que tenías abiertas.'
          : 'Pide un enlace nuevo. Cada enlace sirve una sola vez y vence en 1 hora.'}
      </p>
      <Link to={cambiada ? '/ingresar' : '/recuperar'} className="boton-primario boton-primario--grande">
        {cambiada ? 'Entrar' : 'Pedir otro enlace'}
      </Link>
    </div>
  )
}
