import { medidas, sombra } from '@fuertia/tokens';
import { Platform, View, type ViewProps } from 'react-native';

import { useTema } from '@/tema/ContextoTema';

/**
 * Tarjeta blanca (o gris en oscuro). Cambia según la plataforma, como en el diseño:
 * en iOS lleva un borde de 1 px (guías de Apple) y en Android una sombra (Material).
 */
export function Tarjeta({ style, ...resto }: ViewProps) {
  const { colores, tema } = useTema();
  const estiloPlataforma =
    Platform.OS === 'android'
      ? {
          borderRadius: medidas.radios.tarjeta,
          elevation: 2,
          // En oscuro la sombra es negra: con el color de claro quedaba con tinte rojizo.
          shadowColor: colores.sombra,
          shadowOpacity: sombra[tema].tarjeta,
        }
      : {
          borderRadius: medidas.radios.grande,
          borderWidth: 1,
          borderColor: colores.linea,
        };
  return (
    <View
      {...resto}
      style={[
        { backgroundColor: colores.tarjeta, padding: medidas.espacios.l, gap: medidas.espacios.m },
        estiloPlataforma,
        style,
      ]}
    />
  );
}
