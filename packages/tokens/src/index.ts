/**
 * Tokens de diseño de Fuertia, compartidos por la web (web/) y la app (app/).
 * Los valores salen de design/screens/*.dc.html (claro) y *Oscuro.dc.html (oscuro).
 * Regla: en la web y en la app nunca se escribe un color suelto; siempre un token.
 */

// Cada token se nombra por su USO, no por su aspecto: así en oscuro apunta al color
// que cumple el mismo papel y no a uno "parecido".
export const claro = {
  fondo: '#FAF6F4',
  tarjeta: '#FFFFFF',
  elevado: '#F3E9E6',

  texto: '#2A1216',
  textoSecundario: '#6E4D52',

  linea: '#EBDDD8', // borde de tarjeta
  lineaSuave: '#F2E7E4', // divisor entre filas
  lineaCampo: '#E5D3CE', // borde de campo y de botón secundario
  lineaFuerte: '#D9C3BE',
  contornoMaterial: '#A68A8E', // contorno de campos y chips en Android

  // Relleno de acción: círculo de comida hecha, "Nuevo asesorado", botón flotante de Android
  rellenoAccion: '#7A1E2C',
  textoSobreRelleno: '#FFFFFF',

  // Color de la sombra de las tarjetas en Android (la opacidad está en `sombra`)
  sombra: '#2A1216',

  primario: '#7A1E2C',
  primarioHover: '#5C1521',
  textoSobrePrimario: '#FFFFFF',
  enlace: '#7A1E2C',
  enlaceHover: '#5C1521',

  dorado: '#D4A537',
  textoSobreDorado: '#2A1216',
  doradoSuave: '#F6E9C9',
  textoSobreDoradoSuave: '#5C1521',

  // Tarjeta oscura de la pantalla Hoy (calorías del día)
  heroe: '#2A1216',
  heroeTexto: '#FFFFFF',
  heroeTextoSecundario: '#C4ABAF',
  heroePista: '#45282D',

  // Menú lateral de la web del entrenador
  menuFondo: '#2A1216',
  menuActivo: '#45282D',
  menuTexto: '#E3CFD2',
  menuTarjeta: '#3A2226',
  menuPista: '#45282D',

  exito: '#1E5A32',
  exitoSuave: '#E3F1E6',
  aviso: '#5C4413',
  avisoSuave: '#FFF8EA',
  error: '#8A2416',
  errorSuave: '#FDE2DD',
} as const;

export type NombreToken = keyof typeof claro;
export type Paleta = Record<NombreToken, string>;

// Oscuro: gris neutro sin tinte rojizo. El dorado solo va en el botón principal,
// el progreso, lo seleccionado y los enlaces (docs/04-diseno.md).
export const oscuro: Paleta = {
  fondo: '#111214',
  tarjeta: '#1C1D20',
  elevado: '#26272B',

  texto: '#EDEEF0',
  textoSecundario: '#A7AAB1',

  linea: '#2A2B2F',
  lineaSuave: '#26272B',
  lineaCampo: '#34353A',
  lineaFuerte: '#3E3F45',
  contornoMaterial: '#55585F',

  rellenoAccion: '#4A4B52',
  textoSobreRelleno: '#FFFFFF',

  sombra: '#000000',

  primario: '#D4A537',
  primarioHover: '#E2C171',
  textoSobrePrimario: '#141516',
  enlace: '#E2C171',
  enlaceHover: '#E9D6A8',

  dorado: '#D4A537',
  textoSobreDorado: '#141516',
  doradoSuave: '#2E2818',
  textoSobreDoradoSuave: '#E9D6A8',

  heroe: '#25262A',
  heroeTexto: '#FFFFFF',
  heroeTextoSecundario: '#9DA1A8',
  heroePista: '#36373C',

  menuFondo: '#25262A',
  menuActivo: '#36373C',
  menuTexto: '#C9CCD1',
  menuTarjeta: '#2B2C30',
  menuPista: '#36373C',

  exito: '#8FD3A3',
  exitoSuave: '#16291E',
  aviso: '#F2C46B',
  avisoSuave: '#24211A',
  error: '#F4A493',
  errorSuave: '#33191A',
};

export const temas = { claro, oscuro } as const;
export type NombreTema = keyof typeof temas;

export const tipografia = {
  familia: 'Poppins',
  pesos: { regular: 400, medio: 500, seminegrita: 600, negrita: 700, extranegrita: 800 },
  // Tamaños en px. El mínimo es 12 (regla de accesibilidad del diseño).
  tamanos: {
    pequeno: 12,
    secundario: 13,
    apoyo: 14, // fecha de Hoy y textos de apoyo: el segundo tamaño más usado del diseño
    cuerpo: 15,
    intermedio: 16,
    destacado: 17,
    subtitulo: 18,
    saludo: 22, // saludo de AndroidHoy y nombres en Perfil
    titulo: 28,
    cifra: 44,
  },
} as const;

export const medidas = {
  tactilMinimo: 44,
  radios: { pequeno: 10, medio: 12, boton: 14, tarjeta: 16, grande: 20, heroe: 24 },
  // ml (14) y lxl (18) existen porque el diseño los usa mucho entre l y xl.
  espacios: { xs: 4, s: 8, m: 12, ml: 14, l: 16, lxl: 18, xl: 20, xxl: 24 },
} as const;

/** Opacidad de la sombra de las tarjetas en Android, por tema. El color es el token `sombra`. */
export const sombra = {
  claro: { tarjeta: 0.12, flotante: 0.28 },
  oscuro: { tarjeta: 0.5, flotante: 0.6 },
} as const;

/** Convierte "textoSecundario" en "--fu-texto-secundario" para usar como variable CSS en la web. */
export function nombreVariableCss(token: NombreToken): string {
  return '--fu-' + token.replace(/[A-Z]/g, (letra) => '-' + letra.toLowerCase());
}

/** Bloque de variables CSS de un tema, listo para poner dentro de un selector. */
export function variablesCss(paleta: Paleta): string {
  return (Object.keys(paleta) as NombreToken[])
    .map((token) => `${nombreVariableCss(token)}: ${paleta[token]};`)
    .join('\n');
}

/** Contraste WCAG entre dos colores hex. AA pide 4.5 para texto normal y 3 para texto grande. */
export function contraste(colorA: string, colorB: string): number {
  const luminancia = (hex: string) => {
    const canales = [1, 3, 5].map((i) => parseInt(hex.slice(i, i + 2), 16) / 255);
    const [r, g, b] = canales.map((c) => (c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4));
    return 0.2126 * r + 0.7152 * g + 0.0722 * b;
  };
  const [claroL, oscuroL] = [luminancia(colorA), luminancia(colorB)].sort((a, b) => b - a);
  return (claroL + 0.05) / (oscuroL + 0.05);
}
