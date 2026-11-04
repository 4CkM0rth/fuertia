import { TarjetaEstado } from './estado/TarjetaEstado'
import { SelectorTema } from './tema/SelectorTema'

export default function App() {
  return (
    <div className="pagina">
      <header className="cabecera">
        <div className="marca">
          {/* Logo provisional: la inicial sobre dorado, como en WebDashboard, hasta que exista el logo. */}
          <span className="marca__logo" aria-hidden="true">
            F
          </span>
          <div className="marca__textos">
            <h1 className="marca__nombre">Fuertia</h1>
            <p className="marca__lema">Planificación deportiva</p>
          </div>
        </div>
        <SelectorTema />
      </header>

      <main className="contenido">
        <TarjetaEstado />
      </main>
    </div>
  )
}
