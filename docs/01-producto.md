# Producto: Fuertia (Coaching integral)

## Problema

Los entrenadores personales en Colombia planifican con Excel y Word y hacen seguimiento por WhatsApp. Las apps del mercado (Everfit, Trainerize, TrueCoach, Harbiz) están en inglés o cobran en dólares o euros, no conocen la comida colombiana y no cobran en pesos. Sebastian habló con 10 entrenadores y todos trabajan así. La validación del problema ya está hecha: no la repitas.

Propuesta: lo que hoy haces en Excel, Word y WhatsApp, en una sola herramienta premium, en español y a precio colombiano.

## Roles

- **Entrenador:** trabaja en el panel web (que también debe verse bien en el celular). Paga la suscripción en la web con Wompi.
- **Asesorado:** usa la app móvil. Siempre es gratis.
- **Admin:** soporte interno.

## Alcance del MVP

| Función | Notas |
| --- | --- |
| Cuentas, roles, tenants, invitación por enlace | Un tenant por entrenador o equipo |
| Consentimientos por tipo y versión | Datos personales, datos de salud, fotos para IA. Se pueden revocar |
| Perfil del negocio | Nombre, logo, color, equipo |
| Perfil del asesorado | Foto de perfil opcional; sin foto se muestran las iniciales |
| Base de ejercicios propia por disciplina | Fuerza, funcional, CrossFit, Hyrox, running, ciclismo, natación, movilidad, híbridos. Cada ejercicio define qué se registra: peso y reps, tiempo, distancia y ritmo, o rondas. Arranca con unos 150 |
| Editor de rutinas por bloques | Calentamiento, fuerza, metcon, estaciones, carrera, técnica, enfriamiento. Columnas: bloque, ejercicio, prescripción, qué se registra, técnica |
| Registro de series y sesiones | Incluye sesiones híbridas con temporizador en vivo y temporizador de descanso |
| Técnica del ejercicio | Video o fotos del entrenador. El entrenador elige el rango que se muestra. La animación generada pasa a la fase 2 |
| Plan de alimentación con opciones | Cada comida tiene 2 o 3 opciones con gramos y macros, más meta diaria |
| Base de alimentos | USDA, tabla del ICBF (revisar licencia) y alimentos propios del entrenador |
| Diario de comidas | Registro por foto, búsqueda o marcando la opción del plan |
| Escaneo de comida con IA | Foto, borrador con alimentos y gramos, el asesorado confirma o corrige, el entrenador revisa. Código de barras: "Pronto" |
| Lista de mercado automática | Sale del plan de alimentación |
| Chequeos | Peso, bioimpedancia (grasa, masa muscular, masa magra, agua, grasa visceral, masa ósea, metabolismo basal, edad metabólica), pliegues y perímetros: cuello, pecho, cintura, cadera, brazo, muñeca, pierna y pantorrilla. IMC, cintura/cadera y cintura/estatura calculados |
| Progreso | Gráficas, historial y récords por ejercicio |
| Recordatorios | Notificaciones push |
| Plantillas | De rutinas y planes |
| Importar desde Excel | Ejercicios, alimentos, rutinas y planes. Plantilla descargable o pegar desde la hoja, con vista previa y validación antes de guardar |
| Exportar PDF | Ver abajo |
| Perfil del entrenador | Foto, logo y portada opcional (sin portada no hay franja), equipo, bio, especialidades, formación, casos reales con datos verificados por la app y permiso del asesorado. Enlace público opcional |
| Suscripción del entrenador | Wompi solo en la web, con webhooks firmados |
| Borrado de cuenta dentro de la app | Obligatorio para Apple |
| Modo claro y oscuro | En app y web |

### PDF exportable

