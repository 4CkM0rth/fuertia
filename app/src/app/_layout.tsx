import { QueryClientProvider } from '@tanstack/react-query';
import { useFonts } from 'expo-font';
import { DarkTheme, DefaultTheme, Stack, ThemeProvider } from 'expo-router';
import * as SplashScreen from 'expo-splash-screen';
import { StatusBar } from 'expo-status-bar';
import { useEffect, useState } from 'react';

import { crearClienteQuery } from '@/api/clienteQuery';
import { PantallaCarga } from '@/pantallas/PantallaCarga';
import { ProveedorTema, useTema } from '@/tema/ContextoTema';
import { archivosFuentes } from '@/tema/fuentes';

// La pantalla de bienvenida nativa se queda hasta que estén las fuentes y la apariencia guardada.
// Así la persona no ve la app un instante con la letra del sistema o en el modo equivocado.
SplashScreen.preventAutoHideAsync().catch(() => {});

export default function RootLayout() {
  // useState con función: el cliente se crea una sola vez y no en cada render.
  const [clienteQuery] = useState(crearClienteQuery);
  return (
    <QueryClientProvider client={clienteQuery}>
      <ProveedorTema>
        <Contenido />
      </ProveedorTema>
    </QueryClientProvider>
  );
}

function Contenido() {
  const { tema, colores, listo } = useTema();
  const [fuentesCargadas, errorFuentes] = useFonts(archivosFuentes);
  // Si las fuentes fallan se sigue con la del sistema: es mejor eso que una app bloqueada.
  const preparada = listo && (fuentesCargadas || errorFuentes !== null);

  useEffect(() => {
    if (preparada) SplashScreen.hideAsync().catch(() => {});
  }, [preparada]);

  if (!preparada) {
    return <PantallaCarga />;
  }

  // El tema de navegación usa los mismos tokens, para que el fondo entre pantallas coincida.
  const base = tema === 'oscuro' ? DarkTheme : DefaultTheme;
  const temaNavegacion = {
    ...base,
    colors: {
      ...base.colors,
      primary: colores.primario,
      background: colores.fondo,
      card: colores.tarjeta,
      text: colores.texto,
      border: colores.linea,
      notification: colores.primario,
    },
  };

  return (
    <ThemeProvider value={temaNavegacion}>
      {/* Barra de estado con íconos claros sobre fondo oscuro y al revés. */}
      <StatusBar style={tema === 'oscuro' ? 'light' : 'dark'} />
      <Stack screenOptions={{ headerShown: false, contentStyle: { backgroundColor: colores.fondo } }} />
    </ThemeProvider>
  );
}
