import { medidas } from '@fuertia/tokens';
import { Platform, ScrollView, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { SelectorApariencia } from '@/componentes/SelectorApariencia';
import { Tarjeta } from '@/componentes/Tarjeta';
import { TarjetaEstadoSistema } from '@/componentes/TarjetaEstadoSistema';
import { TarjetaHeroe } from '@/componentes/TarjetaHeroe';
import { Texto } from '@/componentes/Texto';
import { useTema } from '@/tema/ContextoTema';
import { fechaLarga } from '@/utilidades/fecha';

type Props = {
  /** Se puede fijar la fecha en las pruebas; en la app es hoy. */
  hoy?: Date;
};

/**
 * Pantalla inicial de la semana 1, con el estilo de Main (iOS) y AndroidHoy.
 * Va dentro de un ScrollView para que nada se corte cuando la letra del teléfono está grande.
 */
export function PantallaHoy({ hoy = new Date() }: Props) {
  const { colores } = useTema();
  const margenes = useSafeAreaInsets();
  const esAndroid = Platform.OS === 'android';
  const fecha = fechaLarga(hoy);

  return (
    <ScrollView
      testID="pantalla-hoy"
      style={{ flex: 1, backgroundColor: colores.fondo }}
      contentContainerStyle={{
        paddingTop: margenes.top + (esAndroid ? medidas.espacios.l : medidas.espacios.xxl),
        paddingBottom: margenes.bottom + medidas.espacios.xxl,
        paddingHorizontal: esAndroid ? medidas.espacios.l : medidas.espacios.xl,
        gap: esAndroid ? medidas.espacios.l : medidas.espacios.lxl,
      }}>
      {/* iOS pone la fecha arriba y el saludo grande; Android, el saludo y la fecha debajo (Material). */}
      <View style={{ gap: 2 }}>
        {esAndroid ? (
          <>
            <Texto tamano="saludo" peso="negrita" accessibilityRole="header">
              Hola
            </Texto>
            <Texto tamano="secundario" color="textoSecundario">
              {fecha}
            </Texto>
          </>
        ) : (
          <>
            <Texto tamano="apoyo" peso="medio" color="textoSecundario">
              {fecha}
            </Texto>
            <Texto
              tamano="titulo"
              peso="extranegrita"
              accessibilityRole="header"
              style={{ letterSpacing: -0.5 }}>
              Hola
            </Texto>
          </>
        )}
      </View>

      <TarjetaHeroe />

      <TarjetaEstadoSistema />

      <Tarjeta>
        <Texto peso="seminegrita">
          Apariencia
        </Texto>
        <SelectorApariencia />
      </Tarjeta>
    </ScrollView>
  );
}
