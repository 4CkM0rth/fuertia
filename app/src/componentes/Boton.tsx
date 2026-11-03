import { medidas } from '@fuertia/tokens';
import { Platform, Pressable, type PressableProps } from 'react-native';

import { useTema } from '@/tema/ContextoTema';
import { Texto } from './Texto';

type Props = Omit<PressableProps, 'children'> & {
  texto: string;
  /** Lo que lee el lector de pantalla. Si no se da, se usa el texto visible. */
  etiqueta?: string;
};

/**
 * Botón principal. En Android es una píldora (Material) y en iOS un rectángulo redondeado
 * (guías de Apple). En los dos mide al menos 44 px de alto para que sea fácil de tocar.
 */
export function Boton({ texto, etiqueta, style, ...resto }: Props) {
  const { colores } = useTema();
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={etiqueta ?? texto}
      // Android muestra la onda de Material al tocar; iOS baja la opacidad.
      android_ripple={{ color: colores.primarioHover }}
      {...resto}
      style={(estado) => [
        {
          // Apple pide 44 pt como mínimo y Material 48 dp; se usa el mayor de cada plataforma.
          minHeight: Platform.OS === 'android' ? 48 : medidas.tactilMinimo,
          paddingHorizontal: medidas.espacios.xxl,
          paddingVertical: medidas.espacios.s,
          borderRadius: Platform.OS === 'android' ? 999 : medidas.radios.medio,
          backgroundColor: colores.primario,
          alignItems: 'center',
          justifyContent: 'center',
          alignSelf: 'flex-start',
          overflow: 'hidden',
          opacity: Platform.OS === 'ios' && estado.pressed ? 0.7 : 1,
        },
        typeof style === 'function' ? style(estado) : style,
      ]}>
      <Texto peso="negrita" color="textoSobrePrimario">
        {texto}
      </Texto>
    </Pressable>
  );
}
