package app.fuertia;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaPackage;
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

    // --- Piezas a la medida que ArchUnit no trae listas ---

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
