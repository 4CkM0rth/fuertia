/**
 * Tokens de diseño de Fuertia, compartidos por la web (web/) y la app (app/).
 * Los valores salen de design/screens/*.dc.html (claro) y *Oscuro.dc.html (oscuro).
 * Regla: en la web y en la app nunca se escribe un color suelto; siempre un token.
 */

export const claro = {
  fondo: '#FAF6F4',
  tarjeta: '#FFFFFF',
  elevado: '#F3E9E6',
  seleccionado: '#F6E9C9',

  texto: '#2A1216',
  textoSecundario: '#6E4D52',
  textoTenue: '#A68A8E',

  linea: '#EBDDD8',
  lineaSuave: '#F2E7E4',
  lineaFuerte: '#D9C3BE',

  primario: '#7A1E2C',
  primarioHover: '#5C1521',
  textoSobrePrimario: '#FFFFFF',
  enlace: '#7A1E2C',
  enlaceHover: '#5C1521',

  dorado: '#D4A537',
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

  exito: '#1E5A32',
  exitoSuave: '#E3F1E6',
  aviso: '#5C4413',
  avisoSuave: '#FFF8EA',
  error: '#8A2416',
  errorSuave: '#F6E1DC',
} as const;

export type NombreToken = keyof typeof claro;
export type Paleta = Record<NombreToken, string>;

// Oscuro: gris neutro sin tinte rojizo. El dorado solo va en el botón principal,
// el progreso, lo seleccionado y los enlaces (docs/04-diseno.md).
export const oscuro: Paleta = {
  fondo: '#111214',
  tarjeta: '#1C1D20',
  elevado: '#26272B',
  seleccionado: '#4A4B52',

  texto: '#EDEEF0',
  textoSecundario: '#A7AAB1',
  textoTenue: '#9DA1A8',

  linea: '#34353A',
  lineaSuave: '#2A2B2F',
  lineaFuerte: '#3E3F45',

  primario: '#D4A537',
  primarioHover: '#E2C171',
  textoSobrePrimario: '#141516',
  enlace: '#E2C171',
  enlaceHover: '#E9D6A8',

  dorado: '#D4A537',
  doradoSuave: '#2E2818',
  textoSobreDoradoSuave: '#E9D6A8',

  heroe: '#25262A',
  heroeTexto: '#EDEEF0',
  heroeTextoSecundario: '#A7AAB1',
  heroePista: '#3A3B40',

  menuFondo: '#1C1D20',
  menuActivo: '#26272B',
  menuTexto: '#C9CCD1',
  menuTarjeta: '#26272B',

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
  tamanos: { pequeno: 12, secundario: 13, cuerpo: 15, destacado: 17, subtitulo: 18, titulo: 28, cifra: 44 },
} as const;

export const medidas = {
  tactilMinimo: 44,
  radios: { pequeno: 10, medio: 12, tarjeta: 16, grande: 20, heroe: 24 },
  espacios: { xs: 4, s: 8, m: 12, l: 16, xl: 20, xxl: 24 },
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
