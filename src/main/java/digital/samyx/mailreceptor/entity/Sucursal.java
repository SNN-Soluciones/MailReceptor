package digital.samyx.mailreceptor.entity;

import digital.samyx.mailreceptor.enums.mh.ModoFacturacion;
import digital.samyx.mailreceptor.enums.mh.ModoImpresion;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

@Entity
@Table(name = "sucursales",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"nombre", "empresa_id"}),
        @UniqueConstraint(columnNames = {"codigo_sucursal", "empresa_id"})
    },
    indexes = {
        @Index(name = "idx_sucursal_empresa", columnList = "empresa_id"),
        @Index(name = "idx_sucursal_codigo", columnList = "codigo_sucursal")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Sucursal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 20)
    private String telefono;

    @Column(length = 100)
    private String email;

    // Número para formar el consecutivo (001, 002, etc.)
    @Column(name = "numero_sucursal", length = 3, nullable = false)
    private String numeroSucursal;

    @Column(name = "modo_facturacion", nullable = false, columnDefinition = "modo_facturacion_enum")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private ModoFacturacion modoFacturacion = ModoFacturacion.ELECTRONICO;

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

    @OneToMany(mappedBy = "sucursal", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Terminal> terminales = new ArrayList<>();


    @Column(name = "activa", nullable = false)
    private Boolean activa;

    private String logoSucursalPath;

    // NUEVOS CAMPOS PARA FASE 2

    /**
     * Define si esta sucursal maneja control de inventario
     * Si es false, no se valida stock al vender
     */
    @Column(name = "maneja_inventario", nullable = false)
    @Builder.Default
    private Boolean manejaInventario = true;

    /**
     * Define si esta sucursal permite productos con recetas
     * Si es false, solo puede vender productos de inventario SIMPLE o NINGUNO
     */
    @Column(name = "aplica_recetas", nullable = false)
    @Builder.Default
    private Boolean aplicaRecetas = true;

    /**
     * Define si permite inventario negativo (vender sin stock)
     * Solo aplica si manejaInventario = true
     */
    @Column(name = "permite_negativos", nullable = false)
    @Builder.Default
    private Boolean permiteNegativos = false;

    /**
     * Define si imprime LOCAL (navegador) u ORQUESTADOR (La Chismosa)
     */
    @Column(name = "modo_impresion", nullable = false)
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Builder.Default
    private ModoImpresion modoImpresion = ModoImpresion.LOCAL;

    /**
     * IP y puerto del orquestador de impresoras
     * Ejemplo: "192.168.1.100:5001" o "http://192.168.1.100:5001"
     * Solo se usa si modoImpresion = ORQUESTADOR
     */
    @Column(name = "ip_orquestador", length = 100)
    private String ipOrquestador;

    // Campos existentes de auditoría
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Métodos helper
    public boolean puedeVenderProductoConReceta() {
        return this.aplicaRecetas;
    }

    public boolean requiereValidarStock() {
        return this.manejaInventario && !this.permiteNegativos;
    }

    public boolean puedeVenderSinStock() {
        return !this.manejaInventario || this.permiteNegativos;
    }

    /**
     * Verifica si esta sucursal usa impresión local (desde navegador)
     */
    public boolean usaImpresionLocal() {
        return this.modoImpresion == ModoImpresion.LOCAL;
    }

    /**
     * Verifica si esta sucursal usa el orquestador de impresoras
     */
    public boolean usaOrquestador() {
        return this.modoImpresion == ModoImpresion.ORQUESTADOR;
    }

    /**
     * Obtiene la URL completa del orquestador con protocolo
     * @return URL con protocolo, ej: "http://192.168.1.100:5001", o null si no configurado
     */
    public String getUrlOrquestador() {
        if (this.ipOrquestador == null || this.ipOrquestador.trim().isEmpty()) {
            return null;
        }

        String ip = this.ipOrquestador.trim();

        // Si ya tiene http:// o https://, devolverlo tal cual
        if (ip.startsWith("http://") || ip.startsWith("https://")) {
            return ip;
        }

        // Si no, agregar http://
        return "http://" + ip;
    }

    /**
     * Valida que la configuración de impresión sea correcta
     * @return true si es válida, false si falta configuración
     */
    public boolean tieneConfiguracionImpresionValida() {
        if (this.modoImpresion == ModoImpresion.ORQUESTADOR) {
            return this.ipOrquestador != null && !this.ipOrquestador.trim().isEmpty();
        }
        return true; // LOCAL no requiere configuración adicional
    }
}