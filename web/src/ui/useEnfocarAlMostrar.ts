import { useEffect, useRef } from 'react'

/**
 * Pone el foco en un título cuando aparece una pantalla de resultado ("Revisa tu correo").
 * En una web de una sola página el navegador no anuncia el cambio de contenido; mover el foco
 * al título hace que el lector de pantalla lo lea y que el teclado siga desde ahí.
 * El título necesita tabIndex={-1} para poder recibir foco sin entrar en el orden del tabulador.
 */
export function useEnfocarAlMostrar<T extends HTMLElement>() {
  const referencia = useRef<T>(null)
  useEffect(() => {
    referencia.current?.focus()
  }, [])
  return referencia
}
