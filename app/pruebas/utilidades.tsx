import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render } from '@testing-library/react-native';
import type { ReactElement, ReactNode } from 'react';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { ProveedorTema } from '@/tema/ContextoTema';

// Medidas fijas de pantalla: en Jest no hay teléfono que diga cuánto mide el área segura.
const metricasPrueba = {
  frame: { x: 0, y: 0, width: 390, height: 844 },
  insets: { top: 0, left: 0, right: 0, bottom: 0 },
};

/** Renderiza con los mismos proveedores que la app (consultas, tema y área segura). */
export function renderConProveedores(elemento: ReactElement) {
  // Un cliente nuevo por prueba y sin reintentos: cada prueba empieza limpia y el error llega de una.
  const clienteQuery = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: Infinity } },
  });
  function Proveedores({ children }: { children: ReactNode }) {
    return (
      <SafeAreaProvider initialMetrics={metricasPrueba}>
        <QueryClientProvider client={clienteQuery}>
          <ProveedorTema>{children}</ProveedorTema>
        </QueryClientProvider>
      </SafeAreaProvider>
    );
  }
  return render(elemento, { wrapper: Proveedores });
}

/** Respuesta HTTP simulada en JSON, como la que daría el backend. */
export function respuestaJson(cuerpo: unknown, estado = 200): Response {
  return new Response(JSON.stringify(cuerpo), {
    status: estado,
    headers: { 'Content-Type': 'application/json' },
  });
}
