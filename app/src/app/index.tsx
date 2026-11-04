import { PantallaHoy } from '@/pantallas/PantallaHoy';

// Expo Router: cada archivo de src/app es una ruta. index.tsx es "/", la primera pantalla.
// La pantalla vive en src/pantallas para poder probarla sin el enrutador.
export default function Inicio() {
  return <PantallaHoy />;
}
