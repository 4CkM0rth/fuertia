package app.fuertia.identidad.application.usecase;

import app.fuertia.identidad.application.port.out.CifradorDeContrasenas;
import app.fuertia.identidad.application.port.out.EmisorDeTokensDeAcceso;
import app.fuertia.identidad.application.port.out.EnviadorDeCorreos;
import app.fuertia.identidad.application.port.out.LimitadorDePeticiones;
import app.fuertia.identidad.application.port.out.RepositorioDeSesiones;
import app.fuertia.identidad.application.port.out.RepositorioDeTenants;
import app.fuertia.identidad.application.port.out.RepositorioDeTokensDeRenovacion;
import app.fuertia.identidad.application.port.out.RepositorioDeTokensDeUnUso;
import app.fuertia.identidad.application.port.out.RepositorioDeUsuarios;
import app.fuertia.identidad.domain.Correo;
import app.fuertia.identidad.domain.DemasiadosIntentos;
import app.fuertia.identidad.domain.HashDeToken;
import app.fuertia.identidad.domain.Sesion;
import app.fuertia.identidad.domain.SesionId;
import app.fuertia.identidad.domain.Tenant;
import app.fuertia.identidad.domain.TenantId;
import app.fuertia.identidad.domain.TipoDeTokenDeUnUso;
import app.fuertia.identidad.domain.TokenDeRenovacion;
import app.fuertia.identidad.domain.TokenDeUnUso;
import app.fuertia.identidad.domain.TokenSecreto;
import app.fuertia.identidad.domain.Usuario;
import app.fuertia.identidad.domain.UsuarioId;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Dobles de prueba de los puertos de salida: implementaciones mínimas en memoria. Permiten probar
 * los casos de uso sin Spring, sin base de datos y sin servidor de correo. Es posible justamente
 * porque los casos de uso dependen de interfaces (puertos) y no de JPA.
 */
final class Dobles {

    private Dobles() {
    }

    static final class Usuarios implements RepositorioDeUsuarios {
        final Map<UsuarioId, Usuario> porId = new LinkedHashMap<>();

        @Override
        public boolean existeCorreo(Correo correo) {
            return porId.values().stream().anyMatch(u -> u.correo().equals(correo));
        }

        @Override
        public void guardar(Usuario usuario) {
            porId.put(usuario.id(), usuario);
        }

        @Override
        public void actualizar(Usuario usuario) {
            if (!porId.containsKey(usuario.id())) {
                throw new IllegalStateException("No existe");
            }
            porId.put(usuario.id(), usuario);
        }

        @Override
        public Optional<Usuario> buscarPorId(UsuarioId id) {
            return Optional.ofNullable(porId.get(id));
        }

        @Override
        public Optional<Usuario> buscarPorCorreo(Correo correo) {
            return porId.values().stream().filter(u -> u.correo().equals(correo)).findFirst();
        }
    }

    static final class Tenants implements RepositorioDeTenants {
        final Map<TenantId, Tenant> porId = new LinkedHashMap<>();

        @Override
        public void guardar(Tenant tenant) {
            porId.put(tenant.id(), tenant);
        }

        @Override
        public Optional<Tenant> buscarPorId(TenantId id) {
            return Optional.ofNullable(porId.get(id));
        }
    }

    static final class TokensDeUnUso implements RepositorioDeTokensDeUnUso {
        final Map<UUID, TokenDeUnUso> porId = new LinkedHashMap<>();

        @Override
        public void guardar(TokenDeUnUso token) {
            porId.put(token.id(), token);
        }

        @Override
        public Optional<TokenDeUnUso> buscarPorHash(HashDeToken hash) {
            return porId.values().stream().filter(t -> t.hash().equals(hash)).findFirst();
        }

        @Override
        public boolean marcarComoUsado(UUID id, Instant ahora) {
            TokenDeUnUso t = porId.get(id);
            if (t == null || t.usadoEn() != null) {
                return false;
            }
            porId.put(id, usado(t, ahora));
            return true;
        }

        @Override
        public void invalidarPendientes(UsuarioId usuario, TipoDeTokenDeUnUso tipo, Instant ahora) {
            porId.replaceAll((id, t) -> t.usuarioId().equals(usuario) && t.tipo() == tipo && t.usadoEn() == null
                    ? usado(t, ahora) : t);
        }

