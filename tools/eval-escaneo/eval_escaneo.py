"""Evaluación del escaneo de comida con IA.

Lee una carpeta de fotos y un CSV con los gramos reales de cada alimento, le pide a
Gemini (Flash-Lite por defecto) que estime alimentos y gramos, y reporta el error medio
por plato y por alimento. Con esto se decide si el escaneo sale completo en el MVP
(error medio menor al 25% en comidas simples) o en la versión simple (gramos a mano).

Uso:
    python eval_escaneo.py --fotos datos/fotos --csv datos/gramos.csv
    python eval_escaneo.py --fotos ejemplo/fotos --csv ejemplo/gramos.csv --proveedor falso

Formato del CSV (una fila por alimento de cada plato):
    plato,foto,alimento,gramos
    p01,p01.jpg,arroz blanco,150
    p01,p01.jpg,frijoles,120

Solo usa la librería estándar de Python, para que corra sin instalar nada.
"""

from __future__ import annotations

import argparse
import base64
import csv
import difflib
import json
import os
import random
import sys
import time
import unicodedata
import urllib.error
import urllib.request
from dataclasses import dataclass, field
from datetime import datetime
from pathlib import Path
from typing import Protocol

UMBRAL_APROBACION = 0.25
SIMILITUD_MINIMA = 0.6
MODELO_POR_DEFECTO = "gemini-flash-lite-latest"

PROMPT = (
    "Eres un asistente de nutrición. Mira la foto del plato e identifica cada alimento "
    "que se ve. Para cada uno estima los gramos servidos. Usa nombres simples en español "
    "de Colombia (por ejemplo: arroz blanco, frijoles, carne molida, aguacate, arepa, "
    "plátano maduro). No calcules calorías ni macros. Responde solo con el JSON pedido."
)

# Esquema de salida estructurada: Gemini responde siempre con esta forma.
ESQUEMA_RESPUESTA = {
    "type": "OBJECT",
    "properties": {
        "alimentos": {
            "type": "ARRAY",
            "items": {
                "type": "OBJECT",
                "properties": {
                    "nombre": {"type": "STRING"},
                    "gramos": {"type": "NUMBER"},
                    "confianza": {"type": "NUMBER"},
                },
                "required": ["nombre", "gramos", "confianza"],
            },
        }
    },
    "required": ["alimentos"],
}


# --------------------------------------------------------------------------- datos


@dataclass
class Plato:
    id: str
    foto: Path
    reales: dict[str, float] = field(default_factory=dict)

    @property
    def total_real(self) -> float:
        return sum(self.reales.values())


@dataclass
class Estimacion:
    alimentos: dict[str, float]
    entrada_tokens: int = 0
    salida_tokens: int = 0
    segundos: float = 0.0


def normalizar(nombre: str) -> str:
    """Minúsculas, sin tildes y sin espacios repetidos: 'Plátano  Maduro' -> 'platano maduro'."""
    sin_tildes = unicodedata.normalize("NFKD", nombre).encode("ascii", "ignore").decode()
    return " ".join(sin_tildes.lower().split())


def leer_platos(ruta_csv: Path, carpeta_fotos: Path) -> list[Plato]:
    platos: dict[str, Plato] = {}
    with ruta_csv.open(encoding="utf-8-sig", newline="") as archivo:
        lector = csv.DictReader(archivo)
        faltantes = {"plato", "foto", "alimento", "gramos"} - set(lector.fieldnames or [])
        if faltantes:
            raise ValueError(f"Al CSV le faltan columnas: {', '.join(sorted(faltantes))}")
        for numero, fila in enumerate(lector, start=2):
            try:
                gramos = float(fila["gramos"].replace(",", "."))
            except ValueError:
                raise ValueError(f"Fila {numero}: los gramos '{fila['gramos']}' no son un número")
            if gramos <= 0:
                raise ValueError(f"Fila {numero}: los gramos deben ser mayores a cero")
            plato = platos.setdefault(fila["plato"], Plato(fila["plato"], carpeta_fotos / fila["foto"]))
            alimento = normalizar(fila["alimento"])
            plato.reales[alimento] = plato.reales.get(alimento, 0) + gramos
    for plato in platos.values():
        if not plato.foto.is_file():
            raise FileNotFoundError(f"No encuentro la foto del plato {plato.id}: {plato.foto}")
    return list(platos.values())


# ---------------------------------------------------------------------- proveedores


