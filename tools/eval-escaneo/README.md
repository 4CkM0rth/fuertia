# Evaluación del escaneo con IA

Mide qué tan bien estima Gemini los gramos de un plato, antes de construir el escaneo en la semana 5.

**Criterio de aprobación:** error medio por plato menor al 25% en comidas simples. Si no se cumple, el escaneo sale en la versión simple: identifica los alimentos y los gramos se ponen a mano.

## Cómo correrlo

Necesitas Python 3.10 o más. No hay que instalar nada.

1. Copia `.env.example` como `.env` y pon tu `GEMINI_API_KEY`. El `.env` nunca se sube al repositorio.
2. Pon las fotos en `datos/fotos/` y los gramos en `datos/gramos.csv`. La carpeta `datos/` tampoco se sube, porque son fotos reales de asesorados.
3. Corre:

```bash
python eval_escaneo.py --fotos datos/fotos --csv datos/gramos.csv
```

Para probarlo sin clave y sin gastar, usa el proveedor falso con los datos de ejemplo:

```bash
python eval_escaneo.py --fotos ejemplo/fotos --csv ejemplo/gramos.csv --proveedor falso
```

## Formato del CSV

Una fila por cada alimento de cada plato. Los gramos se pueden escribir con coma o con punto.

```csv
plato,foto,alimento,gramos
p01,p01.jpg,arroz blanco,150
p01,p01.jpg,frijoles,120
```

Para Nutrition5k: cada plato trae el peso por ingrediente. Pásalo a este mismo formato (un plato por foto cenital) y córrelo igual.

## Qué reporta

- **Error por plato:** diferencia entre los gramos totales reales y los estimados.
- **Error por alimento:** el error de cada alimento, promediado en todos los platos donde aparece. Si la IA no detecta un alimento, cuenta como 100% de error.
- **Sobrantes:** alimentos que la IA vio pero no estaban en el CSV (quedan en el reporte JSON).
- **Tokens** de entrada y salida, para calcular el costo por escaneo.

El reporte queda en `reportes/` en JSON y CSV.

## Pruebas

```bash
python -m unittest -v
```
