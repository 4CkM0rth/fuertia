import { claro, medidas } from '@fuertia/tokens';
import { Platform, StyleSheet, View, type ViewProps } from 'react-native';

import { useTema } from '@/tema/ContextoTema';

/**
 * Tarjeta blanca (o gris en oscuro). Cambia según la plataforma, como en el diseño:
 * en iOS lleva un borde fino (guías de Apple) y en Android una sombra (Material).
 */
export function Tarjeta({ style, ...resto }: ViewProps) {
  const { colores, tema } = useTema();
  const estiloPlataforma =
    Platform.OS === 'android'
      ? {
          borderRadius: medidas.radios.tarjeta,
          elevation: 2,
          // Falta un token de sombra en @fuertia/tokens. El diseño usa el texto de claro con
          // transparencia (rgba(42,18,22,0.12)), así que se toma ese color del token.
          shadowColor: claro.texto,
        }
      : {
          borderRadius: medidas.radios.grande,
          borderWidth: StyleSheet.hairlineWidth * 2,
          // En el diseño la línea de la tarjeta es "linea" en claro y "lineaSuave" en oscuro.
          borderColor: tema === 'oscuro' ? colores.lineaSuave : colores.linea,
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
