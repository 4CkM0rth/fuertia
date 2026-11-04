import { z } from 'zod'

/*
 * Reglas de los formularios de acceso. Los límites son los mismos del contrato
 * (docs/api/openapi.yaml): así la persona ve el error antes de enviar y el backend
 * igual lo vuelve a validar, porque nunca hay que confiar solo en el navegador.
 */

const correo = z
  .string()
  .trim()
  .min(1, 'Escribe tu correo')
  .max(254, 'El correo es demasiado largo')
  .pipe(z.email('Escribe un correo válido, por ejemplo nombre@correo.com'))

const contrasenaNueva = z
  .string()
  .min(10, 'Usa 10 caracteres o más')
  .max(128, 'Usa 128 caracteres o menos')

const nombreCorto = (vacio: string) =>
  z.string().trim().min(1, vacio).min(2, 'Escribe al menos 2 caracteres').max(80, 'Usa 80 caracteres o menos')

export const esquemaIngreso = z.object({
  correo,
  // Al entrar no se exige el mínimo de 10: si la contraseña es incorrecta, lo dice el backend.
  contrasena: z.string().min(1, 'Escribe tu contraseña').max(128, 'Usa 128 caracteres o menos'),
})

export const esquemaRegistro = z.object({
  nombreNegocio: nombreCorto('Escribe el nombre de tu negocio'),
  nombre: nombreCorto('Escribe tu nombre'),
  correo,
  contrasena: contrasenaNueva,
})

export const esquemaRecuperacion = z.object({ correo })

export const esquemaNuevaContrasena = z.object({ contrasenaNueva })

export type DatosIngreso = z.infer<typeof esquemaIngreso>
export type DatosRegistroFormulario = z.infer<typeof esquemaRegistro>
export type DatosRecuperacion = z.infer<typeof esquemaRecuperacion>
export type DatosNuevaContrasena = z.infer<typeof esquemaNuevaContrasena>
