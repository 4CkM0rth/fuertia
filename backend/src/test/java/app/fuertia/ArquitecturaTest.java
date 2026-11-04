package app.fuertia;

import app.fuertia.identidad.infrastructure.persistence.ContextoDeTenant;
import app.fuertia.identidad.infrastructure.persistence.TenantEntidad;
import app.fuertia.identidad.infrastructure.persistence.TokenDeRenovacionJpaRepositorio;
import app.fuertia.identidad.infrastructure.persistence.TokenDeUnUsoJpaRepositorio;
import app.fuertia.identidad.infrastructure.persistence.UsuarioJpaRepositorio;
import app.fuertia.identidad.infrastructure.web.FiltroDeTenantDelToken;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaPackage;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import org.hibernate.annotations.TenantId;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.jpa.repository.Query;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.AbstractClassesTransformer;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.all;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Reglas de la arquitectura hexagonal (docs/02-arquitectura.md), verificadas en cada build.
 * Si alguien importa Spring en el dominio o un módulo se mete al dominio de otro, esta prueba falla.
 *
 * Solo analiza el código de producción (DoNotIncludeTests): las pruebas sí pueden usar JUnit en cualquier paquete.
 */
@AnalyzeClasses(packages = "app.fuertia", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaTest {

    private static final String RAIZ = "app.fuertia";

    // Librerías de terceros que solo puede usar infrastructure.
    private static final String[] FRAMEWORKS = {
            "org.springframework..", "jakarta..", "javax.persistence..", "org.hibernate..",
            "com.fasterxml..", "tools.jackson..", "org.springdoc..", "io.swagger..",
            "org.flywaydb..", "org.postgresql..", "io.lettuce..", "org.slf4j..", "org.bouncycastle.."
    };

    @ArchTest
    static final ArchRule el_dominio_solo_usa_java =
            classes().that().resideInAPackage("..domain..")
                    .should().onlyDependOnClassesThat().resideInAnyPackage("java..", "..domain..")
                    .because("el dominio es Java puro: sin Spring, Jakarta, Hibernate ni otras librerías");

    @ArchTest
    static final ArchRule la_aplicacion_solo_usa_el_dominio_y_java =
            classes().that().resideInAPackage("..application..")
                    .should().onlyDependOnClassesThat().resideInAnyPackage("java..", "..domain..", "..application..")
                    .because("los casos de uso solo conocen el dominio y sus puertos");

    @ArchTest
    static final ArchRule solo_la_infraestructura_usa_frameworks =
            noClasses().that().resideOutsideOfPackages("..infrastructure..", RAIZ)
                    .should().dependOnClassesThat().resideInAnyPackage(FRAMEWORKS)
                    .because("los frameworks, la base de datos y los proveedores son detalles de infraestructura");

    @ArchTest
    static final ArchRule ningun_modulo_entra_al_dominio_de_otro =
            classes().that().resideInAPackage(RAIZ + ".*..")
                    .should(noUsarElDominioDeOtroModulo())
                    .because("los módulos se comunican por puertos de entrada o por eventos, no por el dominio ajeno");

    @ArchTest
    static final ArchRule cada_modulo_tiene_domain_application_e_infrastructure =
            all(modulos()).should(tenerLasTresCapas());

    // --- Reglas del aislamiento por tenant ---
    // Las listas blancas son a propósito cortas y explícitas: agregar algo exige editar esta prueba,
    // y ese cambio lo ve quien revisa el pull request.

    /** Entidades que pueden no tener @TenantId. Solo la raíz del multi-tenant. */
    private static final Set<String> ENTIDADES_SIN_TENANT = Set.of(TenantEntidad.class.getName());

    /** Métodos con SQL nativo, que Hibernate NO filtra por tenant. Cada uno debe justificarse en su comentario. */
    private static final Set<String> CONSULTAS_NATIVAS_PERMITIDAS = Set.of(
            // El correo es único en todo el sistema: el registro debe verlo aunque sea de otro tenant.
            UsuarioJpaRepositorio.class.getName() + "#existeCorreoEnTodoElSistema",
            // Descubrir el tenant en operaciones públicas, donde aún no hay JWT. Solo devuelven el
            // tenant_id; el resto del caso de uso corre dentro de ese tenant (DescubridorDeTenant).
            UsuarioJpaRepositorio.class.getName() + "#tenantDelCorreo",
            TokenDeUnUsoJpaRepositorio.class.getName() + "#tenantDelToken",
            TokenDeRenovacionJpaRepositorio.class.getName() + "#tenantDelToken");

    /** Clases que pueden apagar el filtro por tenant con ejecutarComoSistema. */
    private static final Set<String> PUEDEN_EJECUTAR_COMO_SISTEMA = Set.of(
            "app.fuertia.identidad.infrastructure.config.RegistrarEntrenadorTransaccional");

    /** Clases que pueden usar entrarEnTenant (si se olvida cerrarlo, el tenant se queda pegado al hilo). */
    private static final Set<String> PUEDEN_ENTRAR_EN_TENANT = Set.of(
            FiltroDeTenantDelToken.class.getName());

    /**
     * Clases que pueden armar SQL a mano (EntityManager.createNativeQuery, JdbcTemplate, JdbcClient o
     * JDBC directo). Hoy ninguna: todo el SQL nativo vive en @Query de la lista de arriba.
     */
    private static final Set<String> PUEDEN_ARMAR_SQL_A_MANO = Set.of();

    @ArchTest
    static final ArchRule toda_entidad_de_negocio_tiene_tenant_id =
            classes().that().areAnnotatedWith(Entity.class)
                    .and(noEstanEn(ENTIDADES_SIN_TENANT))
                    .should(tenerUnCampoConTenantId())
                    .because("toda tabla de negocio lleva tenant_id y Hibernate solo la filtra si el campo tiene @TenantId");

    @ArchTest
    static final ArchRule el_sql_nativo_solo_esta_en_la_lista_blanca =
            methods().that().areAnnotatedWith(consultaNativa())
                    .should(estarEn(CONSULTAS_NATIVAS_PERMITIDAS))
                    .because("Hibernate no aplica el filtro por tenant a las consultas nativas");

    @ArchTest
    static final ArchRule solo_la_lista_blanca_apaga_el_filtro_por_tenant =
            classes().should(noUsarElMetodoSalvo(ContextoDeTenant.class, "ejecutarComoSistema", PUEDEN_EJECUTAR_COMO_SISTEMA))
                    .because("ejecutarComoSistema apaga el filtro por tenant y deja ver los datos de todos los negocios");

    @ArchTest
    static final ArchRule solo_el_filtro_jwt_entra_en_un_tenant_sin_supplier =
            classes().should(noUsarElMetodoSalvo(ContextoDeTenant.class, "entrarEnTenant", PUEDEN_ENTRAR_EN_TENANT))
                    .because("si se olvida cerrar el alcance, la siguiente petición del mismo hilo heredaría el tenant");

    @ArchTest
    static final ArchRule el_sql_armado_a_mano_solo_esta_en_la_lista_blanca =
            classes().should(noArmarSqlAMano(PUEDEN_ARMAR_SQL_A_MANO))
                    .because("el SQL armado a mano no pasa por el filtro por tenant de Hibernate");

    // --- Piezas a la medida que ArchUnit no trae listas ---

    private static DescribedPredicate<JavaClass> noEstanEn(Set<String> nombres) {
        return DescribedPredicate.describe("no están en la lista " + nombres, clase -> !nombres.contains(clase.getName()));
    }

    private static ArchCondition<JavaClass> tenerUnCampoConTenantId() {
        return new ArchCondition<>("tener un campo anotado con @org.hibernate.annotations.TenantId") {
            @Override
            public void check(JavaClass entidad, ConditionEvents eventos) {
                // getAllFields incluye los heredados, por si el campo vive en una @MappedSuperclass.
                boolean tieneTenantId = entidad.getAllFields().stream()
                        .anyMatch(campo -> campo.isAnnotatedWith(TenantId.class));
                if (!tieneTenantId) {
                    eventos.add(SimpleConditionEvent.violated(entidad,
                            "La entidad " + entidad.getName() + " no tiene un campo con @TenantId"));
                }
            }
        };
    }

    /** @Query(nativeQuery = true) y también @NativeQuery, que es otra forma de escribir lo mismo. */
    private static DescribedPredicate<JavaAnnotation<?>> consultaNativa() {
        return DescribedPredicate.describe("@Query(nativeQuery = true) o @NativeQuery", anotacion ->
                anotacion.getRawType().isEquivalentTo(NativeQuery.class)
                        || (anotacion.getRawType().isEquivalentTo(Query.class)
                        && Boolean.TRUE.equals(anotacion.get("nativeQuery").orElse(false))));
    }

    private static ArchCondition<JavaMethod> estarEn(Set<String> permitidos) {
        return new ArchCondition<>("estar en la lista blanca " + permitidos) {
            @Override
            public void check(JavaMethod metodo, ConditionEvents eventos) {
                String nombre = metodo.getOwner().getName() + "#" + metodo.getName();
                if (!permitidos.contains(nombre)) {
                    eventos.add(SimpleConditionEvent.violated(metodo,
                            "SQL nativo fuera de la lista blanca: " + nombre));
                }
            }
        };
    }

    private static ArchCondition<JavaClass> noUsarElMetodoSalvo(Class<?> duenio, String metodo, Set<String> permitidas) {
        return new ArchCondition<>("no usar " + duenio.getSimpleName() + "." + metodo + ", salvo " + permitidas) {
            @Override
            public void check(JavaClass clase, ConditionEvents eventos) {
                if (permitidas.contains(clase.getName())) {
                    return;
                }
                // getAccessesFromSelf cubre llamadas normales, las que van dentro de lambdas y
                // las referencias a método (contexto::ejecutarComoSistema).
                clase.getAccessesFromSelf().stream()
                        .filter(acceso -> acceso.getTargetOwner().isEquivalentTo(duenio)
                                && acceso.getName().equals(metodo))
                        .forEach(acceso -> eventos.add(SimpleConditionEvent.violated(acceso,
                                clase.getName() + " usa " + metodo + " fuera de la lista blanca: "
                                        + acceso.getDescription())));
            }
        };
    }

    /**
     * Cubre el hueco que dejaba la regla de @Query: SQL escrito en código y no en una anotación.
     * - EntityManager o Session de Hibernate: createNativeQuery, createNativeMutationQuery, createSQLQuery.
     * - JdbcTemplate, NamedParameterJdbcTemplate y JdbcClient: cualquier uso.
     * - JDBC directo (Connection, Statement, DataSource.getConnection): cualquier uso.
     */
    private static ArchCondition<JavaClass> noArmarSqlAMano(Set<String> permitidas) {
        return new ArchCondition<>("no armar SQL a mano, salvo " + permitidas) {
            @Override
            public void check(JavaClass clase, ConditionEvents eventos) {
                if (permitidas.contains(clase.getName())) {
                    return;
                }
                clase.getAccessesFromSelf().stream()
                        .filter(acceso -> esSqlAMano(acceso.getTargetOwner(), acceso.getName()))
                        .forEach(acceso -> eventos.add(SimpleConditionEvent.violated(acceso,
                                clase.getName() + " arma SQL a mano: " + acceso.getDescription())));
            }
        };
    }

    private static boolean esSqlAMano(JavaClass duenio, String metodo) {
        boolean consultaNativaDeJpa = (duenio.isAssignableTo(EntityManager.class)
                || duenio.isAssignableTo("org.hibernate.query.QueryProducer"))
                && (metodo.startsWith("createNative") || metodo.equals("createSQLQuery"));
        boolean jdbcDeSpring = duenio.isAssignableTo("org.springframework.jdbc.core.JdbcOperations")
                || duenio.isAssignableTo("org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations")
                || duenio.isAssignableTo("org.springframework.jdbc.core.simple.JdbcClient");
        boolean jdbcDirecto = duenio.isAssignableTo("java.sql.Connection")
                || duenio.isAssignableTo("java.sql.Statement")
                || (duenio.isAssignableTo("javax.sql.DataSource") && metodo.equals("getConnection"));
        return consultaNativaDeJpa || jdbcDeSpring || jdbcDirecto;
    }

    private static final Pattern PAQUETE_DE_MODULO = Pattern.compile("^" + Pattern.quote(RAIZ) + "\\.([^.]+)(\\..*)?$");
    private static final Pattern PAQUETE_DE_DOMINIO = Pattern.compile("^" + Pattern.quote(RAIZ) + "\\.([^.]+)\\.domain(\\..*)?$");

    private static Optional<String> moduloDe(String paquete) {
        Matcher m = PAQUETE_DE_MODULO.matcher(paquete);
        return m.matches() ? Optional.of(m.group(1)) : Optional.empty();
    }

    private static ArchCondition<JavaClass> noUsarElDominioDeOtroModulo() {
        return new ArchCondition<>("no depender del domain de otro módulo") {
            @Override
            public void check(JavaClass origen, ConditionEvents eventos) {
                String moduloOrigen = moduloDe(origen.getPackageName()).orElseThrow();
                origen.getDirectDependenciesFromSelf().forEach(dependencia -> {
                    Matcher destino = PAQUETE_DE_DOMINIO.matcher(dependencia.getTargetClass().getPackageName());
                    if (destino.matches() && !destino.group(1).equals(moduloOrigen)) {
                        eventos.add(SimpleConditionEvent.violated(dependencia,
                                "El módulo '" + moduloOrigen + "' usa el dominio del módulo '" + destino.group(1)
                                        + "': " + dependencia.getDescription()));
                    }
                });
            }
        };
    }

    /** Convierte las clases importadas en la lista de módulos: los subpaquetes directos de app.fuertia. */
    private static AbstractClassesTransformer<JavaPackage> modulos() {
        return new AbstractClassesTransformer<>("módulos") {
            @Override
            public Iterable<JavaPackage> doTransform(JavaClasses clases) {
                return clases.getPackage(RAIZ).getSubpackages();
            }
        };
    }

    private static ArchCondition<JavaPackage> tenerLasTresCapas() {
        return new ArchCondition<>("tener los paquetes domain, application e infrastructure") {
            @Override
            public void check(JavaPackage modulo, ConditionEvents eventos) {
                Set<String> capas = modulo.getSubpackages().stream()
                        .map(JavaPackage::getRelativeName)
                        .collect(Collectors.toSet());
                for (String capa : List.of("domain", "application", "infrastructure")) {
                    if (!capas.contains(capa)) {
                        eventos.add(SimpleConditionEvent.violated(modulo,
                                "Al módulo '" + modulo.getRelativeName() + "' le falta el paquete '" + capa + "'"));
                    }
                }
            }
        };
    }
}