class Proveedor(Protocol):
    nombre: str
    modelo: str

    def estimar(self, foto: Path) -> Estimacion: ...


class ProveedorGemini:
    """Llama a la API REST de Gemini con la foto y un esquema JSON de respuesta."""

    nombre = "gemini"
    URL = "https://generativelanguage.googleapis.com/v1beta/models/{modelo}:generateContent"

    def __init__(self, clave: str, modelo: str, abrir=urllib.request.urlopen, reintentos: int = 3):
        self.clave = clave
        self.modelo = modelo
        self._abrir = abrir  # se inyecta para poder probar sin internet
        self._reintentos = reintentos

    def construir_solicitud(self, foto: Path) -> urllib.request.Request:
        tipo = "image/png" if foto.suffix.lower() == ".png" else "image/jpeg"
        cuerpo = {
            "contents": [{
                "parts": [
                    {"text": PROMPT},
                    {"inline_data": {"mime_type": tipo, "data": base64.b64encode(foto.read_bytes()).decode()}},
                ]
            }],
            "generationConfig": {
                "responseMimeType": "application/json",
                "responseSchema": ESQUEMA_RESPUESTA,
                "temperature": 0,
            },
        }
        return urllib.request.Request(
            self.URL.format(modelo=self.modelo),
            data=json.dumps(cuerpo).encode(),
            # La clave va en un encabezado y no en la URL, para que no quede en registros.
            headers={"Content-Type": "application/json", "x-goog-api-key": self.clave},
            method="POST",
        )

    @staticmethod
    def interpretar(respuesta: dict) -> Estimacion:
        texto = respuesta["candidates"][0]["content"]["parts"][0]["text"]
        alimentos: dict[str, float] = {}
        for item in json.loads(texto).get("alimentos", []):
            nombre = normalizar(str(item.get("nombre", "")))
            if nombre:
                alimentos[nombre] = alimentos.get(nombre, 0) + max(float(item.get("gramos", 0)), 0)
        uso = respuesta.get("usageMetadata", {})
        return Estimacion(alimentos, uso.get("promptTokenCount", 0), uso.get("candidatesTokenCount", 0))

    def estimar(self, foto: Path) -> Estimacion:
        for intento in range(1, self._reintentos + 1):
            inicio = time.monotonic()
            try:
                with self._abrir(self.construir_solicitud(foto), timeout=60) as respuesta:
                    estimacion = self.interpretar(json.load(respuesta))
                estimacion.segundos = time.monotonic() - inicio
                return estimacion
            except urllib.error.HTTPError as error:
                # 429 (límite) y 5xx se reintentan con espera creciente; lo demás es un error real.
                if error.code not in (429, 500, 502, 503, 504) or intento == self._reintentos:
                    raise RuntimeError(f"Gemini respondió {error.code}: {error.read().decode(errors='ignore')[:300]}")
                time.sleep(2 ** intento)
        raise RuntimeError("Gemini no respondió")


class ProveedorFalso:
    """Proveedor de prueba: devuelve los gramos reales con un error aleatorio fijo por semilla.

    Sirve para probar el script sin clave de Gemini y sin gastar.
    """

    nombre = "falso"

    def __init__(self, platos: list[Plato], error_maximo: float = 0.3, semilla: int = 7):
        self.modelo = f"falso-error-{int(error_maximo * 100)}"
        self._por_foto = {plato.foto.resolve(): plato for plato in platos}
        self._error = error_maximo
        self._azar = random.Random(semilla)

    def estimar(self, foto: Path) -> Estimacion:
        plato = self._por_foto[foto.resolve()]
        alimentos = {
            nombre: round(gramos * (1 + self._azar.uniform(-self._error, self._error)), 1)
            for nombre, gramos in plato.reales.items()
        }
        return Estimacion(alimentos)


# -------------------------------------------------------------------------- métricas


def emparejar(reales: dict[str, float], estimados: dict[str, float]) -> tuple[dict[str, str], list[str]]:
    """Empareja cada alimento real con el estimado de nombre más parecido.

    Devuelve {alimento_real: alimento_estimado} y la lista de estimados que sobraron
    (alimentos que la IA vio pero no estaban en el plato).
    """
    libres = set(estimados)
    parejas: dict[str, str] = {}
    candidatos = sorted(
        ((difflib.SequenceMatcher(None, real, est).ratio(), real, est) for real in reales for est in estimados),
        reverse=True,
    )
    for similitud, real, est in candidatos:
        # Si un nombre contiene al otro ("arroz" y "arroz blanco") se acepta aunque la similitud sea baja.
        contiene = real in est or est in real
        if real in parejas or est not in libres or (similitud < SIMILITUD_MINIMA and not contiene):
            continue
        parejas[real] = est
        libres.discard(est)
    return parejas, sorted(libres)


