import { medidas } from '@fuertia/tokens';
import { Platform, View } from 'react-native';

import { useTema } from '@/tema/ContextoTema';
import { Texto } from './Texto';

/**
 * Tarjeta oscura destacada de la pantalla Hoy (tokens heroe*). En la semana 1 todavía no hay
 * plan asignado, así que muestra una bienvenida. Más adelante aquí van las calorías del día.
 */
export function TarjetaHeroe() {
  const { colores } = useTema();
  const esAndroid = Platform.OS === 'android';
  return (
    <View
      style={{
        backgroundColor: colores.heroe,
        // iOS usa la esquina grande del héroe; Android, la de tarjeta de Material.
        borderRadius: esAndroid ? medidas.radios.tarjeta : medidas.radios.heroe,
        padding: medidas.espacios.xl,
        gap: medidas.espacios.m,
      }}>
      <Texto
        tamano="secundario"
        peso="seminegrita"
        color="heroeTextoSecundario"
        // En iOS la etiqueta va en mayúsculas con espaciado, como en el diseño de Main.
        style={esAndroid ? undefined : { textTransform: 'uppercase', letterSpacing: 0.8 }}>
        Tu plan
      </Texto>
      <Texto tamano="titulo" peso="extranegrita" color="heroeTexto" accessibilityRole="header">
        Te damos la bienvenida
      </Texto>
      <Texto color="heroeTextoSecundario">
        Cuando tu entrenador te asigne el entrenamiento y la alimentación, los verás aquí.
      </Texto>
    </View>
  );
}
