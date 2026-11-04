import { tipografia } from '@fuertia/tokens';
// Se importa cada peso por separado para empaquetar solo los 5 archivos que usa el diseño
// (el índice del paquete trae los 18 estilos de Poppins).
import { Poppins_400Regular } from '@expo-google-fonts/poppins/400Regular';
import { Poppins_500Medium } from '@expo-google-fonts/poppins/500Medium';
import { Poppins_600SemiBold } from '@expo-google-fonts/poppins/600SemiBold';
import { Poppins_700Bold } from '@expo-google-fonts/poppins/700Bold';
import { Poppins_800ExtraBold } from '@expo-google-fonts/poppins/800ExtraBold';

export type Peso = keyof typeof tipografia.pesos;

// En React Native una fuente propia no cambia de grosor con fontWeight (Android lo ignora):
// cada peso es un archivo y se elige por su nombre de familia. Este mapa traduce el peso
// del token al nombre con el que se registra la fuente en useFonts.
export const familiaPorPeso: Record<Peso, string> = {
  regular: 'Poppins_400Regular',
  medio: 'Poppins_500Medium',
  seminegrita: 'Poppins_600SemiBold',
  negrita: 'Poppins_700Bold',
  extranegrita: 'Poppins_800ExtraBold',
};

export const archivosFuentes = {
  [familiaPorPeso.regular]: Poppins_400Regular,
  [familiaPorPeso.medio]: Poppins_500Medium,
  [familiaPorPeso.seminegrita]: Poppins_600SemiBold,
  [familiaPorPeso.negrita]: Poppins_700Bold,
  [familiaPorPeso.extranegrita]: Poppins_800ExtraBold,
};