def error_relativo(estimado: float, real: float) -> float:
    return abs(estimado - real) / real


@dataclass
class ResultadoPlato:
    plato: str
    total_real: float
    total_estimado: float
    error_total: float
    error_medio_alimentos: float
    no_detectados: list[str]
    sobrantes: list[str]
    detalle: list[dict]
    entrada_tokens: int
    salida_tokens: int
    segundos: float


def evaluar_plato(plato: Plato, estimacion: Estimacion) -> ResultadoPlato:
    parejas, sobrantes = emparejar(plato.reales, estimacion.alimentos)
    detalle = []
    for real, gramos in plato.reales.items():
        estimado = estimacion.alimentos.get(parejas.get(real, ""), 0.0)
        detalle.append({
            "alimento": real,
            "detectado_como": parejas.get(real),
            "gramos_reales": gramos,
            "gramos_estimados": estimado,
            "error": error_relativo(estimado, gramos),  # no detectado = error de 100%
        })
    total_estimado = sum(d["gramos_estimados"] for d in detalle)
    return ResultadoPlato(
        plato=plato.id,
        total_real=plato.total_real,
        total_estimado=total_estimado,
        error_total=error_relativo(total_estimado, plato.total_real),
        error_medio_alimentos=sum(d["error"] for d in detalle) / len(detalle),
        no_detectados=[d["alimento"] for d in detalle if d["detectado_como"] is None],
        sobrantes=sobrantes,
        detalle=detalle,
        entrada_tokens=estimacion.entrada_tokens,
        salida_tokens=estimacion.salida_tokens,
        segundos=estimacion.segundos,
    )


def resumir(resultados: list[ResultadoPlato]) -> dict:
    por_alimento: dict[str, list[float]] = {}
    for resultado in resultados:
        for d in resultado.detalle:
            por_alimento.setdefault(d["alimento"], []).append(d["error"])
    error_platos = sum(r.error_total for r in resultados) / len(resultados)
    return {
        "platos": len(resultados),
        "error_medio_por_plato": error_platos,
        "error_medio_por_alimento": sum(r.error_medio_alimentos for r in resultados) / len(resultados),
        "aprobado": error_platos < UMBRAL_APROBACION,
        "alimentos": {
            nombre: {"apariciones": len(errores), "error_medio": sum(errores) / len(errores)}
            for nombre, errores in sorted(por_alimento.items(), key=lambda par: -sum(par[1]) / len(par[1]))
        },
        "entrada_tokens": sum(r.entrada_tokens for r in resultados),
        "salida_tokens": sum(r.salida_tokens for r in resultados),
    }


# --------------------------------------------------------------------------- reporte


def pct(valor: float) -> str:
    return f"{valor * 100:.1f}%".replace(".", ",")


def imprimir_reporte(resultados: list[ResultadoPlato], resumen: dict, proveedor: Proveedor, salida=sys.stdout) -> None:
    p = lambda texto="": print(texto, file=salida)
    p(f"Evaluación del escaneo · {proveedor.nombre} · {proveedor.modelo}")
    p()
    p(f"{'Plato':<10}{'Real (g)':>10}{'IA (g)':>10}{'Error plato':>13}{'Error alim.':>13}  No detectados")
    for r in resultados:
        p(f"{r.plato:<10}{r.total_real:>10.0f}{r.total_estimado:>10.0f}{pct(r.error_total):>13}"
          f"{pct(r.error_medio_alimentos):>13}  {', '.join(r.no_detectados) or 'ninguno'}")
    p()
    p("Error por alimento (de peor a mejor):")
    for nombre, datos in resumen["alimentos"].items():
        p(f"  {nombre:<28}{pct(datos['error_medio']):>8}   en {datos['apariciones']} plato(s)")
    p()
    p(f"Error medio por plato:    {pct(resumen['error_medio_por_plato'])}")
    p(f"Error medio por alimento: {pct(resumen['error_medio_por_alimento'])}")
    if resumen["entrada_tokens"]:
        p(f"Tokens: {resumen['entrada_tokens']} de entrada y {resumen['salida_tokens']} de salida")
    veredicto = "APROBADO" if resumen["aprobado"] else "NO APROBADO"
    p(f"Criterio (error medio por plato menor a {pct(UMBRAL_APROBACION)}): {veredicto}")


