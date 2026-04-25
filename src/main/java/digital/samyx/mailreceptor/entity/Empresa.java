package digital.samyx.mailreceptor.entity;

import digital.samyx.mailreceptor.enums.mh.RegimenTributario;
import digital.samyx.mailreceptor.enums.mh.TipoIdentificacion;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import org.hibernate.proxy.HibernateProxy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Data
@Entity
@Table(name = "empresas")
@ToString(exclude = {"sucursales", "usuarioEmpresas", "configHacienda", "actividades"})
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_identificacion", length = 20)
    private TipoIdentificacion tipoIdentificacion;

    @Column(unique = true, length = 20)
    private String identificacion;

    @Column(length = 20)
    private String telefono;

    @Column(length = 100)
    private String email;

    @Column(nullable = false)
    private Boolean activa = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Datos comerciales adicionales
    @Column(name = "nombre_comercial", length = 80)
    private String nombreComercial;

    @Column(name = "nombre_razon_social", length = 100)
    private String nombreRazonSocial;

    @Column(length = 20)
    private String fax;

    @Column(name = "email_notificacion", length = 100)
    private String emailNotificacion;

    // Ubicación completa CR
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provincia_id")
    private Provincia provincia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "canton_id")
    private Canton canton;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "distrito_id")
    private Distrito distrito;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barrio_id")
    private Barrio barrio;

    @Column(name = "otras_senas", length = 500)
    private String otrasSenas;

    // Configuración fiscal
    @Column(name = "requiere_hacienda")
    private Boolean requiereHacienda = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "regimen_tributario")
    private RegimenTributario regimenTributario = RegimenTributario.REGIMEN_TRADICIONAL;

    @Column(name = "limite_anual_simplificado", precision = 18, scale = 2)
    private BigDecimal limiteAnualSimplificado;

    // Logo
    @Column(name = "logo_url")
    private String logoUrl;

    // ===== RELACIONES EXISTENTES =====
    @OneToMany(mappedBy = "empresa", fetch = FetchType.LAZY)
    private Set<Sucursal> sucursales = new HashSet();

    @OneToMany(mappedBy = "empresa", fetch = FetchType.LAZY)
    private Set<UsuarioEmpresa> usuarioEmpresas = new HashSet<>();

    // ===== NUEVAS RELACIONES =====
    @OneToOne(mappedBy = "empresa", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private EmpresaConfigHacienda configHacienda;

    @OneToMany(mappedBy = "empresa", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<EmpresaActividad> actividades = new HashSet<>();

    @Column(name = "productos_por_sucursal", nullable = false)
    private Boolean productosPorSucursal = false;

    @Column(name = "clientes_por_sucursal", nullable = false)
    private Boolean clientesPorSucursal = false;

    @Column(name = "categorias_por_sucursal", nullable = false)
    private Boolean categoriasPorSucursal = false;

    @Column(name = "inventario_por_sucursal", nullable = false)
    private Boolean inventarioPorSucursal = true;

    @Column(name = "proveedores_por_sucursal", nullable = false)
    private Boolean proveedoresPorSucursal = false;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        Class<?> oEffectiveClass = o instanceof HibernateProxy
            ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass()
            : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy
            ? ((HibernateProxy) this).getHibernateLazyInitializer()
            .getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) {
            return false;
        }
        Empresa that = (Empresa) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
            ? ((HibernateProxy) this).getHibernateLazyInitializer()
            .getPersistentClass().hashCode() : getClass().hashCode();
    }
}