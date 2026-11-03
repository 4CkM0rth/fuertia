import AsyncStorage from '@react-native-async-storage/async-storage';
import { claro, oscuro } from '@fuertia/tokens';
import { fireEvent, screen, waitFor } from '@testing-library/react-native';
import { StyleSheet } from 'react-native';

import { CLAVE_APARIENCIA } from '@/tema/ContextoTema';
import { PantallaHoy } from '@/pantallas/PantallaHoy';
import { renderConProveedores, respuestaJson } from './utilidades';

// Martes 3 de noviembre de 2026, a mediodía para que la zona horaria no cambie el día.
const HOY = new Date(2026, 10, 3, 12, 0, 0);

const espiaFetch = jest.spyOn(globalThis, 'fetch');

/** Color de fondo de la pantalla (el ScrollView), para saber qué tema se está viendo. */
function fondoPantalla(): string | undefined {
  const scroll = screen.getByTestId('pantalla-hoy');
  return StyleSheet.flatten(scroll.props.style)?.backgroundColor;
}

beforeEach(async () => {
  espiaFetch.mockReset();
  await AsyncStorage.clear();
});

describe('Pantalla Hoy', () => {
  test('saluda y muestra la fecha en español de Colombia', async () => {
    espiaFetch.mockResolvedValue(respuestaJson({ nombre: 'Fuertia', version: '0.1.0', estado: 'ok' }));
    await renderConProveedores(<PantallaHoy hoy={HOY} />);

    expect(screen.getByRole('header', { name: 'Hola' })).toBeOnTheScreen();
    expect(screen.getByText('Martes 3 de noviembre')).toBeOnTheScreen();
    expect(await screen.findByText(/Conectado con el backend/)).toBeOnTheScreen();
  });

  test('muestra que está conectado y la versión cuando el backend responde', async () => {
    espiaFetch.mockResolvedValue(respuestaJson({ nombre: 'Fuertia', version: '0.1.0', estado: 'ok' }));
    await renderConProveedores(<PantallaHoy hoy={HOY} />);

    expect(await screen.findByText('Conectado con el backend · versión 0.1.0')).toBeOnTheScreen();
    expect(screen.queryByRole('button', { name: /Reintentar/ })).toBeNull();

    // La llamada va al endpoint del contrato.
    const solicitud = espiaFetch.mock.calls[0][0] as Request;
    expect(solicitud.url).toBe('http://localhost:8080/api/v1/sistema/estado');
    expect(solicitud.method).toBe('GET');
  });

  test('muestra el error y deja reintentar cuando no hay conexión', async () => {
    // fetch falla como cuando el backend está apagado o la IP está mal.
    espiaFetch.mockRejectedValueOnce(new TypeError('Network request failed'));
    await renderConProveedores(<PantallaHoy hoy={HOY} />);

    expect(await screen.findByText('No hay conexión con el backend')).toBeOnTheScreen();
    const reintentar = screen.getByRole('button', { name: 'Reintentar la conexión con el backend' });

    // Al reintentar, el backend ya responde.
    espiaFetch.mockResolvedValueOnce(respuestaJson({ nombre: 'Fuertia', version: '0.2.0', estado: 'ok' }));
    await fireEvent.press(reintentar);

    expect(await screen.findByText('Conectado con el backend · versión 0.2.0')).toBeOnTheScreen();
    expect(espiaFetch).toHaveBeenCalledTimes(2);
  });

  test('también trata como error una respuesta 500 del backend', async () => {
    espiaFetch.mockResolvedValue(respuestaJson({ title: 'Error interno', status: 500 }, 500));
    await renderConProveedores(<PantallaHoy hoy={HOY} />);

    expect(await screen.findByText('No hay conexión con el backend')).toBeOnTheScreen();
  });
});

describe('Selector de apariencia', () => {
  beforeEach(() => {
    espiaFetch.mockResolvedValue(respuestaJson({ nombre: 'Fuertia', version: '0.1.0', estado: 'ok' }));
  });

  test('cambia el tema y guarda la elección', async () => {
    await renderConProveedores(<PantallaHoy hoy={HOY} />);
    await screen.findByText(/Conectado con el backend/);

    // Por defecto sigue al sistema, y en las pruebas el sistema está en claro.
    expect(screen.getByRole('radio', { name: 'Sistema' })).toBeChecked();
    expect(fondoPantalla()).toBe(claro.fondo);

    await fireEvent.press(screen.getByRole('radio', { name: 'Oscuro' }));
    expect(screen.getByRole('radio', { name: 'Oscuro' })).toBeChecked();
    expect(screen.getByRole('radio', { name: 'Sistema' })).not.toBeChecked();
    expect(fondoPantalla()).toBe(oscuro.fondo);
    expect(await AsyncStorage.getItem(CLAVE_APARIENCIA)).toBe('oscuro');

    await fireEvent.press(screen.getByRole('radio', { name: 'Claro' }));
    expect(fondoPantalla()).toBe(claro.fondo);
    expect(await AsyncStorage.getItem(CLAVE_APARIENCIA)).toBe('claro');
  });

  test('al abrir usa la apariencia que se guardó antes', async () => {
    await AsyncStorage.setItem(CLAVE_APARIENCIA, 'oscuro');
    await renderConProveedores(<PantallaHoy hoy={HOY} />);

    await waitFor(() => expect(screen.getByRole('radio', { name: 'Oscuro' })).toBeChecked());
    expect(fondoPantalla()).toBe(oscuro.fondo);
  });

  test('cada opción mide al menos 44 px de alto', async () => {
    await renderConProveedores(<PantallaHoy hoy={HOY} />);
    for (const nombre of ['Claro', 'Oscuro', 'Sistema']) {
      const estilo = StyleSheet.flatten(screen.getByRole('radio', { name: nombre }).props.style);
      expect(estilo.minHeight).toBeGreaterThanOrEqual(44);
    }
  });
});