def guardar_reporte(carpeta: Path, resultados: list[ResultadoPlato], resumen: dict, proveedor: Proveedor) -> Path:
    carpeta.mkdir(parents=True, exist_ok=True)
    sello = datetime.now().strftime("%Y%m%d-%H%M%S")
    ruta_json = carpeta / f"reporte-{proveedor.nombre}-{sello}.json"
    ruta_json.write_text(json.dumps({
        "proveedor": proveedor.nombre,
        "modelo": proveedor.modelo,
        "resumen": resumen,
        "platos": [r.__dict__ for r in resultados],
    }, ensure_ascii=False, indent=2), encoding="utf-8")
    with (carpeta / f"reporte-{proveedor.nombre}-{sello}.csv").open("w", encoding="utf-8", newline="") as archivo:
        escritor = csv.writer(archivo)
        escritor.writerow(["plato", "alimento", "detectado_como", "gramos_reales", "gramos_estimados", "error"])
        for r in resultados:
            for d in r.detalle:
                escritor.writerow([r.plato, d["alimento"], d["detectado_como"] or "", d["gramos_reales"],
                                   d["gramos_estimados"], round(d["error"], 4)])
    return ruta_json


# ------------------------------------------------------------------------------ CLI


def cargar_env(ruta: Path) -> None:
    """Lee un .env sencillo (CLAVE=valor) sin pisar variables que ya existan."""
    if not ruta.is_file():
        return
    for linea in ruta.read_text(encoding="utf-8").splitlines():
        linea = linea.strip()
        if linea and not linea.startswith("#") and "=" in linea:
            clave, valor = linea.split("=", 1)
            os.environ.setdefault(clave.strip(), valor.strip().strip('"').strip("'"))


def main(argumentos: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Evalúa el escaneo de comida con IA contra gramos reales.")
    parser.add_argument("--fotos", type=Path, required=True, help="Carpeta con las fotos de los platos")
    parser.add_argument("--csv", type=Path, required=True, help="CSV con plato,foto,alimento,gramos")
    parser.add_argument("--proveedor", choices=["gemini", "falso"], default="gemini")
    parser.add_argument("--modelo", help=f"Modelo de Gemini (por defecto GEMINI_MODELO o {MODELO_POR_DEFECTO})")
    parser.add_argument("--salida", type=Path, default=Path(__file__).parent / "reportes")
    parser.add_argument("--pausa", type=float, default=1.0, help="Segundos entre llamadas, para no pasar el límite")
    args = parser.parse_args(argumentos)

    # En Windows la consola usa cp1252 por defecto y las tildes salen dañadas.
    for flujo in (sys.stdout, sys.stderr):
        if hasattr(flujo, "reconfigure"):
            flujo.reconfigure(encoding="utf-8")

    cargar_env(Path(__file__).parent / ".env")
    try:
        platos = leer_platos(args.csv, args.fotos)
    except (ValueError, FileNotFoundError) as error:
        print(f"Error: {error}", file=sys.stderr)
        return 2

    if args.proveedor == "gemini":
        clave = os.environ.get("GEMINI_API_KEY")
        if not clave:
            print("Error: falta GEMINI_API_KEY. Ponla en tools/eval-escaneo/.env (mira .env.example).", file=sys.stderr)
            return 2
        proveedor: Proveedor = ProveedorGemini(clave, args.modelo or os.environ.get("GEMINI_MODELO", MODELO_POR_DEFECTO))
    else:
        proveedor = ProveedorFalso(platos)

    resultados = []
    for indice, plato in enumerate(platos, start=1):
        print(f"[{indice}/{len(platos)}] {plato.id}...", file=sys.stderr)
        resultados.append(evaluar_plato(plato, proveedor.estimar(plato.foto)))
        if args.proveedor == "gemini" and indice < len(platos):
            time.sleep(args.pausa)

    resumen = resumir(resultados)
    imprimir_reporte(resultados, resumen, proveedor)
    print(f"\nReporte guardado en {guardar_reporte(args.salida, resultados, resumen, proveedor)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
