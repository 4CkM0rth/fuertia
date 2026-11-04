import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link } from 'react-router'
import { AvisoError } from '../ui/AvisoError'
import { Campo, CampoContrasena } from '../ui/Campo'
import { aplicarErroresDeCampo } from './erroresDeCampo'
import { esquemaRegistro, type DatosRegistroFormulario } from './esquemas'
import { RevisaTuCorreo } from './RevisaTuCorreo'
import { registrarEntrenador } from './servicioAcceso'

const CAMPOS = ['nombreNegocio', 'nombre', 'correo', 'contrasena'] as const

/** /registro: "Crea tu cuenta gratis". No hay diseño propio; sigue el estilo de WebLogin. */
export function Registro() {
  const [falla, setFalla] = useState<string>()
  const [mensajeEnviado, setMensajeEnviado] = useState<string>()
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<DatosRegistroFormulario>({
    resolver: zodResolver(esquemaRegistro),
    defaultValues: { nombreNegocio: '', nombre: '', correo: '', contrasena: '' },
  })
  const registro = useMutation({ mutationFn: registrarEntrenador })

  const enviar = handleSubmit(async (datos) => {
    setFalla(undefined)
    const resultado = await registro.mutateAsync(datos)
    if (resultado.ok) {
      setMensajeEnviado(resultado.datos)
      return
    }
    aplicarErroresDeCampo(resultado.errores, CAMPOS, setError)
    setFalla(resultado.mensaje)
  })

  if (mensajeEnviado) return <RevisaTuCorreo mensaje={mensajeEnviado} />

  return (
    <form className="acceso__formulario" onSubmit={enviar} noValidate>
      <title>Crea tu cuenta · Fuertia</title>
      <h1 className="acceso__titulo">Crea tu cuenta gratis</h1>

      {falla && <AvisoError mensaje={falla} />}

      <Campo
        id="registro-negocio"
        etiqueta="Nombre del negocio"
        autoComplete="organization"
        error={errors.nombreNegocio?.message}
        {...register('nombreNegocio')}
      />
      <Campo
        id="registro-nombre"
        etiqueta="Tu nombre"
        autoComplete="name"
        error={errors.nombre?.message}
        {...register('nombre')}
      />
      <Campo
        id="registro-correo"
        etiqueta="Correo"
        type="email"
        autoComplete="email"
        inputMode="email"
        error={errors.correo?.message}
        {...register('correo')}
      />
      <CampoContrasena
        id="registro-contrasena"
        etiqueta="Contraseña"
        autoComplete="new-password"
        ayuda="Usa 10 caracteres o más."
        error={errors.contrasena?.message}
        {...register('contrasena')}
      />

      <button type="submit" className="boton-primario boton-primario--grande" disabled={isSubmitting}>
        {isSubmitting ? 'Creando tu cuenta…' : 'Crear cuenta'}
      </button>

      <p className="acceso__pie">
        ¿Ya tienes cuenta?{' '}
        <Link to="/ingresar" className="acceso__enlace">
          Entrar
        </Link>
      </p>
    </form>
  )
}
