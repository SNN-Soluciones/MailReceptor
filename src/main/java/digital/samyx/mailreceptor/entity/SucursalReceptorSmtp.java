package digital.samyx.mailreceptor.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sucursal_receptor_smtp")
@Data
@EqualsAndHashCode(exclude = {"sucursal", "empresa", "createdBy", "updatedBy"})
@ToString(exclude = {"sucursal", "empresa", "createdBy", "updatedBy"})
public class SucursalReceptorSmtp {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;
    
    @Column(unique = true, nullable = false, length = 160)
    private String email;
    
    @Column(name = "smtp_password", nullable = false)
    private String smtpPassword;
    
    @Column(name = "email_domain", length = 100)
    private String emailDomain;
    
    @Column(columnDefinition = "boolean default true")
    private Boolean activo = true;
    
    @Column(name = "procesar_automaticamente", columnDefinition = "boolean default true")
    private Boolean procesarAutomaticamente = true;
    
    @Column(name = "notificar_rechazos", columnDefinition = "boolean default true")
    private Boolean notificarRechazos = true;
    
    @ElementCollection
    @CollectionTable(name = "sucursal_smtp_emails_notificacion", 
                      joinColumns = @JoinColumn(name = "sucursal_smtp_id"))
    @Column(name = "email")
    private List<String> emailsNotificacion = new ArrayList<>();
    
    @Column(name = "ultimo_procesamiento")
    private LocalDateTime ultimoProcesamiento;
    
    @Column(name = "total_procesados", columnDefinition = "integer default 0")
    private Integer totalProcesados = 0;
    
    @Column(name = "total_errores", columnDefinition = "integer default 0")
    private Integer totalErrores = 0;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Usuario createdBy;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private Usuario updatedBy;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}