        private static TokenDeUnUso usado(TokenDeUnUso t, Instant ahora) {
            return new TokenDeUnUso(t.id(), t.tipo(), t.usuarioId(), t.tenantId(), t.hash(), t.creadoEn(),
                    t.venceEn(), ahora);
        }
    }

    static final class Sesiones implements RepositorioDeSesiones {
        final Map<SesionId, Sesion> porId = new LinkedHashMap<>();

        @Override
        public void guardar(Sesion sesion) {
            porId.put(sesion.id(), sesion);
        }

        @Override
        public Optional<Sesion> buscarPorId(SesionId id) {
            return Optional.ofNullable(porId.get(id));
        }

        @Override
        public void revocar(SesionId id, Instant ahora) {
            porId.computeIfPresent(id, (clave, s) -> s.estaActiva() ? revocada(s, ahora) : s);
        }

        @Override
        public void revocarTodasDe(UsuarioId usuario, Instant ahora) {
            porId.replaceAll((id, s) -> s.usuarioId().equals(usuario) && s.estaActiva() ? revocada(s, ahora) : s);
        }

        private static Sesion revocada(Sesion s, Instant ahora) {
            return new Sesion(s.id(), s.tenantId(), s.usuarioId(), s.canal(), s.creadaEn(), ahora);
        }
    }

    static final class TokensDeRenovacion implements RepositorioDeTokensDeRenovacion {
        final Map<UUID, TokenDeRenovacion> porId = new LinkedHashMap<>();

        @Override
        public void guardar(TokenDeRenovacion token) {
            porId.put(token.id(), token);
        }

        @Override
        public Optional<TokenDeRenovacion> buscarPorHash(HashDeToken hash) {
            return porId.values().stream().filter(t -> t.hash().equals(hash)).findFirst();
        }

        @Override
        public boolean marcarComoUsado(UUID id, Instant ahora) {
            TokenDeRenovacion t = porId.get(id);
            if (t == null || t.usadoEn() != null) {
                return false;
            }
            porId.put(id, new TokenDeRenovacion(t.id(), t.sesionId(), t.tenantId(), t.hash(), t.creadoEn(),
                    t.venceEn(), ahora));
            return true;
        }
    }

    /** Cifrado de mentiras, predecible, que además cuenta cuántas veces se usó. */
    static final class Cifrador implements CifradorDeContrasenas {
        static final String SENUELO = "cifrada(señuelo)";
        int cifrados;
        final List<String> comparadosContra = new ArrayList<>();

        @Override
        public String cifrar(String contrasenaEnClaro) {
            cifrados++;
            return "cifrada(" + contrasenaEnClaro + ")";
        }

        @Override
        public boolean coincide(String contrasenaEnClaro, String hash) {
            comparadosContra.add(hash);
            return ("cifrada(" + contrasenaEnClaro + ")").equals(hash);
        }

        @Override
        public String hashSenuelo() {
            return SENUELO;
        }
    }

    /** Guarda los correos "enviados" para revisarlos. */
    static final class Correos implements EnviadorDeCorreos {
        record Enviado(String tipo, Correo destinatario, TokenSecreto token) {
        }

        final List<Enviado> enviados = new ArrayList<>();

        @Override
        public void enviarEnlaceDeVerificacion(Correo destinatario, String nombre, TokenSecreto token) {
            enviados.add(new Enviado("verificacion", destinatario, token));
        }

        @Override
        public void enviarAvisoDeCuentaExistente(Correo destinatario) {
            enviados.add(new Enviado("cuenta-existente", destinatario, null));
        }

        @Override
        public void enviarEnlaceDeRecuperacion(Correo destinatario, String nombre, TokenSecreto token) {
            enviados.add(new Enviado("recuperacion", destinatario, token));
        }
    }

    /** Limitador en memoria con el mismo comportamiento: cuenta por clave y lanza al pasarse. */
    static final class Limitador implements LimitadorDePeticiones {
        final Map<String, Integer> intentos = new HashMap<>();

        @Override
        public void consumir(String clave, int maximo, Duration ventana) {
            int n = intentos.merge(clave, 1, Integer::sum);
            if (n > maximo) {
                throw new DemasiadosIntentos(ventana);
            }
        }
    }

    static final class Emisor implements EmisorDeTokensDeAcceso {
        @Override
        public TokenDeAcceso emitir(Usuario usuario, SesionId sesion, Instant ahora) {
            return new TokenDeAcceso("jwt-de-" + usuario.id() + "-" + sesion, ahora.plus(Duration.ofMinutes(15)));
        }
    }
}
