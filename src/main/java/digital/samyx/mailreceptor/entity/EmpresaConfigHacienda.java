package digital.samyx.mailreceptor.entity;

import com.snnsoluciones.backnathbitpos.enums.mh.AmbienteHacienda;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoAutenticacionHacienda;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "empresa_config_hacienda")
@ToString(exclude = {"empresa"})
public class EmpresaConfigHacienda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    // Ambiente de trabajo
    @Enumerated(EnumType.STRING)
    @Column(name = "ambiente", nullable = false)
    private AmbienteHacienda ambiente = AmbienteHacienda.SANDBOX;

    // Tipo de autenticación
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_autenticacion", nullable = false)
    private TipoAutenticacionHacienda tipoAutenticacion = TipoAutenticacionHacienda.LLAVE_CRIPTOGRAFICA;

    // === Para Llave ATV (Criptográfica) ===
    @Column(name = "usuario_hacienda", length = 100)
    private String usuarioHacienda;

    @Column(name = "clave_hacienda", length = 255)
    private String claveHacienda; // Se debe encriptar antes de guardar

    // === Para Firma Digital (futuro) ===
    @Column(name = "url_certificado_key", length = 500)
    private String urlCertificadoKey;

    // NUEVO: Para saber si el certificado está encriptado en S3
    @Column(name = "certificado_encriptado")
    private Boolean certificadoEncriptado = true;


    @Column(name = "pin_certificado", length = 255)
    private String pinCertificado; // Se debe encriptar antes de guardar

    @Column(name = "fecha_emision_certificado")
    private LocalDate fechaEmisionCertificado;

    @Column(name = "fecha_vencimiento_certificado")
    private LocalDate fechaVencimientoCertificado;

    // === Mensajes personalizados ===
    @Column(name = "nota_factura", length = 500)
    private String notaFactura;

    @Column(name = "nota_validez_proforma", length = 200)
    private String notaValidezProforma;

    @Column(name = "detalle_factura1", length = 200)
    private String detalleFactura1;

    @Column(name = "detalle_factura2", length = 200)
    private String detalleFactura2;

    // Auditoría
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Valida si la configuración está completa para el tipo de autenticación
     */
    public boolean isConfiguracionCompleta() {
        if (tipoAutenticacion == TipoAutenticacionHacienda.LLAVE_CRIPTOGRAFICA) {
            return usuarioHacienda != null && !usuarioHacienda.isEmpty() &&
                claveHacienda != null && !claveHacienda.isEmpty();
        } else if (tipoAutenticacion == TipoAutenticacionHacienda.FIRMA_DIGITAL) {
            return urlCertificadoKey != null && !urlCertificadoKey.isEmpty() &&
                pinCertificado != null && !pinCertificado.isEmpty() &&
                fechaVencimientoCertificado != null &&
                fechaVencimientoCertificado.isAfter(LocalDate.now());
        }
        return false;
    }

}