import AsyncStorage from '@react-native-async-storage/async-storage';
import { temas, type NombreTema, type Paleta } from '@fuertia/tokens';
import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import { Appearance, useColorScheme } from 'react-native';

/** Lo que elige el asesorado en su perfil. "sistema" sigue el modo del teléfono. */
export type Apariencia = 'claro' | 'oscuro' | 'sistema';

export const APARIENCIAS: readonly Apariencia[] = ['claro', 'oscuro', 'sistema'];

// Es una preferencia visual, no un secreto: por eso va en AsyncStorage.
// Los tokens de sesión (semana 2) irán en expo-secure-store, nunca aquí.
export const CLAVE_APARIENCIA = 'fuertia.apariencia';

type ValorTema = {
  /** La elección guardada: claro, oscuro o sistema. */
  apariencia: Apariencia;
  /** El tema que se ve de verdad, ya resuelto (sistema se convierte en claro u oscuro). */
  tema: NombreTema;
  /** Los colores del tema activo. Toda la app pinta con esto, nunca con colores sueltos. */
  colores: Paleta;
  /** false mientras se lee la preferencia guardada (evita un parpadeo de tema al abrir). */
  listo: boolean;
  cambiarApariencia: (nueva: Apariencia) => void;
};

const ContextoTema = createContext<ValorTema | null>(null);

function esApariencia(valor: unknown): valor is Apariencia {
  return typeof valor === 'string' && (APARIENCIAS as readonly string[]).includes(valor);
}

export function ProveedorTema({ children }: { children: ReactNode }) {
  const [apariencia, setApariencia] = useState<Apariencia>('sistema');
  const [listo, setListo] = useState(false);
  // useColorScheme devuelve "light" o "dark" según el teléfono (o null si no lo sabe).
  const esquemaSistema = useColorScheme();

  // Al abrir la app se lee la elección guardada. Si falla, se queda en "sistema".
  useEffect(() => {
    let vigente = true;
    AsyncStorage.getItem(CLAVE_APARIENCIA)
      .then((guardada) => {
        if (vigente && esApariencia(guardada)) setApariencia(guardada);
      })
      .catch(() => {})
      .finally(() => {
        if (vigente) setListo(true);
      });
    return () => {
      vigente = false;
    };
  }, []);

  // Le avisamos también al sistema para que lo nativo (alertas, teclado, selectores de fecha)
  // use el mismo modo que la app. "auto" quita el ajuste y vuelve a seguir al teléfono.
  useEffect(() => {
    const ajuste = apariencia === 'claro' ? 'light' : apariencia === 'oscuro' ? 'dark' : 'auto';
    try {
      Appearance.setColorScheme(ajuste);
    } catch {
      // En algunos entornos (pruebas) no existe el módulo nativo; la app funciona igual.
    }
  }, [apariencia]);

  const tema: NombreTema =
    apariencia === 'sistema' ? (esquemaSistema === 'dark' ? 'oscuro' : 'claro') : apariencia;

  const cambiarApariencia = (nueva: Apariencia) => {
    setApariencia(nueva);
    AsyncStorage.setItem(CLAVE_APARIENCIA, nueva).catch(() => {
      // Si no se pudo guardar, la elección vale mientras la app siga abierta.
    });
  };

  return (
    <ContextoTema.Provider
      value={{ apariencia, tema, colores: temas[tema], listo, cambiarApariencia }}>
      {children}
    </ContextoTema.Provider>
  );
}

export function useTema(): ValorTema {
  const valor = useContext(ContextoTema);
  if (valor === null) {
    throw new Error('useTema se usa dentro de <ProveedorTema>.');
  }
  return valor;
}
