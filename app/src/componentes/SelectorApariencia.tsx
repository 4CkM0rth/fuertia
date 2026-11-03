import { medidas } from '@fuertia/tokens';
import { Pressable, View } from 'react-native';

import { APARIENCIAS, useTema, type Apariencia } from '@/tema/ContextoTema';
import { Texto } from './Texto';

const ETIQUETAS: Record<Apariencia, string> = {
  claro: 'Claro',
  oscuro: 'Oscuro',
  sistema: 'Sistema',
};

/**
 * Control segmentado Claro / Oscuro / Sistema, como el de la pantalla Perfil.
 * Para el lector de pantalla es un grupo de opciones donde solo una queda marcada.
 */
export function SelectorApariencia() {
  const { apariencia, colores, cambiarApariencia } = useTema();
  return (
    <View
      accessibilityRole="radiogroup"
      accessibilityLabel="Apariencia"
      style={{
        flexDirection: 'row',
        gap: medidas.espacios.xs,
        padding: medidas.espacios.xs,
        borderRadius: medidas.radios.medio,
        backgroundColor: colores.elevado,
      }}>
      {APARIENCIAS.map((opcion) => {
        const elegida = opcion === apariencia;
        return (
          <Pressable
            key={opcion}
            accessibilityRole="radio"
            accessibilityLabel={ETIQUETAS[opcion]}
            accessibilityState={{ checked: elegida, selected: elegida }}
            onPress={() => cambiarApariencia(opcion)}
            style={{
              flex: 1,
              // En el diseño el segmento mide 36 px; se sube a 44 px por la regla de objetivos táctiles.
              minHeight: medidas.tactilMinimo,
              borderRadius: medidas.radios.medio - medidas.espacios.xs,
              alignItems: 'center',
              justifyContent: 'center',
              paddingHorizontal: medidas.espacios.xs,
              backgroundColor: elegida ? colores.tarjeta : 'transparent',
            }}>
            <Texto tamano="secundario" peso={elegida ? 'extranegrita' : 'seminegrita'}>
              {ETIQUETAS[opcion]}
            </Texto>
          </Pressable>
        );
      })}
    </View>
  );
}
