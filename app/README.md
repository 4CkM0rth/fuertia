# App del asesorado (Fuertia)

App de Fuertia para iPhone y Android, hecha con Expo (SDK 58), React Native 0.88 y Expo Router.
Es un workspace de npm del monorepo: el paquete se llama `@fuertia/app` y usa los colores de
`@fuertia/tokens` (`packages/tokens`).

## Antes de empezar

Desde la **raíz del repositorio** (no desde `app/`), una sola vez:

```bash
npm install
```

npm instala todo el monorepo de una vez (tokens, web y app) y deja una sola copia de React.

Copia `app/.env.example` como `app/.env` y ajusta la dirección del backend:

```bash
EXPO_PUBLIC_API_URL=http://localhost:8080
```

- En el **simulador de iOS** o el **emulador de Android** sirve `localhost` (en el emulador también `http://10.0.2.2:8080`).
- En un **celular físico**, `localhost` es el propio celular. Pon la IP de tu computador en la red wifi,
  por ejemplo `http://192.168.1.20:8080`. En Windows la ves con `ipconfig` (Dirección IPv4 del adaptador wifi).
  El celular y el computador deben estar en la misma red, y el firewall de Windows debe dejar pasar el puerto 8080.
- Después de cambiar `.env`, reinicia Metro con `npx expo start --clear`: las variables `EXPO_PUBLIC_*`
  se escriben dentro del código al empaquetar.

El backend se levanta aparte (ver `backend/` y `docker-compose.yml`). Si no está encendido, la app
muestra "No hay conexión con el backend" y un botón Reintentar.

## iPhone con Expo Go (sin Mac y sin cuenta de Apple)

1. Instala **Expo Go** desde la App Store en el iPhone.
2. En `app/`, corre:

   ```bash
   npx expo start --go
   ```

   El `--go` importa: como la app tiene `expo-dev-client` (para el APK de Android), `npx expo start`
   sin nada prepara el QR para el build de desarrollo. Si ya lo arrancaste sin `--go`, oprime `s`
   en la terminal para cambiar a Expo Go.

3. Escanea el código QR con la cámara del iPhone. Se abre en Expo Go.

Si el celular no encuentra el computador (redes con aislamiento de clientes, VPN), usa
`npx expo start --tunnel`.

Expo Go solo trae los módulos nativos que incluye Expo. Todo lo que usa la app hoy funciona ahí.
Cuando agreguemos algo nativo que Expo Go no tenga, el iPhone necesitará un build de desarrollo
(con cuenta de Apple, o con Xcode y aprovisionamiento gratuito en un Mac).

## Android con build de desarrollo (APK interno)

Un build de desarrollo es tu propia versión de "Expo Go": una app instalable con el nombre, el
ícono y los módulos nativos de Fuertia, que carga el código desde Metro en tu computador.
Se instala una vez; después solo corres `npx expo start` y los cambios llegan al momento.

Hay dos formas de generar el APK:

**A. En la nube con EAS** (requiere una cuenta gratuita de Expo, no la de Google Play):

```bash
npx eas-cli@latest login
npx eas-cli@latest build --profile development --platform android
```

Al terminar, EAS da un enlace y un QR para descargar el APK en el celular. La primera vez pregunta
si crea el proyecto en EAS y una llave de firma de Android: acepta, EAS la guarda.

**B. En tu computador con Android Studio** (sin cuenta):

```bash
npx expo run:android
```

Necesita Android Studio con el SDK de Android y un emulador, o un celular conectado por USB con la
depuración USB activa. Genera las carpetas `android/` (están en `.gitignore`: no se suben).

Luego, con el APK instalado:

```bash
npx expo start
```

y abre la app Fuertia en el celular.

### Perfiles de `eas.json`

| Perfil | Para qué | Resultado |
| --- | --- | --- |
| `development` | Desarrollar en el celular, con el menú de desarrollo | APK interno que carga el código desde Metro |
| `preview` | Probar una versión cerrada sin Metro | APK interno con el código adentro |
| `production` | Tiendas (cuando existan las cuentas de Apple y Google) | AAB para Play Store e IPA para App Store |

No hay credenciales en el repositorio. Las de las tiendas se agregan en EAS cuando Sebastian cree las cuentas.

## Comandos

Desde `app/` (o desde la raíz con `npm run <comando> -w @fuertia/app`):

| Comando | Qué hace |
| --- | --- |
| `npm start` | Arranca Metro para el build de desarrollo (`expo start`) |
| `npx expo start --go` | Arranca Metro para Expo Go (iPhone) |
| `npm run lint` | ESLint con las reglas de Expo |
| `npm run typecheck` | Revisa los tipos con TypeScript |
| `npm test` | Pruebas con Jest, jest-expo y Testing Library |
| `npm run generar:api` | Regenera `src/api/esquema.d.ts` desde `docs/api/openapi.yaml` |
| `npx expo-doctor` | Revisa versiones y configuración del proyecto |

## Estructura

```
app/
  app.config.ts        Configuración de Expo. Lee la marca de ../fuertia.config.json
  eas.json             Perfiles de build de EAS
  src/app/             Rutas de Expo Router (cada archivo es una pantalla)
  src/pantallas/       Pantallas, fuera de las rutas para poder probarlas
  src/componentes/     Texto, Tarjeta, Boton, selector de apariencia, tarjetas de Hoy
  src/tema/            Contexto de tema (Claro / Oscuro / Sistema) y fuentes Poppins
  src/api/             Cliente de la API generado desde el contrato y consultas
  pruebas/             Pruebas de Jest
```

## Notas del monorepo

- Expo configura Metro solo para monorepos (desde el SDK 52), por eso no hay `metro.config.js`.
  Metro mira la raíz del repositorio y resuelve `@fuertia/tokens` desde `packages/tokens`.
- React: el SDK 58 recomienda `react` 19.3.0 y la web usa 19.3.1. Con dos versiones, npm deja dos
  copias y Expo Doctor lo marca como duplicado (dos copias de React rompen los hooks). La app usa
  19.3.1 para que haya una sola copia en todo el monorepo; entre 19.3.0 y 19.3.1 el paquete `react`
  solo cambia el número de versión. Por eso `package.json` tiene `expo.install.exclude: ["react"]`,
  que le dice a Expo que no intente bajarla. Al subir de SDK, revisa esto de nuevo.
- La app no tiene versión web: el panel del entrenador es `web/`.
