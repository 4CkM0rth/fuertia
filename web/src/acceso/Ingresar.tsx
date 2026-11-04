import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link } from 'react-router'
import { AvisoError } from '../ui/AvisoError'
import { Campo, CampoContrasena } from '../ui/Campo'
import { aplicarErroresDeCampo } from './erroresDeCampo'
import { esquemaIngreso, type DatosIngreso } from './esquemas'
import { iniciarSesion } from './servicioAcceso'

/** /ingresar (design/screens/WebLogin.dc.html). */
export function Ingresar() {
  const [falla, setFalla] = useState<{ mensaje: string; ayuda?: string }>()
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<DatosIngreso>({
    resolver: zodResolver(esquemaIngreso),
    defaultValues: { correo: '', contrasena: '' },
  })
  const ingreso = useMutation({ mutationFn: (datos: DatosIngreso) => iniciarSesion(datos.correo, datos.contrasena) })

  // handleSubmit valida con Zod y solo llama a esta función si todo está bien.
  // Mientras la promesa no termina, isSubmitting es true y el botón queda desactivado.
  const enviar = handleSubmit(async (datos) => {
    setFalla(undefined)
    const resultado = await ingreso.mutateAsync(datos)
    // Si entra, no hace falta navegar aquí: la sesión cambia a "activa" y SoloSinSesion
    // lleva a la persona al panel (o a la página que quería abrir).
    if (resultado.ok) return
    aplicarErroresDeCampo(resultado.errores, ['correo', 'contrasena'], setError)
    setFalla({ mensaje: resultado.mensaje, ayuda: resultado.ayuda })
  })

  return (
    <form className="acceso__formulario" onSubmit={enviar} noValidate>
      <title>Entrar · Fuertia</title>
      <h1 className="acceso__titulo">Entrar</h1>

      {falla && <AvisoError mensaje={falla.mensaje} ayuda={falla.ayuda} />}

      <Campo
        id="ingreso-correo"
        etiqueta="Correo"
        type="email"
        autoComplete="email"
        inputMode="email"
        error={errors.correo?.message}
        {...register('correo')}
      />
      <CampoContrasena
        id="ingreso-contrasena"
        etiqueta="Contraseña"
        autoComplete="current-password"
        error={errors.contrasena?.message}
        {...register('contrasena')}
      />
      <Link to="/recuperar" className="acceso__enlace acceso__enlace--derecha">
        ¿Olvidaste tu contraseña?
      </Link>

      <button type="submit" className="boton-primario boton-primario--grande" disabled={isSubmitting}>
        {isSubmitting ? 'Entrando…' : 'Entrar'}
      </button>

      {/* "Entrar con Google" está en el diseño, pero no se muestra: necesita credenciales de Google
          que todavía no existen (decisión anotada en la Bitácora técnica). Vuelve cuando existan. */}

      <p className="acceso__pie">
        ¿Eres entrenador y no tienes cuenta?{' '}
        <Link to="/registro" className="acceso__enlace">
          Crea tu cuenta gratis
        </Link>
      </p>
    </form>
  )
}
