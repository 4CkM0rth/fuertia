import { medidas } from '@fuertia/tokens';
import { ActivityIndicator, View } from 'react-native';

import { useEstadoSistema } from '@/api/useEstadoSistema';
import { useTema } from '@/tema/ContextoTema';
import { Boton } from './Boton';
import { Tarjeta } from './Tarjeta';
import { Texto } from './Texto';

/** Muestra si la app logra hablar con el backend (GET /api/v1/sistema/estado). */
export function TarjetaEstadoSistema() {
  const { colores } = useTema();
  const consulta = useEstadoSistema();

  return (
    <Tarjeta>
      <Texto tamano="destacado" peso="negrita" accessibilityRole="header">
        Estado del sistema
      </Texto>

      {/* accessibilityLiveRegion hace que Android lea el cambio de estado sin tocar nada. */}
      <View
        accessibilityLiveRegion="polite"
        style={{ flexDirection: 'row', alignItems: 'center', gap: medidas.espacios.s }}>
        {consulta.isPending ? (
          <>
            <ActivityIndicator color={colores.primario} accessibilityLabel="Comprobando conexión" />
            <Texto color="textoSecundario">Comprobando conexión</Texto>
          </>
        ) : consulta.isSuccess ? (
          <>
            <View
              accessibilityElementsHidden
              importantForAccessibility="no"
              style={{ width: 10, height: 10, borderRadius: 5, backgroundColor: colores.exito }}
            />
            <Texto style={{ flexShrink: 1 }}>
              Conectado con el backend · versión {consulta.data.version}
            </Texto>
          </>
        ) : (
          <>
            <View
              accessibilityElementsHidden
              importantForAccessibility="no"
              style={{ width: 10, height: 10, borderRadius: 5, backgroundColor: colores.error }}
            />
            <Texto peso="seminegrita" color="error" style={{ flexShrink: 1 }}>
              No hay conexión con el backend
            </Texto>
          </>
        )}
      </View>

      {consulta.isError ? (
        <>
          <Texto tamano="secundario" color="textoSecundario">
            Revisa que el backend esté encendido. En un celular físico, la dirección debe ser la
            IP de tu computador.
          </Texto>
          <Boton
            texto={consulta.isFetching ? 'Reintentando' : 'Reintentar'}
            etiqueta="Reintentar la conexión con el backend"
            disabled={consulta.isFetching}
            onPress={() => consulta.refetch()}
          />
        </>
      ) : null}
    </Tarjeta>
  );
}
