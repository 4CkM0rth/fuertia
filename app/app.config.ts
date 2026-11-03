import type { ConfigContext, ExpoConfig } from 'expo/config';

import { claro, oscuro } from '@fuertia/tokens';

// La marca sale de un solo archivo en la raíz del monorepo. Así el nombre, el esquema
// de enlaces y los identificadores de las tiendas no se escriben a mano en ningún otro lado.
// Se importa como JSON para que Expo lo lea al momento de generar la configuración
// (en `expo start`, `expo export` y en los builds de EAS).
import marca from '../fuertia.config.json';

export default ({ config }: ConfigContext): ExpoConfig => ({
  ...config,
  name: marca.nombre,
  slug: 'fuertia',
  scheme: marca.esquemaEnlaces,
  version: '0.1.0',
  // Solo iPhone y Android: el panel del entrenador es la web (web/), no esta app.
  platforms: ['ios', 'android'],
  orientation: 'portrait',
  icon: './assets/images/icon.png',
  // "automatic" deja que la app siga el modo del teléfono. La elección Claro / Oscuro / Sistema
  // del asesorado se aplica encima, desde el contexto de tema (src/tema).
  userInterfaceStyle: 'automatic',
  ios: {
    bundleIdentifier: marca.ios.bundleId,
    supportsTablet: false,
  },
  android: {
    package: marca.android.package,
    adaptiveIcon: {
      backgroundColor: claro.fondo,
      foregroundImage: './assets/images/android-icon-foreground.png',
      backgroundImage: './assets/images/android-icon-background.png',
      monochromeImage: './assets/images/android-icon-monochrome.png',
    },
    predictiveBackGestureEnabled: false,
  },
  plugins: [
    'expo-router',
    [
      'expo-splash-screen',
      {
        backgroundColor: claro.fondo,
        image: './assets/images/splash-icon.png',
        imageWidth: 76,
        dark: { backgroundColor: oscuro.fondo },
      },
    ],
  ],
  experiments: {
    typedRoutes: true,
    reactCompiler: true,
  },
});
