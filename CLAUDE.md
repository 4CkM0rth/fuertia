# Fuertia: Coaching integral

**Fuertia** es una app para que entrenadores personales planifiquen entrenamiento (de cualquier deporte) y alimentación de sus asesorados, y les hagan seguimiento. Reemplaza lo que hoy hacen con Excel, Word y WhatsApp. Primer mercado: Neiva, Colombia. Todo en español de Colombia.

- **App del asesorado:** Expo / React Native, iPhone y Android con la misma prioridad.
- **Panel web del entrenador:** React + TypeScript.
- **Backend:** Spring Boot 4.1 (Java 21), arquitectura hexagonal, multi-tenant. Se aprobó la 4.1 el 3 de noviembre de 2026 porque la 3.5 ya no recibe parches gratis.
- **Local antes de AWS:** hasta la semana 6 todo corre con `docker-compose.yml` (PostgreSQL, Redis y MinIO en lugar de S3). AWS se crea en la semana 7; el CI compila y prueba, sin desplegar.

El dueño del producto es Sebastian Rojas (entrenador personal con 6 años de experiencia y desarrollador). Las decisiones de producto y diseño ya están tomadas: no las reabras sin que él lo pida.

En Notion el proyecto todavía aparece como "1 · FitCoach", que fue su nombre de trabajo. Es el mismo proyecto.

## Fuentes de verdad

Lee esto antes de escribir código, en este orden:

1. `docs/01-producto.md`: alcance del MVP, roles, funciones y flujos.
2. `docs/02-arquitectura.md`: stack, módulos hexagonales, escaneo con IA, PDF, pagos y seguridad.
3. `docs/03-datos.md`: modelo de datos y reglas.
4. `docs/04-diseno.md`: sistema visual (claro y oscuro) e inventario de pantallas.
5. `docs/05-plan.md`: plan de 9 semanas, orden de construcción y qué se recorta si hay atraso.
6. `design/screens/*.dc.html`: el diseño final de cada pantalla. Son HTML de referencia (los `{{...}}` son datos de ejemplo). Ábrelos para copiar estructura, textos, espaciados y colores. No se usan como código de producción.

Notion es la fuente de verdad viva (ver "Notion" abajo). Si algo en estos archivos choca con Notion, gana Notion. Avísale a Sebastian si encuentras una contradicción.

## Notion

Conexión en Claude Code (una sola vez):

```
claude mcp add --transport http notion https://mcp.notion.com/mcp
```

Luego ejecuta `/mcp` y autentica con la cuenta de Notion de Sebastian.

Páginas:

| Página | ID | Para qué |
| --- | --- | --- |
| 1 · FitCoach (raíz del proyecto) | 407b733267dc81569203f1517c991b74 | Página madre |
| Diseño v2: arquitectura, datos y flujos | 408b733267dc81f6bb00c90956dcd2a2 | Especificación completa. La sección 13 tiene el estado, las decisiones y los pendientes |
| Plan de trabajo: 2 meses | 40cb733267dc810a99acc249feafa84f | Calendario semana a semana |

Cómo usar Notion:

- Al empezar una sesión, lee la sección 13 de "Diseño v2" y la semana actual del plan.
- Al cerrar una tarea, márcala en Notion. Haz ediciones pequeñas y puntuales, nunca reescribas páginas enteras.
- Las decisiones técnicas nuevas van en una página hija "Bitácora técnica" bajo 1 · FitCoach (créala si no existe): fecha, decisión y por qué.
- No toques otras páginas del espacio de Sebastian.

## Equipo de agentes

Los agentes no son carpetas: son los archivos de `.claude/agents/`. Las carpetas del código (`backend/`, `web/`, `app/`, etc.) las creas tú en la semana 1.


Tú (la sesión principal) eres el **líder técnico**: planificas cada función, defines el contrato, delegas, integras, verificas y actualizas Notion. El equipo está en `.claude/agents/`:

| Agente | Qué hace | Dónde trabaja |
| --- | --- | --- |
| `backend-dev` | Módulos hexagonales, migraciones, API y workers | `backend/`, `docs/api/` |
| `web-dev` | Panel web del entrenador | `web/` |
| `mobile-dev` | App del asesorado en iPhone y Android | `app/` |
| `qa-tester` | Pruebas de punta a punta y criterios de la semana | Pruebas en todo el repositorio |
| `security-reviewer` | Seguridad, aislamiento por tenant y Ley 1581. Solo revisa | Todo, sin editar |
| `design-reviewer` | Fidelidad al diseño, claro y oscuro, y accesibilidad. Solo revisa | Todo, sin editar |

Flujo para cada función, en este orden:

1. **Líder:** lee la semana actual del plan en Notion y define el alcance de la función y su contrato de API en `docs/api/`.
2. **`backend-dev`:** construye el módulo y los endpoints con sus pruebas.
3. **`web-dev` y `mobile-dev`:** construyen sus pantallas. Pueden ir en paralelo porque trabajan en carpetas distintas.
4. **`qa-tester`:** prueba de punta a punta con los criterios de la semana.
5. **`security-reviewer`**, si la función toca datos de salud, fotos, pagos o permisos, y **`design-reviewer`**, si hay pantallas nuevas.
6. **Líder:** corrige o delega las correcciones, hace commit en la rama de la función, marca lo cerrado en Notion y anota las decisiones en la Bitácora técnica.

Reglas del equipo:
- Nunca pongas a dos agentes a editar la misma carpeta al mismo tiempo.
- Ningún agente cambia el contrato de API sin pasar por el líder.
- Cada agente termina su resumen con una sección "Para entender" (ver Modo aprendizaje).
- Si un agente encuentra una contradicción con los documentos o con Notion, se detiene y te la reporta. Tú se la pasas a Sebastian si no puedes resolverla.
- Al final de cada semana, escríbele a Sebastian un resumen corto: qué se cerró, qué se movió y qué riesgo ves.

## Modo aprendizaje (obligatorio)

Sebastian quiere entender todo lo que se construye; esto es parte de su aprendizaje. Explica siempre, en español, de forma simple y natural, sin guiones largos (—).

- **Antes de cada función:** explícale en pocas líneas qué vas a construir, cómo encaja en la arquitectura y por qué se hace así.
- **Después de cada función:** escribe una explicación en `docs/aprendizaje/NN-nombre-de-la-funcion.md` con:
  1. Qué se construyó y para qué sirve en Fuertia.
  2. Los archivos que se crearon o cambiaron, y qué hace cada uno.
  3. Los conceptos nuevos que aparecieron (por ejemplo: puerto, adaptador, migración, JWT, Testcontainers o hooks de React), cada uno explicado con un ejemplo del propio código.
  4. Las decisiones que se tomaron, por qué, y qué alternativa se descartó.
  5. Cómo probarlo él mismo, paso a paso.
  6. Dos o tres preguntas o un ejercicio corto para que confirme que lo entendió.
- **Código comentado donde aporta:** comenta el porqué de las partes no obvias, no lo evidente.
- **Si Sebastian pregunta algo, explícalo sin prisa**, aunque frene el avance. Entender es más importante que la velocidad.
- **Cada agente** incluye en su resumen final una sección "Para entender", con lo que hizo explicado para alguien que está aprendiendo.
- Al final de cada semana, agrega una página en Notion, dentro de "Bitácora de aprendizaje" (bajo 1 · FitCoach; créala si no existe), con el resumen de lo aprendido esa semana y los enlaces a los archivos de `docs/aprendizaje/`.

## Reglas de trabajo

- Construye por funciones completas, de punta a punta: backend, web y app de una misma función en la misma semana, según `docs/05-plan.md`.
- Hexagonal estricta, con la misma estructura en todos los módulos. Activa ArchUnit desde el primer commit.
- Todas las tablas de negocio llevan `tenant_id`, con un filtro global en los adaptadores de persistencia.
- La IA nunca calcula macros: solo propone alimentos y gramos. Los macros los calcula el código con la base de alimentos.
- Cada función nueva lleva sus pruebas: unitarias del dominio y de integración con Testcontainers.
- Textos de interfaz en español de Colombia, simples y naturales. Sin guiones largos (—) en los textos. Unidades métricas, pesos colombianos y fechas como "2 de noviembre de 2026".
- Accesibilidad: objetivos táctiles de 44 px o más, contraste AA y texto dinámico en iOS.
- Modo claro y oscuro en todo. El asesorado elige Claro, Oscuro o Sistema.
- Commits pequeños con mensajes claros en español. Ramas por función. No hagas push a `main` sin pruebas en verde.
- Nada de secretos en el repositorio. Usa variables de entorno y AWS Secrets Manager.

## Marca

- **Nombre:** Fuertia. En las tiendas: "Fuertia: Coaching integral" (26 caracteres; el límite es 30).
- **Descripción corta:** Coaching integral. En la web y en el PDF se puede usar "Planificación deportiva".
- **Dominio:** fuertia.app (Sebastian lo registra). fuertia.com está ocupado.
- **Identificadores sugeridos:** bundle id de iOS `app.fuertia.client`, package de Android `app.fuertia.client` y paquete Java `app.fuertia`. Déjalos en un solo archivo de configuración.
- **Diseño:** el lienzo y las pantallas de `design/screens/` todavía dicen "FitCoach" en algunos textos. En el código usa siempre "Fuertia".

## Lo que todavía no existe

- **Logo.** Usa las iniciales del entrenador sobre su color, como en el diseño.
- **Cuentas de Apple Developer y Google Play.** Sebastian las crea cuando la app esté al 90%. Mientras tanto se prueba con builds de desarrollo de Expo en simuladores y en el celular propio (Android por APK interno; iPhone con aprovisionamiento gratuito de Xcode, que dura 7 días).
- **Cuenta de Wompi en producción.** Usa el sandbox de Wompi.
