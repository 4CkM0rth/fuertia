# 01 · Monorepo, tokens de diseño, evaluación del escaneo, Terraform y CI

Semana 1 · 3 de noviembre de 2026

## 1. Qué se construyó y para qué sirve

La base sobre la que se construye todo Fuertia:

- **Monorepo:** un solo repositorio con `backend/`, `web/`, `app/`, `packages/tokens/`, `tools/eval-escaneo/`, `infra/` y `docs/`. Una función completa (API, web y app) queda en una sola rama y una sola revisión.
- **Archivo de marca** (`fuertia.config.json`): nombre, bundle id y package name en un solo lugar.
- **Tokens de diseño** (`packages/tokens/`): los colores, la tipografía y las medidas de claro y oscuro, compartidos por la web y la app.
- **Contrato de API** (`docs/api/openapi.yaml`): lo que el backend promete y lo que la web y la app consumen.
- **Servicios locales** (`docker-compose.yml`): PostgreSQL, Redis y MinIO, que reemplazan a AWS hasta la semana 7.
- **Script de evaluación del escaneo** (`tools/eval-escaneo/`): mide si Gemini acierta los gramos antes de construir el escaneo.
- **Terraform** (`infra/terraform/`): la infraestructura de AWS escrita como código, sin aplicar.
- **CI** (`.github/workflows/ci.yml`): compila y prueba todo en cada PR.

## 2. Archivos y qué hace cada uno

| Archivo | Qué hace |
| --- | --- |
| `fuertia.config.json` | Fuente única de la marca. La app lo lee en `app.config.ts` |
| `package.json` (raíz) | Declara los *workspaces* de npm: tokens, web y app comparten una sola instalación |
| `packages/tokens/src/index.ts` | Paletas `claro` y `oscuro`, `tipografia`, `medidas`, y funciones para variables CSS y contraste |
| `packages/tokens/test/tokens.test.ts` | 31 pruebas: contraste AA, mismas llaves en los dos temas, sin tinte rojizo en oscuro |
| `docs/api/openapi.yaml` | Contrato: `GET /api/v1/sistema/estado` y `POST /api/v1/identidad/registro` |
| `docker-compose.yml` | Levanta PostgreSQL, Redis, MinIO y crea los buckets privados |
| `tools/eval-escaneo/eval_escaneo.py` | Lee fotos y CSV, llama a Gemini, empareja nombres y calcula errores |
| `tools/eval-escaneo/test_eval_escaneo.py` | 16 pruebas sin internet, con proveedor falso y llamada simulada |
| `infra/terraform/modulos/*` | Red, datos, almacenamiento, servicio y web, cada uno reutilizable |
| `infra/terraform/ambientes/pruebas/main.tf` | Usa los módulos con tamaños pequeños para el ambiente de pruebas |
| `.github/workflows/ci.yml` | Cuatro jobs en paralelo: backend, node (tokens, web y app), eval-escaneo e infra |

## 3. Conceptos nuevos

**Token de diseño.** Un nombre con significado que guarda un valor. En lugar de escribir `#7A1E2C` en cada botón, se escribe `primario`:

```ts
export const claro = { primario: '#7A1E2C', /* ... */ } as const;
export const oscuro: Paleta = { primario: '#D4A537', /* ... */ };
```

En oscuro, `primario` vale dorado. El botón no cambia su código: cambia el valor del token. Y como `oscuro` tiene el tipo `Paleta` (las mismas llaves que `claro`), si alguien agrega un token en claro y lo olvida en oscuro, TypeScript no compila.

**Contraste WCAG.** Es la relación de luminosidad entre el texto y su fondo. AA pide 4.5 o más para texto normal. La función `contraste()` lo calcula y las pruebas revisan cada pareja que usa el diseño:

```ts
['textoSecundario', 'tarjeta'],   // #6E4D52 sobre #FFFFFF en claro
```

**Workspaces de npm.** Varios paquetes en un repositorio que se instalan juntos. La web declara `"@fuertia/tokens": "*"` y npm la enlaza a `packages/tokens/` sin publicarlo en internet.

