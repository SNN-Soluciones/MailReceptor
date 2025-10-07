package digital.samyx.mailreceptor.enums;

import lombok.Getter;

/**
 * Enum de roles del sistema con jerarquía definida.
 * El orden define la jerarquía (menor ordinal = mayor poder).
 */
@Getter
public enum RolNombre {

    ROOT("Root", "Control total del sistema", 1),
    SOPORTE("Soporte", "Soporte técnico con acceso casi total", 2),
    SUPER_ADMIN("Super Admin", "Dueño de empresa/restaurante", 3),
    ADMIN("Administrador", "Administrador de sucursal", 4),
    JEFE_CAJAS("Jefe de Cajas", "Supervisor de turno", 5),
    CAJERO("Cajero", "Operador de caja", 6),
    MESERO("Mesero", "Atención de mesas", 6),
    COCINA("Cocina", "Personal de cocina", 6),
    TEMPORAL("Temporal", "Rol temporal para pruebas", 88);

    private final String displayName;
    private final String descripcion;
    private final int nivelJerarquia;

    RolNombre(String displayName, String descripcion, int nivelJerarquia) {
        this.displayName = displayName;
        this.descripcion = descripcion;
        this.nivelJerarquia = nivelJerarquia;
    }

    /**
     * Verifica si este rol es operativo (sin capacidad de gestión).
     */
    public boolean esOperativo() {
        return this == CAJERO || this == MESERO || this == COCINA;
    }

    /**
     * Verifica si este rol es administrativo.
     */
    public boolean esAdministrativo() {
        return this == ADMIN || this == SUPER_ADMIN || this == SOPORTE || this == ROOT;
    }

    /**
     * Verifica si este rol es de sistema (SNN Soluciones).
     */
    public boolean esDeSistema() {
        return this == ROOT || this == SOPORTE;
    }

    /**
     * Verifica si el usuario es ROOT
     */
    public boolean esRoot() {
        return this == ROOT;
    }

    /**
     * Verifica si este rol puede crear otros usuarios.
     */
    public boolean puedeCrearUsuarios() {
        return !esOperativo();
    }

    /**
     * Verifica si este rol tiene mayor jerarquía que otro.
     */
    public boolean tieneMayorJerarquiaQue(RolNombre otro) {
        return this.nivelJerarquia < otro.nivelJerarquia;
    }

    /**
     * Obtiene los roles que este rol puede crear.
     */
    public RolNombre[] rolesQueCrear() {
      return switch (this) {
        case ROOT ->
          // ROOT normalmente solo crea SOPORTE y SUPER_ADMIN
            new RolNombre[]{SOPORTE, SUPER_ADMIN};
        case SOPORTE ->
          // SOPORTE puede crear casi todo excepto ROOT
            new RolNombre[]{SUPER_ADMIN, ADMIN, JEFE_CAJAS, CAJERO, MESERO, COCINA};
        case SUPER_ADMIN ->
          // Dueños crean administradores para sus sucursales
            new RolNombre[]{ADMIN};
        case ADMIN ->
          // Administradores crean todo el personal de la sucursal
            new RolNombre[]{JEFE_CAJAS, CAJERO, MESERO, COCINA};
        case JEFE_CAJAS ->
          // Supervisores solo crean operativos
            new RolNombre[]{CAJERO, MESERO, COCINA};
        default ->
          // Roles operativos no crean usuarios
            new RolNombre[]{};
      };
    }

    /**
     * Verifica si este rol puede crear otro rol específico.
     */
    public boolean puedeCrear(RolNombre rolACrear) {
        // ROOT puede crear cualquiera (por si acaso)
        if (this == ROOT) return true;

        // SOPORTE puede crear cualquiera excepto ROOT
        if (this == SOPORTE && rolACrear != ROOT) return true;

        // Para los demás, verificar en la lista permitida
        RolNombre[] permitidos = rolesQueCrear();
        for (RolNombre permitido : permitidos) {
            if (permitido == rolACrear) return true;
        }

        return false;
    }
}