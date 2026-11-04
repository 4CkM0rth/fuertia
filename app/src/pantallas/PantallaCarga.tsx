import { ActivityIndicator, View } from 'react-native';

import { useTema } from '@/tema/ContextoTema';

/**
 * Se ve mientras cargan las fuentes. Normalmente la tapa la pantalla de bienvenida nativa,
 * pero si esa se oculta antes (por ejemplo en Expo Go) la persona ve esto en vez de un blanco.
 * No usa <Texto> porque Poppins todavía no está lista.
 */
export function PantallaCarga() {
  const { colores } = useTema();
  return (
    <View
      accessible
      accessibilityLabel="Cargando"
      style={{ flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: colores.fondo }}>
      <ActivityIndicator size="large" color={colores.primario} />
    </View>
  );
}
