package app.fuertia.identidad.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * El negocio de un entrenador. Es la raíz del multi-tenant: todos los datos de negocio
 * (usuarios, asesorados, rutinas, planes...) cuelgan de un tenant.
 */
public final class Tenant {

    public static final int LONGITUD_MINIMA_NOMBRE = 2;
    public static final int LONGITUD_MAXIMA_NOMBRE = 80;

    private final TenantId id;
    private final String nombre;
    private final Plan plan;
    private final Instant creadoEn;

    public Tenant(TenantId id, String nombre, Plan plan, Instant creadoEn) {
        this.id = Objects.requireNonNull(id);
        this.nombre = Textos.requerido(nombre, "nombreNegocio", "el nombre de tu negocio",
                LONGITUD_MINIMA_NOMBRE, LONGITUD_MAXIMA_NOMBRE);
        this.plan = Objects.requireNonNull(plan);
        this.creadoEn = Objects.requireNonNull(creadoEn);
    }

    /** Un negocio recién creado: id nuevo y plan GRATIS. */
    public static Tenant nuevo(String nombre, Instant ahora) {
        return new Tenant(TenantId.nuevo(), nombre, Plan.GRATIS, ahora);
    }

    public TenantId id() {
        return id;
    }

    public String nombre() {
        return nombre;
    }

    public Plan plan() {
        return plan;
    }

    public Instant creadoEn() {
        return creadoEn;
    }

    // Una entidad se identifica por su id, no por el valor de sus campos.
    @Override
    public boolean equals(Object otro) {
        return otro instanceof Tenant tenant && id.equals(tenant.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
