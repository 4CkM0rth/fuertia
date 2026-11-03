export type PreferenciaTema = 'claro' | 'oscuro' | 'sistema'

export const opcionesTema: { valor: PreferenciaTema; etiqueta: string }[] = [
  { valor: 'claro', etiqueta: 'Claro' },
  { valor: 'oscuro', etiqueta: 'Oscuro' },
  { valor: 'sistema', etiqueta: 'Sistema' },
]

// Si cambias esta clave, cámbiala también en el script de index.html.
export const CLAVE_TEMA = 'fuertia.tema'

const esPreferencia = (valor: unknown): valor is PreferenciaTema =>
  valor === 'claro' || valor === 'oscuro' || valor === 'sistema'

// localStorage puede fallar (modo privado o almacenamiento bloqueado). En ese caso la web
// sigue funcionando con "Sistema" y simplemente no recuerda la elección.
export function leerPreferencia(): PreferenciaTema {
  try {
    const guardada = localStorage.getItem(CLAVE_TEMA)
    return esPreferencia(guardada) ? guardada : 'sistema'
  } catch {
    return 'sistema'
  }
}

export function guardarPreferencia(preferencia: PreferenciaTema): void {
  try {
    localStorage.setItem(CLAVE_TEMA, preferencia)
  } catch {
    // Sin almacenamiento: el tema se aplica igual, solo que no se recuerda.
  }
}

/** El CSS generado mira este atributo para decidir los colores. */
export function aplicarTema(preferencia: PreferenciaTema): void {
  document.documentElement.dataset.tema = preferencia
}
