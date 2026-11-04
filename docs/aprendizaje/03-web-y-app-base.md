# 03 · Base de la web y de la app

Semana 1 · 3 de noviembre de 2026

## 1. Qué se construyó y para qué sirve

La primera pantalla de cada cliente, con todo lo que van a usar las pantallas reales desde la semana 2:

- **Web del entrenador** (`web/`): Vite, React y TypeScript. Muestra la marca, el selector Claro, Oscuro o Sistema y una tarjeta "Estado del sistema" que llama al backend.
- **App del asesorado** (`app/`): Expo SDK 58 con Expo Router, para iPhone y Android. Muestra el saludo, la fecha, la tarjeta héroe, el estado del sistema y el selector de apariencia.

Las dos usan los mismos tokens de `packages/tokens/` y generan su cliente de API desde `docs/api/openapi.yaml`.

## 2. Archivos y qué hace cada uno

**Web**

| Archivo | Qué hace |
| --- | --- |
| `vite.config.ts` | Proxy de `/api` al backend, plugin que genera el CSS del tema y configuración de Vitest |
| `src/tema/cssDelTema.ts` | Convierte los tokens en variables CSS para claro y oscuro |
| `src/tema/SelectorTema.tsx` | Botones Claro, Oscuro y Sistema; guarda la elección en localStorage |
| `src/api/cliente.ts` | Cliente `openapi-fetch` tipado con `esquema.d.ts` |
| `src/api/esquema.d.ts` | Tipos generados desde el contrato (`npm run generar:api`) |
| `src/estado/useEstadoSistema.ts` | Hook de TanStack Query que pide el estado del backend |
| `src/estado/TarjetaEstado.tsx` | Muestra conectado, cargando o error con Reintentar |
| `src/pruebas/servidor.ts` | Backend falso con MSW para las pruebas |

**App**

| Archivo | Qué hace |
| --- | --- |
| `app.config.ts` | Configuración de Expo que lee la marca de `fuertia.config.json` |
| `eas.json` | Perfiles de build: development (APK de desarrollo), preview y production |
| `src/app/_layout.tsx` | Monta TanStack Query, el tema y la navegación |
| `src/app/index.tsx` | La ruta "/", que solo llama a `PantallaHoy` |
| `src/tema/ContextoTema.tsx` | Contexto con Claro, Oscuro y Sistema, guardado en AsyncStorage |
| `src/tema/fuentes.ts` | Los cinco pesos de Poppins |
| `src/pantallas/PantallaHoy.tsx` | La pantalla inicial |
| `src/componentes/` | Texto, Tarjeta, Boton, TarjetaHeroe, TarjetaEstadoSistema y SelectorApariencia |
| `pruebas/` | 8 pruebas con jest-expo y Testing Library |

## 3. Conceptos nuevos

**Vite.** Sirve la web mientras programas, con recarga instantánea, y la empaqueta para producción (`vite build` crea `dist/`). El proxy `'/api': 'http://localhost:8080'` hace que el navegador hable con un solo origen, así no hace falta CORS en local.

**Variables CSS desde los tokens.** El CSS nunca escribe un color, solo la variable:

```css
.marca__lema { color: var(--fu-texto-secundario); }
```

Para el modo oscuro no se toca ningún componente. Se redefinen las mismas variables bajo `:root[data-tema='oscuro']`, y cambiar `data-tema` en `<html>` cambia toda la página.

**Cliente generado desde OpenAPI.** `npm run generar:api` convierte el contrato en tipos. Con `api.GET('/api/v1/sistema/estado')`, si escribes mal la ruta no compila, y TypeScript ya sabe que `data.version` es texto.

**TanStack Query.** Maneja los datos que vienen del servidor: carga, error, caché y reintentos. El componente solo lee `isPending`, `isError` y `data`, y el botón Reintentar llama a `refetch()`.

**MSW (Mock Service Worker).** Un backend falso para las pruebas. `HttpResponse.error()` simula que el backend está caído, y la prueba espera ver "No hay conexión con el backend".

**Expo y Expo Router.** Expo es un conjunto de herramientas sobre React Native que evita tocar Xcode y Android Studio. Con Expo Router, cada archivo de `src/app/` es una pantalla: `index.tsx` es "/" y `_layout.tsx` envuelve a todas.

**app.config.ts leyendo la marca.**

```ts
import marca from '../fuertia.config.json';
ios: { bundleIdentifier: marca.ios.bundleId },
android: { package: marca.android.package },
```

**Expo Go, build de desarrollo y EAS.**

- **Expo Go:** una app genérica de las tiendas que carga tu código desde el computador. Sirve en el iPhone sin Mac ni cuenta, pero solo con los módulos nativos que ya trae.
- **Build de desarrollo:** tu propio "Expo Go", con el nombre y el package de Fuertia. Se instala una vez como APK.
- **EAS Build:** compila en la nube según los perfiles de `eas.json`.

**Contexto de React.** Comparte datos con toda la app sin pasarlos de componente en componente. Cualquier componente pide los colores con `const { colores } = useTema();`.

**Una sola copia de React.** En un monorepo, si la web y la app piden versiones distintas de React, npm instala dos copias y los hooks dejan de funcionar ("Invalid hook call"). La app usa la misma versión que la web, y así hay una sola.

## 4. Decisiones y alternativas descartadas

| Decisión | Por qué | Descartado |
| --- | --- | --- |
| Oxlint en la web | Es lo que trae ahora la plantilla de Vite y es mucho más rápido | ESLint |
| TypeScript 5.9 en la web | openapi-typescript todavía no funciona con TypeScript 7 | TypeScript 7 |
| Poppins empaquetada (`@fontsource`) | No depende de Google Fonts ni les pasa la IP de los entrenadores | Google Fonts por enlace |
| React 19.3.1 en la app | Una sola copia en el monorepo; frente a 19.3.0 solo cambia el número | La 19.3.0 que fija Expo |
| Preferencia de tema en AsyncStorage | Es una preferencia, no un secreto | SecureStore (queda para los tokens de sesión) |
| Selector de 44 px | Regla de objetivos táctiles, aunque el diseño de Perfil usa 36 | 36 px |

## 5. Cómo probarlo tú mismo

**Web**

```bash
npm run dev -w @fuertia/web
```

Abre la dirección que muestra Vite. Sin backend verás "No hay conexión con el backend". Prende el backend (guía 02) y oprime Reintentar.

**App en el iPhone (Expo Go)**

1. Instala Expo Go desde la App Store.
2. En `app/`, copia `.env.example` como `.env` y pon `EXPO_PUBLIC_API_URL=http://<IP-de-tu-PC>:8080`.
3. Corre `npx expo start --go` y escanea el QR con la cámara.

**App en Android**

- Con Android Studio: `npx expo run:android`.
- O en la nube (cuenta gratuita de Expo): `npx eas-cli@latest build --profile development --platform android`, y luego `npx expo start`.

**Pruebas**

```bash
npm test -w @fuertia/web
```

```bash
npm test -w @fuertia/app
```

## 6. Para confirmar que lo entendiste

1. Si el backend agrega un campo nuevo a `EstadoSistema`, ¿qué comando corres para que la web y la app lo conozcan, y qué archivo cambia?
2. ¿Por qué la preferencia de tema puede ir en AsyncStorage, pero el token de sesión de la semana 2 no?
3. Ejercicio: en la web, cambia en el sistema operativo el modo a oscuro con el selector en "Sistema". Luego elige "Claro". ¿Qué regla de `cssDelTema.ts` gana en cada caso?