**Contrato primero (OpenAPI).** Se escribe primero qué responde la API y después se construye. El backend lo cumple, y la web genera sus tipos desde el contrato. Si el backend cambia un campo sin cambiar el contrato, la web lo nota al compilar.

**Puerto y adaptador, en pequeño.** El script de evaluación tiene un `Proveedor` (el puerto) con dos implementaciones (los adaptadores):

```python
class Proveedor(Protocol):
    def estimar(self, foto: Path) -> Estimacion: ...

class ProveedorGemini: ...   # el real, llama a la API
class ProveedorFalso: ...    # para pruebas, sin internet
```

El resto del código no sabe cuál está usando. Es la misma idea que tendrá el backend con `AnalizadorDeComida`.

**Inyección de dependencias para probar sin internet.** `ProveedorGemini` recibe la función que abre la conexión. En la prueba se le pasa una que devuelve una respuesta simulada:

```python
def abrir(solicitud, timeout):
    return RespuestaSimulada(json.dumps(respuesta_gemini([...])).encode())
ev.ProveedorGemini("k", "m", abrir=abrir).estimar(foto)
```

**Infraestructura como código (Terraform).** La red, la base de datos y los buckets se describen en archivos `.tf`. `terraform plan` muestra qué crearía y `terraform apply` lo crea y empieza a cobrar. Por eso el CI solo corre `terraform validate`, que revisa el código sin tocar AWS.

**CI (integración continua).** GitHub corre las pruebas en un servidor limpio en cada PR. Si algo falla, el PR queda en rojo. Así "en mi máquina funciona" deja de ser suficiente.

## 4. Decisiones y alternativas descartadas

| Decisión | Por qué | Descartado |
| --- | --- | --- |
| Spring Boot 4.1 | La 3.5 ya no recibe parches gratis | Spring Boot 3.5 |
| Local con Docker Compose hasta la semana 7 | No hay cuenta de AWS y no se gasta antes de tiempo | Crear AWS desde la semana 1 |
| Tokens en TypeScript sin compilar | Vite y Metro leen TypeScript directo; no hay paso de build | Publicar un paquete compilado |
| Colores oscuros sacados de las pantallas | La tabla de `04-diseno.md` no tenía todos los grises | Inventar los faltantes |
| Script en Python sin librerías | Corre sin instalar nada | Node, o Python con el SDK de Google |
| Modelo de Gemini por variable de entorno | Los nombres de versión cambian | Dejarlo fijo en el código |
| Sin NAT Gateway en pruebas | Ahorra unos USD 32 al mes | NAT en subredes privadas |
| Fotos en `temporales/` y `conservadas/` | S3 borra por prefijo, no por "lo que no esté marcado" | Una etiqueta por foto |

## 5. Cómo probarlo tú mismo

1. Tokens: `npm test -w @fuertia/tokens` desde la raíz. Deben pasar las 31 pruebas.
2. Rompe una a propósito: en `packages/tokens/src/index.ts` cambia `textoSecundario` de claro a `#C4ABAF`. Corre otra vez y mira cómo fallan las pruebas de contraste. Luego devuélvelo.
3. Evaluación con datos falsos:
   ```bash
   cd tools/eval-escaneo
   python eval_escaneo.py --fotos ejemplo/fotos --csv ejemplo/gramos.csv --proveedor falso
   ```
4. Cuando tengas la clave: copia `.env.example` como `.env`, pon `GEMINI_API_KEY` y corre con tus fotos en `datos/`.
5. Servicios locales: `docker compose up -d` y abre la consola de MinIO en http://localhost:9001 (usuario `fuertia`, contraseña `fuertia_local`). Ahí verás los dos buckets.

## 6. Para confirmar que lo entendiste

1. Si mañana el diseño cambia el dorado del modo oscuro, ¿cuántos archivos hay que tocar en la web y en la app? ¿Por qué?
2. En el script, Gemini responde "arroz" y el CSV dice "arroz blanco". ¿Qué hace `emparejar()` y por qué lo acepta aunque la similitud sea baja?
3. Ejercicio: agrega a `ejemplo/gramos.csv` un plato `p04` con un alimento que el proveedor falso no conozca. ¿Qué crees que pasa? Córrelo y compruébalo.
