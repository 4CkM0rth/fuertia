// Se ejecuta antes de cada archivo de pruebas (ver test.setupFiles en vite.config.ts).
import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterAll, afterEach, beforeAll } from 'vitest'
import { almacenSesion } from '../sesion/almacenSesion'
import { servidor } from './servidor'

// onUnhandledRequest: 'error' hace fallar la prueba si el código llama a una ruta que no simulamos.
beforeAll(() => servidor.listen({ onUnhandledRequest: 'error' }))

afterEach(() => {
  // Cada prueba empieza limpia: sin manejadores cambiados, sin componentes montados,
  // sin tema guardado y sin sesión en memoria.
  servidor.resetHandlers()
  cleanup()
  localStorage.clear()
  sessionStorage.clear()
  delete document.documentElement.dataset.tema
  almacenSesion.reiniciar()
})

afterAll(() => servidor.close())
