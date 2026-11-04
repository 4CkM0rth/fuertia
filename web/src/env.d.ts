// Variables de entorno que la web lee con import.meta.env. Vite solo expone las que empiezan por VITE_.
interface ImportMetaEnv {
  /** URL del backend. Vacía en desarrollo para usar el proxy de Vite. */
  readonly VITE_API_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