- Tamaño carta, siempre en modo claro, con el logo y el color del entrenador.
- Páginas: portada, resumen, plan de entrenamiento (las páginas que haga falta), plan de alimentación, historial de chequeos, y notas y próximos pasos.
- El entrenador elige las secciones y el rango de fechas.
- Solo aparecen las disciplinas que el entrenador asignó. Si el plan es solo de gimnasio, sale solo fuerza. Si es fuerza más running, salen las dos, y así con cualquier deporte. "Rendimiento por disciplina", en el resumen, sigue la misma regla.
- Pie de página: "Valores de comida aproximados según la base de alimentos. Este informe no reemplaza el consejo médico."
- Se genera por cola y worker y queda en S3 privado, con enlace firmado de 24 horas. Se comparte por WhatsApp.
- Sin fotos de platos hasta la fase 2.

## Planes y precios (aprobados el 3 de noviembre de 2026)

Precios en pesos colombianos, con IVA incluido y redondos (sin terminaciones en 900). Se cobra mensual, semestral (10% menos) o anual (2 meses gratis). El asesorado nunca paga.

| Plan | Asesorados | Incluye | Mensual | Semestral | Anual |
| --- | --- | --- | --- | --- | --- |
| Gratis | Hasta 3 | Rutinas, plan de alimentación, PDF con la marca de Fuertia | $0 | | |
| Básico | Hasta 10 | Lo anterior, más PDF con el logo del entrenador e importar desde Excel | $40.000 | $216.000 | $400.000 |
| Pro (el más elegido) | Ilimitados | Lo anterior, más escaneo con IA (60 por asesorado al mes), perfil público con casos reales y soporte prioritario | $80.000 | $432.000 | $800.000 |
| Equipo (versión 1.1) | Ilimitados | Todo el Pro, más varias cuentas de entrenador en el mismo negocio y la vista del dueño | $60.000 por entrenador, desde 3 | | |

Reglas para el código:

- Los límites (asesorados, escaneos y funciones) se leen de una tabla de planes, no van fijos en el código.
- Al llegar al límite, el panel web le muestra al entrenador qué plan lo resuelve. La app del asesorado nunca muestra precios.
- Precio de fundador: los primeros 20 entrenadores conservan su precio para siempre. Guarda en la suscripción el precio con el que entró cada uno.
- El escaneo usa Gemini Flash-Lite por defecto, para que el costo por escaneo sea bajo.

## Versión 1.1 (después del MVP)

Check-in semanal con alertas, recetas paso a paso, progresión sugerida de cargas, reporte mensual automático en PDF y el plan Equipo. Las cuatro primeras ya están diseñadas.

## Fase 2

App móvil del entrenador con compra dentro de la app (Apple y Google Play), fotos de platos con IA, mejor animación de técnica (más cuadros, imagen a video, modelos 3D), borrador de plan con IA, Apple Salud y Health Connect, fotos de progreso, código de barras con Open Food Facts y chat interno.

Descartado por ahora: agenda de sesiones presenciales.

## Reglas de Apple que afectan el producto

- La app del asesorado no muestra precios, planes, botones de pago ni enlaces a la web de pagos.
- Los entrenadores se registran y pagan solo en la web.
- Cuenta de prueba con datos de ejemplo para el revisor de Apple.
- Borrado de cuenta dentro de la app.
- El escaneo explica sus datos y su método y se muestra siempre como estimado.

## Flujos principales

1. **Entrenador nuevo (web):** crea cuenta y verifica el correo, configura el negocio (nombre, logo, color), crea o importa ejercicios y alimentos, crea un asesorado, envía la invitación por enlace y, si quiere, paga el plan Pro con Wompi (el webhook activa el plan).
2. **Asesorado nuevo (app):** abre el enlace, crea su cuenta, lee la política, autoriza datos personales y, de forma opcional, datos de salud (que activan medidas y escaneo) y ve su plan de hoy.
3. **Un día del asesorado:** pantalla Hoy, entrenamiento (marca series con peso, reps y esfuerzo, y comenta al final), comidas (foto, búsqueda o la opción del plan) y progreso.
4. **Revisión del entrenador:** dashboard con bandeja (escaneos corregidos, sesiones y chequeos nuevos), ficha del asesorado, ajustes y notas. El asesorado recibe una notificación.
5. **Exportar PDF:** elegir asesorado, secciones y fechas, ver la vista previa, generar (cola y worker, HTML a PDF, S3) y obtener el enlace firmado.
