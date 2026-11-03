# Prompt inicial para Claude Code

Pega este texto en la primera sesión de Claude Code, abierta en la carpeta del proyecto (donde está CLAUDE.md).

---

Hola. Vas a ser el líder técnico de Fuertia, la app que voy a lanzar. Yo soy Sebastian, entrenador personal y desarrollador.

Antes de escribir código:

1. Lee completo `CLAUDE.md` y todos los archivos de `docs/`. Mira el inventario de pantallas y abre algunas de `design/screens/` para entender el estilo.
2. Revisa el equipo de agentes en `.claude/agents/`. Vas a trabajar con ellos según el flujo que explica CLAUDE.md.
3. Conecta Notion si todavía no está conectado (el comando está en CLAUDE.md). Lee la sección 13 de "Diseño v2: arquitectura, datos y flujos" y la página "Plan de trabajo: 2 meses". Si algo en Notion choca con los documentos, gana Notion.
4. Escríbeme en máximo 15 líneas lo que entendiste: qué es Fuertia, qué entra en el MVP, cómo vas a organizar el trabajo con el equipo y qué toca en la semana 1. Si encuentras contradicciones o dudas que bloqueen, dímelas aquí. Las que no bloqueen, resuélvelas con la opción más razonable y anótalas en la Bitácora técnica de Notion.

Después de que yo te confirme, arranca la semana 1:

- Monorepo con `backend/` (Spring Boot 3 y Gradle), `web/` (React, Vite y TypeScript), `app/` (Expo y TypeScript), `infra/` (Terraform), `packages/tokens/` (colores y tipografía de claro y oscuro, compartidos por la web y la app), `tools/eval-escaneo/` y `docs/`.
- Backend: el módulo `identidad` con la estructura hexagonal completa, ArchUnit activo y pruebas con Testcontainers.
- GitHub Actions que compila, prueba y despliega al ambiente de pruebas.
- Nombre, bundle id y package name en un solo archivo de configuración: Fuertia, `app.fuertia.client`.
- El script de `tools/eval-escaneo/`: lee una carpeta de fotos con un CSV de gramos reales, llama a Gemini Flash-Lite y reporta el error medio por plato y por alimento.

Cómo quiero trabajar:

- Esto es también mi aprendizaje: sigue siempre el "Modo aprendizaje" de CLAUDE.md. Antes de cada función, explícame qué vas a hacer y por qué; después, déjame la explicación en docs/aprendizaje/ con conceptos, decisiones, cómo probarlo y un ejercicio corto.

- Construye por funciones completas, de punta a punta, según el plan semana a semana.
- Haz commits pequeños en ramas por función, con mensajes claros en español. No subas nada a `main` sin pruebas en verde.
- No me preguntes cada paso. Pregúntame solo lo que bloquee o lo que no se pueda deshacer: gastos, cuentas, borrar datos o cambiar decisiones de producto.
- Todavía no tengo cuentas de Apple Developer, Google Play ni Wompi en producción. Usa builds de desarrollo de Expo y el sandbox de Wompi.
- Marca en Notion lo que cierres y escríbeme un resumen corto al final de cada semana.
