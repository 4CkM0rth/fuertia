"""Pruebas del script de evaluación. Corren sin internet y sin clave: python -m unittest -v"""

import io
import json
import os
import tempfile
import unittest
import urllib.error
from pathlib import Path
from unittest import mock

import eval_escaneo as ev

CARPETA_EJEMPLO = Path(__file__).parent / "ejemplo"


class RespuestaSimulada(io.BytesIO):
    def __enter__(self):
        return self

    def __exit__(self, *args):
        self.close()


def respuesta_gemini(alimentos):
    return {
        "candidates": [{"content": {"parts": [{"text": json.dumps({"alimentos": alimentos})}]}}],
        "usageMetadata": {"promptTokenCount": 1300, "candidatesTokenCount": 60},
    }


class PruebasNormalizar(unittest.TestCase):
    def test_quita_tildes_mayusculas_y_espacios(self):
        self.assertEqual(ev.normalizar("  Plátano   MADURO "), "platano maduro")


class PruebasEmparejar(unittest.TestCase):
    def test_empareja_nombres_parecidos_y_contenidos(self):
        parejas, sobrantes = ev.emparejar(
            {"arroz": 150, "frijoles": 120},
            {"arroz blanco": 140, "frijol rojo": 100, "aguacate": 50},
        )
        self.assertEqual(parejas, {"arroz": "arroz blanco", "frijoles": "frijol rojo"})
        self.assertEqual(sobrantes, ["aguacate"])

    def test_no_empareja_alimentos_distintos(self):
        parejas, sobrantes = ev.emparejar({"arepa": 80}, {"pollo": 100})
        self.assertEqual(parejas, {})
        self.assertEqual(sobrantes, ["pollo"])

    def test_cada_estimado_se_usa_una_sola_vez(self):
        parejas, _ = ev.emparejar({"arroz": 100, "arroz integral": 100}, {"arroz": 90})
        self.assertEqual(list(parejas.values()), ["arroz"])


class PruebasMetricas(unittest.TestCase):
    def test_error_del_plato_y_de_alimentos(self):
        plato = ev.Plato("p1", Path("x.jpg"), {"arroz": 100, "carne": 100})
        resultado = ev.evaluar_plato(plato, ev.Estimacion({"arroz": 120, "carne": 70}))
        # Total: real 200, estimado 190, error 5%. Alimentos: 20% y 30%, promedio 25%.
        self.assertAlmostEqual(resultado.error_total, 0.05)
        self.assertAlmostEqual(resultado.error_medio_alimentos, 0.25)
        self.assertEqual(resultado.no_detectados, [])

    def test_alimento_no_detectado_cuenta_como_error_total(self):
        plato = ev.Plato("p1", Path("x.jpg"), {"arroz": 100, "aguacate": 50})
        resultado = ev.evaluar_plato(plato, ev.Estimacion({"arroz": 100}))
        self.assertEqual(resultado.no_detectados, ["aguacate"])
        self.assertAlmostEqual(resultado.error_medio_alimentos, 0.5)

    def test_resumen_aprueba_bajo_el_umbral(self):
        plato = ev.Plato("p1", Path("x.jpg"), {"arroz": 100})
        resumen = ev.resumir([ev.evaluar_plato(plato, ev.Estimacion({"arroz": 110}))])
        self.assertTrue(resumen["aprobado"])
        self.assertAlmostEqual(resumen["error_medio_por_plato"], 0.10)
        resumen = ev.resumir([ev.evaluar_plato(plato, ev.Estimacion({"arroz": 140}))])
        self.assertFalse(resumen["aprobado"])


class PruebasCsv(unittest.TestCase):
    def test_lee_el_ejemplo(self):
        platos = ev.leer_platos(CARPETA_EJEMPLO / "gramos.csv", CARPETA_EJEMPLO / "fotos")
        self.assertEqual(len(platos), 3)
        self.assertEqual(platos[0].reales["arroz blanco"], 150)

    def test_rechaza_gramos_invalidos_y_columnas_faltantes(self):
        with tempfile.TemporaryDirectory() as carpeta:
            ruta = Path(carpeta) / "g.csv"
            ruta.write_text("plato,foto,alimento,gramos\np1,a.jpg,arroz,mucho\n", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "no son un número"):
                ev.leer_platos(ruta, Path(carpeta))
            ruta.write_text("plato,alimento\np1,arroz\n", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "faltan columnas"):
                ev.leer_platos(ruta, Path(carpeta))

    def test_avisa_si_falta_la_foto(self):
        with tempfile.TemporaryDirectory() as carpeta:
            ruta = Path(carpeta) / "g.csv"
            ruta.write_text("plato,foto,alimento,gramos\np1,no-existe.jpg,arroz,100\n", encoding="utf-8")
            with self.assertRaises(FileNotFoundError):
                ev.leer_platos(ruta, Path(carpeta))


