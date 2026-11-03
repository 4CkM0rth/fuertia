import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { renderizar } from '../pruebas/renderizar'
import { RUTA_ESTADO, servidor } from '../pruebas/servidor'
import { TarjetaEstado } from './TarjetaEstado'

describe('TarjetaEstado', () => {
  it('muestra que está conectado y la versión del backend', async () => {
    renderizar(<TarjetaEstado />)

    expect(screen.getByText('Comprobando la conexión…')).toBeInTheDocument()
    // findBy espera a que aparezca: la respuesta llega de forma asíncrona.
    expect(await screen.findByText('Conectado con el backend · versión 0.1.0')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Reintentar' })).not.toBeInTheDocument()
  })

  it('muestra el error y deja reintentar cuando no hay backend', async () => {
    // Simula que el backend no responde (error de red, como si estuviera apagado).
    servidor.use(http.get(RUTA_ESTADO, () => HttpResponse.error()))
    renderizar(<TarjetaEstado />)

    expect(await screen.findByText('No hay conexión con el backend')).toBeInTheDocument()

    // El backend vuelve: al reintentar, la tarjeta pasa a conectado.
    servidor.resetHandlers()
    await userEvent.click(screen.getByRole('button', { name: 'Reintentar' }))

    expect(await screen.findByText('Conectado con el backend · versión 0.1.0')).toBeInTheDocument()
  })

  it('muestra el error cuando el backend responde con falla', async () => {
    servidor.use(http.get(RUTA_ESTADO, () => new HttpResponse(null, { status: 503 })))
    renderizar(<TarjetaEstado />)

    expect(await screen.findByText('No hay conexión con el backend')).toBeInTheDocument()
  })
})
