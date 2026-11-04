import { Outlet } from 'react-router'
import './acceso.css'

/**
 * Marco de las pantallas de acceso (design/screens/WebLogin.dc.html): panel de marca a un lado
 * y el formulario al otro. Con flex-wrap, en el celular el panel queda arriba y el formulario abajo.
 */
export function DisenoAcceso() {
  return (
    <div className="acceso">
      <section className="acceso__marca" aria-label="Fuertia">
        <div className="acceso__logo">
          {/* Logo provisional: la inicial sobre dorado, hasta que exista el logo. */}
          <span className="acceso__logo-inicial" aria-hidden="true">
            F
          </span>
          <span className="acceso__logo-nombre">Fuertia</span>
        </div>
        <div className="acceso__mensaje">
          {/* En el diseño es un h1. Aquí es un párrafo con el mismo estilo para que el h1 de cada
              página sea su propósito ("Entrar", "Crea tu cuenta gratis"), que es lo que anuncia el lector. */}
          <p className="acceso__titular">Tus asesorados, su comida y su progreso en un solo lugar.</p>
          <p className="acceso__descripcion">
            Crea rutinas con tu propia técnica, arma planes de alimentación con foto de cada plato y revisa lo que
            tus asesorados registran.
          </p>
        </div>
        <ul className="acceso__ventajas">
          <li>Rutinas con video y fotos tuyas</li>
          <li>Planes con foto por plato</li>
          <li>Escaneo de comida con IA</li>
        </ul>
      </section>

      <main className="acceso__contenido">
        <div className="acceso__columna">
          <Outlet />
        </div>
      </main>
    </div>
  )
}