class PruebasGemini(unittest.TestCase):
    foto = CARPETA_EJEMPLO / "fotos" / "p01.png"

    def test_arma_la_solicitud_con_esquema_y_clave_en_encabezado(self):
        solicitud = ev.ProveedorGemini("clave-falsa", "gemini-flash-lite-latest").construir_solicitud(self.foto)
        cuerpo = json.loads(solicitud.data)
        self.assertIn("gemini-flash-lite-latest:generateContent", solicitud.full_url)
        self.assertNotIn("clave-falsa", solicitud.full_url)
        self.assertEqual(solicitud.get_header("X-goog-api-key"), "clave-falsa")
        self.assertEqual(cuerpo["generationConfig"]["responseMimeType"], "application/json")
        self.assertEqual(cuerpo["contents"][0]["parts"][1]["inline_data"]["mime_type"], "image/png")

    def test_interpreta_la_respuesta_y_suma_repetidos(self):
        estimacion = ev.ProveedorGemini.interpretar(respuesta_gemini([
            {"nombre": "Arroz Blanco", "gramos": 100, "confianza": 0.9},
            {"nombre": "arroz blanco", "gramos": 40, "confianza": 0.5},
            {"nombre": "Frijoles", "gramos": -5, "confianza": 0.3},
        ]))
        self.assertEqual(estimacion.alimentos, {"arroz blanco": 140, "frijoles": 0})
        self.assertEqual(estimacion.entrada_tokens, 1300)

    def test_estimar_con_llamada_simulada(self):
        def abrir(solicitud, timeout):
            return RespuestaSimulada(json.dumps(respuesta_gemini([{"nombre": "arepa", "gramos": 90, "confianza": 0.8}])).encode())

        estimacion = ev.ProveedorGemini("k", "m", abrir=abrir).estimar(self.foto)
        self.assertEqual(estimacion.alimentos, {"arepa": 90})

    def test_error_no_recuperable_se_reporta(self):
        def abrir(solicitud, timeout):
            raise urllib.error.HTTPError(solicitud.full_url, 400, "mal", {}, io.BytesIO(b"API key not valid"))

        with self.assertRaisesRegex(RuntimeError, "Gemini respondió 400"):
            ev.ProveedorGemini("k", "m", abrir=abrir).estimar(self.foto)


class PruebasPuntaAPunta(unittest.TestCase):
    def test_corre_con_el_proveedor_falso_y_guarda_reporte(self):
        with tempfile.TemporaryDirectory() as salida:
            codigo = ev.main([
                "--fotos", str(CARPETA_EJEMPLO / "fotos"),
                "--csv", str(CARPETA_EJEMPLO / "gramos.csv"),
                "--proveedor", "falso",
                "--salida", salida,
            ])
            self.assertEqual(codigo, 0)
            reporte = json.loads(next(Path(salida).glob("*.json")).read_text(encoding="utf-8"))
            self.assertEqual(reporte["resumen"]["platos"], 3)
            # El proveedor falso tiene error máximo de 30% por alimento, así que el plato no pasa de 30%.
            self.assertLess(reporte["resumen"]["error_medio_por_plato"], 0.30)

    def test_sin_clave_de_gemini_termina_con_mensaje_claro(self):
        # Sin la clave en el entorno y sin leer un .env real durante la prueba.
        with mock.patch.dict(os.environ, {}, clear=True), mock.patch.object(ev, "cargar_env"):
            codigo = ev.main(["--fotos", str(CARPETA_EJEMPLO / "fotos"), "--csv", str(CARPETA_EJEMPLO / "gramos.csv")])
        self.assertEqual(codigo, 2)


if __name__ == "__main__":
    unittest.main()
