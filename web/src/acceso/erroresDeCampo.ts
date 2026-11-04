import type { FieldValues, Path, UseFormSetError } from 'react-hook-form'

/**
 * Pone junto a cada campo los errores del 400 del backend. El contrato usa los mismos nombres
 * de campo que el formulario (correo, contrasena...), así que se pasan tal cual.
 * Los nombres que el formulario no tiene se ignoran: el mensaje general ya avisa.
 */
export function aplicarErroresDeCampo<T extends FieldValues>(
  errores: Record<string, string> | undefined,
  campos: readonly Path<T>[],
  setError: UseFormSetError<T>,
): void {
  if (!errores) return
  let primero = true
  for (const campo of campos) {
    const mensaje = errores[campo]
    if (!mensaje) continue
    // El foco va al primer campo con error para que se pueda corregir de una vez.
    setError(campo, { type: 'server', message: mensaje }, { shouldFocus: primero })
    primero = false
  }
}
