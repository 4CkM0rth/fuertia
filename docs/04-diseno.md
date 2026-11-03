# Diseño (aprobado)

El diseño final de cada pantalla está en `design/screens/`. Cada `X.dc.html` es el modo claro y `XOscuro.dc.html` es el modo oscuro. Abre el HTML para tomar la estructura, los textos, los espaciados y los colores exactos. Los valores entre `{{...}}` son datos de ejemplo.

## Tipografía

Poppins, con pesos 400, 500, 600, 700 y 800. Texto mínimo de 12 px y objetivos táctiles de 44 px o más.

## Colores, modo claro (vinotinto)

| Token | Valor |
| --- | --- |
| fondo | #FAF6F4 |
| tarjeta | #FFFFFF |
| texto | #2A1216 |
| texto secundario | #6E4D52 |
| líneas | #EBDDD8, #E5D3CE, #F2E7E4 |
| primario | #7A1E2C (hover #5C1521) |
| dorado | #D4A537 |
| fondo dorado suave | #F6E9C9 |
| error | #8A2416 |

## Colores, modo oscuro (gris neutro con un solo acento dorado)

Sin tinte rojizo. El dorado se usa solo en el botón principal, el progreso, lo seleccionado y los enlaces.

| Token | Valor |
| --- | --- |
| fondo | #111214 |
| tarjeta | #1C1D20 |
| elevado | #26272B |
| héroe | #25262A |
| seleccionado neutro | #4A4B52 |
| texto | #EDEEF0 |
| texto secundario | #A7AAB1 |
| enlaces | #E2C171 |
| botón principal | #D4A537 con texto #141516 |

## Reglas de diseño

- Las fotos de perfil no llevan marco ni borde de color.
- La portada del entrenador es opcional. Si no hay portada, no se muestra la franja.
- La lámina de técnica y la vista previa del PDF se quedan en blanco también en modo oscuro.
- El PDF siempre va en claro.
- iOS sigue las guías de Apple. Android usa Material: flecha atrás, botones tipo píldora y tarjetas con sombra.

## Inventario de pantallas

Las pantallas de Android llevan el prefijo `Android` (por ejemplo `AndroidHoy`) y equivalen a las de iOS.

**App del asesorado (iOS)**

| Archivo | Pantalla |
| --- | --- |
| Main | Hoy |
| Entrenamiento | Entrenamiento del día y registro de series |
| SesionHibrida | Sesión híbrida (Hyrox) con temporizador en vivo |
| Descanso | Temporizador de descanso |
| Tecnica | Técnica del ejercicio |
| Records | Historial y récords |
| ComidasDia | Comidas del día |
| Escanear | Escanear comida: Foto, Buscar, Código ("Pronto") |
| Resultado | Resultado del escaneo y corrección |
| BuscarAlimento | Buscar alimento |
| ListaMercado | Lista de mercado |
| Receta | Receta (versión 1.1) |
| Progreso | Progreso |
| Composicion | Composición corporal |
| NuevoChequeo | Nuevo chequeo |
| CheckIn | Check-in semanal (versión 1.1) |
| Recordatorios | Recordatorios |
| Invitacion | Ingreso por invitación |
| Consentimientos | Consentimientos |
| Perfil | Perfil, privacidad, apariencia y borrado de cuenta |
| PerfilEntrenador | Perfil del entrenador visto por el asesorado |

**Web del entrenador**

| Archivo | Página |
| --- | --- |
| WebLogin | Ingreso |
| WebDashboard | Dashboard con bandeja de revisión |
| WebAsesorados | Lista de asesorados |
| WebAsesorado | Ficha del asesorado |
| WebEjercicios | Biblioteca de ejercicios por disciplina |
| WebRutina | Editor de rutinas por bloques |
| WebPlan | Editor del plan de alimentación |
| WebAlimentos | Base de alimentos |
| WebPlantillas | Plantillas |
| WebProgresion | Progresión de cargas (versión 1.1) |
| WebReporte | Reporte mensual (versión 1.1) |
| WebPerfilPublico | Perfil público del entrenador |
| WebAjustes | Ajustes del negocio y suscripción |
| WebFotoPlato | Foto del plato (fase 2) |

**PDF:** PdfPortada, PdfResumen, PdfEntrenamiento, PdfEntrenamiento2, PdfEntrenamientoGimnasio (variante solo gimnasio), PdfAlimentacion, PdfChequeos, PdfNotas.

**Pendiente de diseñar:** la pantalla de importar desde Excel (web), con plantilla descargable, pegar desde la hoja, vista previa con errores por fila y confirmación. Sigue el estilo de WebAlimentos.
