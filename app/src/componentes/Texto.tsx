import { tipografia, type NombreToken } from '@fuertia/tokens';
import { Text, type TextProps } from 'react-native';

import { useTema } from '@/tema/ContextoTema';
import { familiaPorPeso, type Peso } from '@/tema/fuentes';

type Props = TextProps & {
  tamano?: keyof typeof tipografia.tamanos;
  peso?: Peso;
  color?: NombreToken;
};

/**
 * Texto de Fuertia: Poppins, tamaños y colores solo desde los tokens.
 * No se toca allowFontScaling, así el texto crece si la persona aumenta la letra del teléfono
 * (texto dinámico en iOS y tamaño de fuente en Android).
 */
export function Texto({ tamano = 'cuerpo', peso = 'regular', color = 'texto', style, ...resto }: Props) {
  const { colores } = useTema();
  const tamanoPx = tipografia.tamanos[tamano];
  return (
    <Text
      {...resto}
      style={[
        {
          fontFamily: familiaPorPeso[peso],
          fontSize: tamanoPx,
          // Poppins tiene ascendentes altos; 1.35 evita que se recorten las tildes.
          lineHeight: Math.round(tamanoPx * 1.35),
          color: colores[color],
        },
        style,
      ]}
    />
  );
}
