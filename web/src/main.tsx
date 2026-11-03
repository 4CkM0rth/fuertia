import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClientProvider } from '@tanstack/react-query'
// Poppins va empaquetada con la web (no desde Google Fonts): carga sin depender de terceros
// y no le pasa la IP de los entrenadores a otro servicio. Solo el subconjunto latino, que trae tildes y eñe.
import '@fontsource/poppins/latin-400.css'
import '@fontsource/poppins/latin-500.css'
import '@fontsource/poppins/latin-600.css'
import '@fontsource/poppins/latin-700.css'
import '@fontsource/poppins/latin-800.css'
// Variables --fu-... de claro y oscuro, generadas desde @fuertia/tokens (ver vite.config.ts).
import 'virtual:fuertia-tema.css'
import './index.css'
import App from './App'
import { crearClienteQuery } from './api/clienteQuery'

const clienteQuery = crearClienteQuery()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={clienteQuery}>
      <App />
    </QueryClientProvider>
  </StrictMode>,
)
