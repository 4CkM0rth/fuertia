# Plan de trabajo: 2 meses (del 3 de noviembre de 2026 al 3 de enero de 2027)

La versión viva está en Notion, en "Plan de trabajo: 2 meses" (ID 40cb733267dc810a99acc249feafa84f). Marca ahí el avance.

Ritmo: 40 o más horas por semana. Cada domingo se revisa qué se cerró y qué se movió, y qué se recorta si hay atraso.

| Semana | Fechas | Qué se construye | Al terminar |
| --- | --- | --- | --- |
| 1 | 3 al 8 nov | Monorepo, CI con GitHub Actions, AWS de pruebas con Terraform, Spring Boot hexagonal con ArchUnit, PostgreSQL con Flyway, proyecto Expo y proyecto React. Script de prueba de Gemini | Las tres partes se despliegan solas |
| 2 | 9 al 15 nov | Identidad (JWT y roles), tenants, invitación, consentimientos, perfil del negocio, perfil del asesorado con foto opcional. Primera corrida de la prueba de escaneo | Un entrenador invita y el asesorado entra y firma |
| 3 | 16 al 22 nov | Base de ejercicios (primeros 150), editor de rutinas por bloques, asignación, Hoy, entrenamiento del día, registro de series, descanso | El asesorado entrena con su rutina |
| 4 | 23 al 29 nov | Base de alimentos, editor del plan con opciones, comidas del día, buscar alimento, lista de mercado. Importar desde Excel: ejercicios y alimentos | El asesorado sigue su plan |
| 5 | 30 nov al 6 dic | Escaneo con IA de punta a punta y bandeja de revisión | Foto, borrador, corrección y revisión |
| 6 | 7 al 13 dic | Chequeos, composición, progreso, récords, push, dashboard, ficha, plantillas. Importar desde Excel: rutinas y planes | El entrenador sigue a sus asesorados desde la web |
| 7 | 14 al 20 dic | PDF, perfil público, Wompi con webhooks, borrado de cuenta, revisión del modo oscuro. App al 90%: Sebastian crea las cuentas de Apple y Google Play | El entrenador paga, exporta y comparte |
| 8 | 21 al 27 dic | Seguridad, pruebas, k6, Sentry, privacidad y términos. TestFlight y prueba interna de Google Play. Beta con 3 entrenadores en Neiva | Beta real |
| 9 | 28 dic al 3 ene | Arreglos de la beta, ficha de las tiendas, cuenta de prueba para el revisor, envío | Enviada a las dos tiendas |

Festivos: 16 de noviembre, 8 de diciembre, 25 de diciembre y 1 de enero.

## Antes de tener las cuentas de las tiendas

- Android: builds de desarrollo de Expo y APK interno.
- iPhone: simulador y aprovisionamiento gratuito de Xcode en el celular propio, que dura 7 días.
- Deja EAS configurado para que, apenas existan las cuentas, sea solo agregar credenciales.

## Orden de recorte si hay atraso

1. Perfil público con casos reales.
2. Plantillas.
3. Lista de mercado.
4. Importar rutinas y planes desde Excel (se queda solo ejercicios y alimentos).
5. Historial y récords.

No se recorta: cuentas e invitaciones, consentimientos, rutinas y registro de series, plan de alimentación, escaneo con IA, chequeos, PDF, pagos y borrado de cuenta.

## Cómo empezar (semana 1)

1. Crea el monorepo: `backend/` (Spring Boot, Gradle), `web/` (React con Vite y TypeScript), `app/` (Expo con TypeScript), `infra/` (Terraform) y `docs/` (esta carpeta).
2. Backend: el esqueleto del módulo `identidad` con la estructura hexagonal completa, las pruebas de ArchUnit y Testcontainers con PostgreSQL.
3. Tokens de diseño compartidos (claro y oscuro) en un paquete común que usen la web y la app.
4. Pipeline de GitHub Actions: compila, prueba y despliega al ambiente de pruebas.
5. Script de evaluación del escaneo (`tools/eval-escaneo/`): lee una carpeta de fotos con su CSV de gramos reales, llama a Gemini y reporta el error medio por plato y por alimento.
6. Marca en Notion lo que cierres y anota las decisiones en la Bitácora técnica.
