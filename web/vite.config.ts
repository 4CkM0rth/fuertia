import react from '@vitejs/plugin-react'
import type { Plugin } from 'vite'
import { defineConfig } from 'vitest/config'
import { generarCssTema } from './src/tema/cssDelTema.ts'

// Módulo virtual: el CSS con los colores no existe como archivo; Vite lo genera al vuelo
// desde @fuertia/tokens. Así la web y la app leen la misma fuente y nadie copia colores a mano.
const ID_TEMA = 'virtual:fuertia-tema.css'
// El prefijo \0 es la convención de Vite para ids virtuales: evita que otro plugin lo busque en disco.
const ID_RESUELTO = '\0' + ID_TEMA

function temaFuertia(): Plugin {
  return {
    name: 'fuertia-tema',
    resolveId(id) {
      return id === ID_TEMA ? ID_RESUELTO : undefined
    },
    load(id) {
      return id === ID_RESUELTO ? generarCssTema() : undefined
    },
  }
}

export default defineConfig({
  plugins: [react(), temaFuertia()],
  server: {
    // En desarrollo la web llama a /api en su mismo origen y Vite lo reenvía al backend.
    // Así el navegador no ve dos orígenes distintos y no hace falta configurar CORS para local.
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/pruebas/setup.ts'],
  },
})
